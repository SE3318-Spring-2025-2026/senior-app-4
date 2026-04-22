"use client";

import { useState } from "react";
import { Notification, NotificationDecision } from "@/lib/notification-types";

type Props = {
    notification: Notification;
    onRespond: (id: number, decision: NotificationDecision) => Promise<void>;
    onClear: (id: number) => void;
};

function getIcon(type: string) {
    const cleanType = type
        .normalize("NFKC")
        .replace(/[\s\u200B-\u200D\uFEFF]/g, "")
        .toLowerCase()
        .replace(/ı/g, "i")
        .replace(/İ/g, "i");

    switch (cleanType) {
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
            className={`rounded-full px-2 py-0.5 text-xs font-medium capitalize ${classes[status] ?? "bg-white/10 text-gray-400"
                }`}
        >
            {status}
        </span>
    );
}

export default function NotificationItem({ notification, onRespond, onClear }: Props) {
    const [responding, setResponding] = useState(false);

    // 🔥 KRİTİK FIX BURADA
    function clean(value: string) {
        return String(value ?? "")
            .normalize("NFKC")
            .replace(/[\s\u200B-\u200D\uFEFF]/g, "")
            .toLowerCase();
    }

    function fixTurkishChars(str: string) {
        return str
            .replace(/ı/g, "i")
            .replace(/İ/g, "i");
    }

    const safeType = fixTurkishChars(clean(notification.type));
    const safeStatus = fixTurkishChars(clean(notification.status));

    const isInvite =
        safeType === "membership_invite" &&
        safeStatus === "pending";

    const isProcessed =
        safeStatus === "accepted" ||
        safeStatus === "rejected" ||
        safeStatus === "cleared";

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

                {/* ÜST KISIM */}
                <div className="flex gap-3">
                    <div className="text-xl flex-shrink-0">
                        {getIcon(notification.type)}
                    </div>

                    <div className="min-w-0">
                        {notification.fromUserName && (
                            <p className="text-xs text-blue-400 font-medium mb-1">
                                From: {notification.fromUserName}
                            </p>
                        )}

                        <p className="text-sm text-white leading-relaxed">
                            {notification.message}
                        </p>

                        <div className="flex items-center gap-3 mt-2">
                            <p className="text-xs text-gray-500">
                                {formatDate(notification.createdAt)}
                            </p>
                            <StatusBadge status={notification.status} />
                        </div>
                    </div>
                </div>

                {/* 🔥 BUTONLARI ALTA ALDIK (KESİN GÖRÜNSÜN DİYE) */}
                <div className="flex gap-2 flex-wrap">

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