"use client";

import { useState } from "react";
import { Notification, NotificationDecision } from "@/lib/notification-types";

type Props = {
    notification: Notification;
    onRespond: (id: number, decision: NotificationDecision) => Promise<void>;
    onClear: (id: number) => void;
};

function getIcon(type: Notification["type"]) {
    switch (type) {
        case "membership_invite":
            return "📩";
        case "advisor_request":
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

function StatusBadge({ status }: { status: Notification["status"] }) {
    const classes: Record<string, string> = {
        pending: "bg-yellow-500/20 text-yellow-300",
        accepted: "bg-green-500/20 text-green-300",
        rejected: "bg-red-500/20 text-red-300",
        cleared: "bg-white/10 text-gray-400",
    };
    return (
        <span
            className={`rounded-full px-2 py-0.5 text-xs font-medium capitalize ${classes[status] ?? "bg-white/10 text-gray-400"}`}
        >
            {status}
        </span>
    );
}

export default function NotificationItem({ notification, onRespond, onClear }: Props) {
    const [responding, setResponding] = useState(false);

    const isInvite =
        notification.type === "membership_invite" &&
        notification.status === "pending";

    const isProcessed =
        notification.status === "accepted" ||
        notification.status === "rejected" ||
        notification.status === "cleared";

    async function handleRespond(decision: NotificationDecision) {
        setResponding(true);
        try {
            await onRespond(notification.id, decision);
        } finally {
            setResponding(false);
        }
    }

    return (
        <div className="rounded-xl border border-white/10 bg-gray-900/70 p-5 shadow-md backdrop-blur transition-all">
            <div className="flex items-start justify-between gap-4">
                {/* Left */}
                <div className="flex gap-3 min-w-0">
                    <div className="text-xl flex-shrink-0">{getIcon(notification.type)}</div>

                    <div className="min-w-0">
                        {/* Sender name — shown for invites */}
                        {notification.fromUserName && (
                            <p className="text-xs text-blue-400 font-medium mb-1">
                                From: {notification.fromUserName}
                            </p>
                        )}

                        <p className="text-sm text-white leading-relaxed">{notification.message}</p>

                        <div className="flex items-center gap-3 mt-2">
                            <p className="text-xs text-gray-500">{formatDate(notification.createdAt)}</p>
                            <StatusBadge status={notification.status} />
                        </div>
                    </div>
                </div>

                {/* Right actions */}
                <div className="flex items-center gap-2 flex-shrink-0">
                    {/* Accept / Reject — only for pending membership invites */}
                    {isInvite && (
                        <>
                            <button
                                onClick={() => handleRespond("accept")}
                                disabled={responding}
                                className="px-3 py-1.5 text-xs font-medium rounded-lg bg-green-600 hover:bg-green-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                            >
                                {responding ? "…" : "Accept"}
                            </button>
                            <button
                                onClick={() => handleRespond("reject")}
                                disabled={responding}
                                className="px-3 py-1.5 text-xs font-medium rounded-lg bg-red-600 hover:bg-red-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                            >
                                {responding ? "…" : "Reject"}
                            </button>
                        </>
                    )}

                    {/* Clear processed notifications */}
                    {isProcessed && (
                        <button
                            onClick={() => onClear(notification.id)}
                            className="px-3 py-1.5 text-xs font-medium rounded-lg bg-white/10 hover:bg-white/20 text-gray-300 transition-colors"
                        >
                            Clear
                        </button>
                    )}
                </div>
            </div>
        </div>
    );
}