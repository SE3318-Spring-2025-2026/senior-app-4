"use client";

import { Notification, NotificationDecision } from "@/lib/notification-types";

type Props = {
    notification: Notification;
    onRespond: (id: number, decision: NotificationDecision) => void;
    onClear: (id: number) => void;
};

function getIcon(type: Notification["type"]) {
    switch (type) {
        case "membership_invite":
            return "📩";
        case "advisor_request":
            return "👨‍🏫";
        case "advisor_decision":
            return "👨‍🏫";
        case "system_alert":
            return "⚠️";
        case "group_disbanded":
            return "🚫";
        default:
            return "🔔";
    }
}

function formatDate(date: string) {
    return new Date(date).toLocaleString("en-US", {
        day: "numeric",
        month: "short",
        hour: "2-digit",
        minute: "2-digit",
    });
}

export default function NotificationItem({
    notification,
    onRespond,
    onClear,
}: Props) {
    const isInvite =
        notification.type === "membership_invite" &&
        notification.status === "pending";

    const isProcessed =
        notification.status === "accepted" ||
        notification.status === "rejected" ||
        notification.status === "cleared";

    return (
        <div className="rounded-xl border border-white/10 bg-gray-900/70 p-5 shadow-md backdrop-blur">
            <div className="flex items-start justify-between gap-4">
                {/* Left */}
                <div className="flex gap-3">
                    <div className="text-xl">{getIcon(notification.type)}</div>

                    <div>
                        <p className="text-sm text-white">{notification.message}</p>
                        <p className="text-xs text-gray-400 mt-1">
                            {formatDate(notification.createdAt)}
                        </p>
                    </div>
                </div>

                {/* Right actions */}
                <div className="flex items-center gap-2">
                    {/* Accept / Reject */}
                    {isInvite && (
                        <>
                            <button
                                onClick={() => onRespond(notification.id, "accept")}
                                className="px-3 py-1 text-xs rounded-md bg-green-600 hover:bg-green-500"
                            >
                                Accept
                            </button>
                            <button
                                onClick={() => onRespond(notification.id, "reject")}
                                className="px-3 py-1 text-xs rounded-md bg-red-600 hover:bg-red-500"
                            >
                                Reject
                            </button>
                        </>
                    )}

                    {/* Clear */}
                    {isProcessed && (
                        <button
                            onClick={() => onClear(notification.id)}
                            className="px-3 py-1 text-xs rounded-md bg-white/10 hover:bg-white/20 text-gray-300"
                        >
                            Clear
                        </button>
                    )}
                </div>
            </div>
        </div>
    );
}