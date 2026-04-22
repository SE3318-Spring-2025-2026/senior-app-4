"use client";

import { useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import NotificationList from "@/components/NotificationList";
import AppTopbar from "@/components/AppTopbar";
import { useNotifications } from "@/components/NotificationProvider";

export default function NotificationsPage() {
    const router = useRouter();
    const {
        notifications,
        respondToNotification,
        clearNotification,
    } = useNotifications();

    const [page, setPage] = useState(0);
    const pageSize = 4;

    const totalPages = Math.max(1, Math.ceil(notifications.length / pageSize));

    const paginatedNotifications = useMemo(() => {
        const start = page * pageSize;
        return notifications.slice(start, start + pageSize);
    }, [notifications, page]);

    function handlePrevPage() {
        setPage((prev) => Math.max(prev - 1, 0));
    }

    function handleNextPage() {
        setPage((prev) => Math.min(prev + 1, totalPages - 1));
    }

    function handleGoBack() {
        if (window.history.length > 1) {
            router.back();
        } else {
            router.push("/groups");
        }
    }

    return (
        <main className="min-h-screen bg-gray-950 px-6 py-10 text-white">
            <div className="mx-auto max-w-5xl space-y-8">
                <AppTopbar hideNotification />

                <div>
                    <button
                        onClick={handleGoBack}
                        className="text-sm text-blue-400 hover:underline"
                    >
                        ← Back
                    </button>

                    <h1 className="mt-4 text-3xl font-bold">Notifications</h1>
                    <p className="mt-2 text-gray-400">
                        View your notifications and respond to pending invitations.
                    </p>
                </div>


                <NotificationList
                    notifications={paginatedNotifications}
                    page={page}
                    totalPages={totalPages}
                    onRespond={respondToNotification}
                    onClear={clearNotification}
                    onPrevPage={handlePrevPage}
                    onNextPage={handleNextPage}
                />
            </div>
        </main>
    );
}