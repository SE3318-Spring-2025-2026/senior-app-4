"use client";

import { useState } from "react";
import { Notification, NotificationDecision } from "@/lib/notification-types";

type Props = {
    notification: Notification;
    onRespond: (id: number, decision: NotificationDecision) => Promise<void>;
    onClear: (id: number) => void;
};

function normalizeType(value: string) {
    return String(value ?? "")
        .normalize("NFKC")
        .replace(/[\s\u200B-\u200D\uFEFF]/g, "")
        .replace(/ı/g, "i")
        .replace(/İ/g, "i")
        .toUpperCase();
}

function getTypeMeta(value: string) {
    const key = normalizeType(value);
    switch (key) {
        case "ADVISOR_ASSIGNMENT":
            return { icon: "🧑‍🏫", label: "Advisor Assignment", color: "bg-blue-500/15 text-blue-300" };
        case "JURY_ASSIGNMENT":
            return { icon: "🧑‍⚖️", label: "Jury Assignment", color: "bg-purple-500/15 text-purple-300" };
        case "GROUP_ASSIGNMENT":
            return { icon: "👥", label: "Group Assignment", color: "bg-emerald-500/15 text-emerald-300" };
        case "SCHEDULE_CHANGE":
            return { icon: "🗓️", label: "Schedule Change", color: "bg-orange-500/15 text-orange-300" };
        case "MEETING_REMINDER":
            return { icon: "⏰", label: "Meeting Reminder", color: "bg-red-500/15 text-red-300" };
        case "GENERAL":
            return { icon: "ℹ️", label: "General", color: "bg-slate-500/15 text-slate-300" };
        case "MEMBERSHIP_INVITE":
            return { icon: "📩", label: "Membership Invite", color: "bg-sky-500/15 text-sky-300" };
        case "ADVISOR_REQUEST":
        case "ADVISOR_DECISION":
            return { icon: "👨‍🏫", label: "Advisor Notification", color: "bg-slate-500/15 text-slate-300" };
        case "SYSTEM_ALERT":
            return { icon: "⚠️", label: "System Alert", color: "bg-yellow-500/15 text-yellow-300" };
        case "GROUP_DISBANDED":
            return { icon: "🚫", label: "Group Disbanded", color: "bg-rose-500/15 text-rose-300" };
        default:
            return { icon: "🔔", label: "Notification", color: "bg-white/10 text-gray-300" };
    }
}

function formatDate(date: string) {
    return new Date(date).toLocaleString(undefined, {
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
        revoked: "bg-orange-500/20 text-orange-300",
    };
    return (
        <span className={`rounded-full px-2 py-0.5 text-xs font-medium capitalize ${classes[status] ?? "bg-white/10 text-gray-400"}`}>
            {status}
        </span>
    );
}

export default function NotificationItem({ notification, onRespond, onClear }: Props) {
    const [responding, setResponding] = useState(false);
    const meta = getTypeMeta(notification.type);
    const isInvite = normalizeType(notification.type) === "MEMBERSHIP_INVITE" && notification.status === "pending";

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
            <div className="flex flex-col gap-4">
                <div className="flex gap-3">
                    <div className={`flex h-11 w-11 items-center justify-center rounded-2xl ${meta.color}`}>
                        <span className="text-xl">{meta.icon}</span>
                    </div>

                    <div className="min-w-0 flex-1">
                        <div className="flex flex-wrap items-center gap-2">
                            <span className="text-xs uppercase tracking-[0.18em] text-gray-400">{meta.label}</span>
                            {!notification.readStatus && (
                                <span className="rounded-full bg-blue-500/20 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-[0.15em] text-blue-200">
                                    New
                                </span>
                            )}
                        </div>

                        {notification.fromUserName && (
                            <p className="mt-2 text-xs text-blue-400 font-medium">From: {notification.fromUserName}</p>
                        )}

                        <p className="mt-2 text-sm text-white leading-relaxed">{notification.message}</p>

                        <div className="mt-3 flex flex-wrap items-center gap-3 text-xs text-gray-500">
                            <span>{formatDate(notification.createdAt)}</span>
                            <StatusBadge status={notification.status} />
                        </div>
                    </div>
                </div>

                <div className="flex flex-wrap gap-2">
                    {isInvite && (
                        <>
                            <button
                                onClick={() => handleRespond("accept")}
                                disabled={responding}
                                className="rounded-lg bg-green-600 px-3 py-1.5 text-xs font-medium text-white transition hover:bg-green-500 disabled:cursor-not-allowed disabled:opacity-50"
                            >
                                {responding ? "…" : "Accept"}
                            </button>
                            <button
                                onClick={() => handleRespond("reject")}
                                disabled={responding}
                                className="rounded-lg bg-red-600 px-3 py-1.5 text-xs font-medium text-white transition hover:bg-red-500 disabled:cursor-not-allowed disabled:opacity-50"
                            >
                                {responding ? "…" : "Reject"}
                            </button>
                        </>
                    )}
                    <button
                        onClick={() => onClear(notification.id)}
                        className="rounded-lg bg-white/10 px-3 py-1.5 text-xs font-medium text-gray-300 transition hover:bg-white/20"
                    >
                        Delete
                    </button>
                </div>
            </div>
        </div>
    );
}
