"use client";

import Link from "next/link";
import NotificationBell from "@/components/NotificationBell";

type Props = {
    title?: string;
    notificationCount?: number;
    hideNotification?: boolean;
};

export default function AppTopbar({
    title = "SPMS",
    notificationCount = 0,
    hideNotification = false,
}: Props) {
    return (
        <header className="mb-8 flex items-center justify-end">
            {!hideNotification && (
                <Link href="/notifications" aria-label="Open notifications">
                    <NotificationBell count={notificationCount} />
                </Link>
            )}
        </header>
    );
}