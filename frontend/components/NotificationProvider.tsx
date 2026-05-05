"use client";

import {
    createContext,
    useCallback,
    useContext,
    useEffect,
    useMemo,
    useState,
    ReactNode,
} from "react";
import { useRouter } from "next/navigation";
import { Client, IMessage } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import {
    fetchNotifications,
    respondNotification,
    clearNotificationApi,
} from "@/lib/notifications-api";
import { getToken } from "@/lib/auth";
import {
    Notification,
    NotificationDecision,
} from "@/lib/notification-types";
import { API_BASE } from "@/lib/api-utils";

// ------------------------------------------------------------------ Types
const ENABLE_NOTIFICATIONS = true;
type NotificationContextType = {
    notifications: Notification[];
    unreadOrPendingCount: number;
    loading: boolean;
    respondToNotification: (id: number, decision: NotificationDecision) => Promise<void>;
    clearNotification: (id: number) => Promise<void>;
    refresh: () => void;
};

const NotificationContext = createContext<NotificationContextType | undefined>(
    undefined
);

// ------------------------------------------------------------------ Helpers

type ApiNotification = {
    id: number;
    type: string;
    message: string;
    status: string;
    readStatus: boolean;
    fromUserId: number | null;
    fromUserName: string | null;
    toUserId: number | null;
    groupId: number | null;
    createdAt: string;
};

function mapApiNotification(n: ApiNotification): Notification {
    return {
        id: n.id,
        type: String(n.type).trim() as Notification["type"],
        message: n.message,
        status: String(n.status).trim().toLowerCase() as Notification["status"],
        readStatus: Boolean(n.readStatus),
        fromUserId: n.fromUserId,
        fromUserName: n.fromUserName ?? null,
        groupId: n.groupId,
        createdAt: n.createdAt,
    };
}

function getWebSocketUrl(): string {
    return `${API_BASE.replace(/\/api\/v1$/, "")}/ws`;
}

// ------------------------------------------------------------------ Provider

export function NotificationProvider({ children }: { children: ReactNode }) {
    const router = useRouter();
    const [notifications, setNotifications] = useState<Notification[]>([]);
    const [loading, setLoading] = useState(true);
    const [tick, setTick] = useState(0);

    // Load from real API on mount and whenever `tick` changes
    useEffect(() => {
        let cancelled = false;

        if (!ENABLE_NOTIFICATIONS) {
            setLoading(false);
            return;
        }

        async function load() {
            try {
                setLoading(true);
                const page = await fetchNotifications(0, 100);
                if (cancelled) return;
                const mapped = page.content.map(mapApiNotification);
                mapped.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
                setNotifications(mapped);
            } catch {
                // Silently fail — notifications are non-critical
            } finally {
                if (!cancelled) setLoading(false);
            }
        }

        load();
        return () => {
            cancelled = true;
        };
    }, [tick]);

    const refresh = useCallback(() => setTick((t) => t + 1), []);
    const [socketConnected, setSocketConnected] = useState(false);

    const unreadOrPendingCount = useMemo(
        () => notifications.filter((n) => !n.readStatus || n.status === "pending").length,
        [notifications]
    );

    useEffect(() => {
        let client: Client | null = null;
        let pollingId: number | null = null;
        const wsUrl = getWebSocketUrl();

        if (!ENABLE_NOTIFICATIONS) {
            return;
        }
        function startPolling() {
            if (pollingId !== null) return;
            pollingId = window.setInterval(() => {
                refresh();
            }, 20000);
        }

        function stopPolling() {
            if (pollingId !== null) {
                window.clearInterval(pollingId);
                pollingId = null;
            }
        }

        function handlePush(message: IMessage) {
            if (!message.body) return;
            try {
                const payload = JSON.parse(message.body) as ApiNotification;
                const notification = mapApiNotification(payload);
                setNotifications((prev) => [notification, ...prev.filter((item) => item.id !== notification.id)]);
            } catch {
                // Ignore malformed payloads
            }
        }

        function connectWebSocket() {
            const token = getToken();

            if (!token) {
                setSocketConnected(false);
                startPolling();
                return;
            }

            client = new Client({
                webSocketFactory: () => new SockJS(`${wsUrl}?token=${encodeURIComponent(token)}`),
                connectHeaders: {
                    Authorization: `Bearer ${token}`,
                },
                debug: () => { },
                reconnectDelay: 0,
                onConnect: () => {
                    setSocketConnected(true);
                    stopPolling();

                    client?.subscribe("/user/queue/notifications", handlePush);
                },
                onStompError: () => {
                    setSocketConnected(false);
                    startPolling();
                },
                onWebSocketError: () => {
                    setSocketConnected(false);
                    startPolling();
                    client?.deactivate();
                },
                onWebSocketClose: () => {
                    setSocketConnected(false);
                    startPolling();
                },
            });

            client.activate();
            startPolling();
        }

        startPolling();

        return () => {
            if (client) {
                client.deactivate();
            }
            stopPolling();
        };
    }, [refresh]);

    // ---------------------------------------------------------------- respond
    async function respondToNotification(
        id: number,
        decision: NotificationDecision
    ) {
        // Optimistically update the local state right away
        const targetNotification = notifications.find((n) => n.id === id);
        setNotifications((prev) =>
            prev.map((n) =>
                n.id === id
                    ? { ...n, status: decision === "accept" ? "accepted" : "rejected" }
                    : n
            )
        );

        try {
            await respondNotification(id, decision);

            // ns_f4 → on acceptance redirect to the group detail page
            if (decision === "accept" && targetNotification?.groupId) {
                router.push(`/groups/${targetNotification.groupId}`);
            }
        } catch (err) {
            // Roll back optimistic update on error
            setNotifications((prev) =>
                prev.map((n) =>
                    n.id === id ? { ...n, status: "pending" } : n
                )
            );
            throw err;
        }
    }

    // ---------------------------------------------------------------- clear
    async function clearNotification(id: number) {
        setNotifications((prev) => prev.filter((n) => n.id !== id));

        try {
            await clearNotificationApi(id);
        } catch {
            // Non-critical — already removed from local state
        }
    }

    return (
        <NotificationContext.Provider
            value={{
                notifications,
                unreadOrPendingCount,
                loading,
                respondToNotification,
                clearNotification,
                refresh,
            }}
        >
            {children}
        </NotificationContext.Provider>
    );
}

export function useNotifications() {
    const context = useContext(NotificationContext);
    if (!context) {
        throw new Error("useNotifications must be used within NotificationProvider");
    }
    return context;
}