"use client";

import NotificationItem from "@/components/NotificationItem";
import {
    Notification,
    NotificationDecision,
} from "@/lib/notification-types";

type Props = {
    notifications: Notification[];
    page: number;
    totalPages: number;
    onRespond: (id: number, decision: NotificationDecision) => Promise<void>;
    onClear: (id: number) => void;
    onPrevPage: () => void;
    onNextPage: () => void;
};

export default function NotificationList({
    notifications,
    page,
    totalPages,
    onRespond,
    onClear,
    onPrevPage,
    onNextPage,
}: Props) {
    if (notifications.length === 0) {
        return (
            <div className="rounded-2xl border border-white/10 bg-gray-900/70 p-8 text-center shadow-lg shadow-black/20 backdrop-blur">
                <h2 className="text-lg font-semibold text-white">No notifications</h2>
                <p className="mt-2 text-sm text-gray-400">
                    You do not have any notifications right now.
                </p>
            </div>
        );
    }

    return (
        <div className="space-y-5">
            <div className="space-y-4">
                {notifications.map((notification) => (
                    <NotificationItem
                        key={notification.id}
                        notification={notification}
                        onRespond={onRespond}
                        onClear={onClear}
                    />
                ))}
            </div>

            <div className="flex items-center justify-center gap-3">
                <button
                    onClick={onPrevPage}
                    disabled={page === 0}
                    className="rounded-xl border border-white/10 bg-gray-900/70 px-4 py-2 text-sm text-gray-300 transition hover:bg-white/5 disabled:cursor-not-allowed disabled:opacity-40"
                >
                    Previous
                </button>

                <span className="text-sm text-gray-400">
                    Page <span className="text-white">{page + 1}</span> /{" "}
                    <span className="text-white">{totalPages}</span>
                </span>

                <button
                    onClick={onNextPage}
                    disabled={page >= totalPages - 1}
                    className="rounded-xl border border-white/10 bg-gray-900/70 px-4 py-2 text-sm text-gray-300 transition hover:bg-white/5 disabled:cursor-not-allowed disabled:opacity-40"
                >
                    Next
                </button>
            </div>
        </div>
    );
}