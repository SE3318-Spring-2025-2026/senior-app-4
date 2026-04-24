"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { useNotifications } from "@/components/NotificationProvider";
import { markNotificationRead } from "@/lib/notifications-api";
import { Notification, NotificationType } from "@/lib/notification-types";

function getIcon(type: NotificationType): string {
    switch (type) {
        case "membership_invite": return "📩";
        case "advisor_request":
        case "advisor_decision": return "👨‍🏫";
        case "group_disbanded": return "🚫";
        case "system_alert": return "⚠️";
        default: return "🔔";
    }
}

function formatDate(iso: string): string {
    return new Date(iso).toLocaleString("en-US", {
        day: "numeric",
        month: "short",
        hour: "2-digit",
        minute: "2-digit",
    });
}

export default function NotificationDropdown() {
    const router = useRouter();
    const { notifications, unreadOrPendingCount, loading, refresh } = useNotifications();
    const [open, setOpen] = useState(false);
    const panelRef = useRef<HTMLDivElement>(null);
    const buttonRef = useRef<HTMLButtonElement>(null);

    // Close on outside click
    useEffect(() => {
        function handleClick(e: MouseEvent) {
            if (
                panelRef.current && !panelRef.current.contains(e.target as Node) &&
                buttonRef.current && !buttonRef.current.contains(e.target as Node)
            ) {
                setOpen(false);
            }
        }
        if (open) document.addEventListener("mousedown", handleClick);
        return () => document.removeEventListener("mousedown", handleClick);
    }, [open]);

    // Refresh when dropdown opens
    useEffect(() => {
        if (open) refresh();
    }, [open, refresh]);

    async function handleNotificationClick(n: Notification) {
        if (n.status === "pending") {
            try {
                await markNotificationRead(n.id);
                refresh();
            } catch {
                // best-effort
            }
        }
        if (n.groupId) {
            setOpen(false);
            router.push(`/groups/${n.groupId}`);
        }
    }

    const preview = notifications.slice(0, 10);

    return (
        <div className="relative">
            {/* Bell button */}
            <button
                ref={buttonRef}
                onClick={() => setOpen((v) => !v)}
                aria-label="Open notifications"
                className="relative inline-flex items-center justify-center rounded-xl border border-white/10 bg-gray-900/70 p-3 shadow-lg shadow-black/20 backdrop-blur transition-colors hover:bg-white/5 active:scale-95"
            >
                <span className="text-lg">🔔</span>
                {unreadOrPendingCount > 0 && (
                    <span className="absolute -right-1 -top-1 inline-flex min-w-[20px] items-center justify-center rounded-full bg-red-500 px-1.5 py-0.5 text-xs font-semibold text-white">
                        {unreadOrPendingCount > 99 ? "99+" : unreadOrPendingCount}
                    </span>
                )}
            </button>

            {/* Dropdown panel */}
            {open && (
                <div
                    ref={panelRef}
                    className="absolute right-0 top-full z-50 mt-2 w-80 rounded-2xl border border-white/8 bg-gray-900 shadow-2xl shadow-black/40"
                >
                    {/* Header */}
                    <div className="flex items-center justify-between border-b border-white/8 px-4 py-3">
                        <span className="text-sm font-semibold text-white">Notifications</span>
                        {unreadOrPendingCount > 0 && (
                            <span className="text-xs text-gray-500">{unreadOrPendingCount} unread</span>
                        )}
                    </div>

                    {/* Body */}
                    <div className="max-h-96 overflow-y-auto">
                        {loading ? (
                            <div className="flex items-center justify-center py-10">
                                <svg className="w-5 h-5 animate-spin text-blue-500" fill="none" viewBox="0 0 24 24">
                                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                                </svg>
                            </div>
                        ) : preview.length === 0 ? (
                            <div className="py-10 text-center">
                                <p className="text-sm text-gray-400">No notifications</p>
                            </div>
                        ) : (
                            <ul>
                                {preview.map((n) => (
                                    <li key={n.id}>
                                        <button
                                            onClick={() => handleNotificationClick(n)}
                                            className={`w-full px-4 py-3 text-left transition-colors hover:bg-white/5 ${
                                                n.status === "pending" ? "bg-white/[0.03]" : ""
                                            }`}
                                        >
                                            <div className="flex items-start gap-3">
                                                <span className="mt-0.5 text-base">{getIcon(n.type)}</span>
                                                <div className="min-w-0 flex-1">
                                                    <div className="flex items-center gap-2">
                                                        <p className="truncate text-sm text-white">
                                                            {n.message}
                                                        </p>
                                                        {n.status === "pending" && (
                                                            <span className="h-2 w-2 flex-shrink-0 rounded-full bg-blue-500" />
                                                        )}
                                                    </div>
                                                    <p className="mt-1 text-xs text-gray-600">
                                                        {formatDate(n.createdAt)}
                                                    </p>
                                                </div>
                                            </div>
                                        </button>
                                    </li>
                                ))}
                            </ul>
                        )}
                    </div>

                    {/* Footer */}
                    <div className="border-t border-white/8 px-4 py-2">
                        <button
                            onClick={() => { setOpen(false); router.push("/notifications"); }}
                            className="w-full py-1 text-center text-xs text-blue-400 transition-colors hover:text-blue-300"
                        >
                            View all notifications →
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
}
