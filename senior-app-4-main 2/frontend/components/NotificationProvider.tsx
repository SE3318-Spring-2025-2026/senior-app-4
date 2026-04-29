"use client";

import {
    createContext,
    useContext,
    useMemo,
    useState,
    ReactNode,
} from "react";
import { mockNotifications } from "@/lib/mock-notifications";
import { mockGroups } from "@/lib/mock-groups";
import {
    Notification,
    NotificationDecision,
} from "@/lib/notification-types";

type NotificationContextType = {
    notifications: Notification[];
    unreadOrPendingCount: number;
    respondToNotification: (id: number, decision: NotificationDecision) => void;
    clearNotification: (id: number) => void;
};

const NotificationContext = createContext<NotificationContextType | undefined>(
    undefined
);

type Props = {
    children: ReactNode;
};

const CURRENT_USER = {
    userId: 999,
    studentId: "2201999",
    fullName: "Elif Test",
    role: "student" as const,
    githubUsername: "eliftest",
};

export function NotificationProvider({ children }: Props) {
    const [notifications, setNotifications] =
        useState<Notification[]>(mockNotifications);

    const unreadOrPendingCount = useMemo(() => {
        return notifications.filter(
            (notification) => notification.status === "pending"
        ).length;
    }, [notifications]);

    function respondToNotification(id: number, decision: NotificationDecision) {
        setNotifications((prev) =>
            prev.map((notification) => {
                if (notification.id !== id) return notification;

                if (
                    decision === "accept" &&
                    notification.type === "membership_invite" &&
                    notification.groupId
                ) {
                    const targetGroup = mockGroups.find(
                        (group) => group.groupId === notification.groupId
                    );

                    if (targetGroup) {
                        const alreadyMember = targetGroup.members.some(
                            (member) => member.studentId === CURRENT_USER.studentId
                        );

                        if (!alreadyMember) {
                            targetGroup.members.push(CURRENT_USER);
                            targetGroup.memberCount = targetGroup.members.length;
                            targetGroup.updatedAt = new Date().toISOString();
                        }
                    }
                }

                return {
                    ...notification,
                    status: decision === "accept" ? "accepted" : "rejected",
                    message:
                        decision === "accept"
                            ? `${notification.message} (Accepted)`
                            : `${notification.message} (Rejected)`,
                };
            })
        );
    }

    function clearNotification(id: number) {
        setNotifications((prev) =>
            prev.filter((notification) => notification.id !== id)
        );
    }

    return (
        <NotificationContext.Provider
            value={{
                notifications,
                unreadOrPendingCount,
                respondToNotification,
                clearNotification,
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