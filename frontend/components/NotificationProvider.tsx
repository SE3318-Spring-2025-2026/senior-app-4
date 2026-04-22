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
import {
    fetchNotifications,
    respondNotification,
    clearNotificationApi,
} from "@/lib/notifications-api";
import {
    Notification,
    NotificationDecision,
} from "@/lib/notification-types";

// ------------------------------------------------------------------ Types

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

function mapApiNotification(n: {
    id: number;
    type: string;
    message: string;
    status: string;
    fromUserId: number | null;
    fromUserName: string | null;
    toUserId: number | null;
    groupId: number | null;
    createdAt: string;
}): Notification {
    return {
        id: n.id,
        type: String(n.type).trim().toLowerCase() as Notification["type"],
        message: n.message,
        status: String(n.status).trim().toLowerCase() as Notification["status"],
        fromUserId: n.fromUserId,
        fromUserName: n.fromUserName ?? null,
        groupId: n.groupId,
        createdAt: n.createdAt,
    };
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

        async function load() {
            try {
                setLoading(true);
                const page = await fetchNotifications(0, 50);
                if (cancelled) return;
                setNotifications(page.content.map(mapApiNotification));
            } catch {
                // Silently fail — notifications are non-critical
            } finally {
                if (!cancelled) setLoading(false);
            }
        }

        load();
        return () => { cancelled = true; };
    }, [tick]);

    const refresh = useCallback(() => setTick((t) => t + 1), []);

    const unreadOrPendingCount = useMemo(
        () => notifications.filter((n) => n.status === "pending").length,
        [notifications]
    );

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