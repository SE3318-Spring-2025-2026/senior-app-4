"use client";
import Sidebar from "@/components/Sidebar";
import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import NotificationList from "@/components/NotificationList";
import { useNotifications } from "@/components/NotificationProvider";

export default function NotificationsPage() {
    const router = useRouter();
    const {
        notifications,
        loading,
        respondToNotification,
        clearNotification,
        refresh,
    } = useNotifications();

    // Always trigger a fresh fetch when this page mounts so we never show
    // stale empty state from a previously failed/incomplete provider load.
    const [isPageLoading, setIsPageLoading] = useState(true);

    useEffect(() => {
        setIsPageLoading(true);
        refresh();
    }, [refresh]);

    // Once the provider's loading flag drops to false (fetch settled),
    // mark our local page-loading as done too.
    useEffect(() => {
        if (!loading) {
            setIsPageLoading(false);
        }
    }, [loading]);

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
        <div className="min-h-screen bg-gray-950 flex">
            <Sidebar activePage="notifications" />
            <main className="flex-1 min-w-0 px-6 py-10">
                <div className="mx-auto max-w-6xl">


                    <div>
                        <button
                            onClick={handleGoBack}
                            className="text-sm text-blue-400 hover:underline"
                        >
                            ← Back
                        </button>

                        <h1 className="mt-4 text-3xl font-bold text-white">Notifications</h1>
                        <p className="mt-2 text-gray-400">
                            View your notifications and respond to pending invitations.
                        </p>
                    </div>


                    <NotificationList
                        notifications={paginatedNotifications}
                        loading={isPageLoading || loading}
                        page={page}
                        totalPages={totalPages}
                        onRespond={respondToNotification}
                        onClear={clearNotification}
                        onPrevPage={handlePrevPage}
                        onNextPage={handleNextPage}
                    />
                </div>
            </main>
        </div>
    );
}