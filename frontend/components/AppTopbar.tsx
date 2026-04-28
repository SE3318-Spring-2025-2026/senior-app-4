"use client";

import Link from "next/link";
import NotificationDropdown from "@/components/NotificationDropdown";

type Props = {
    title?: string;
    hideNotification?: boolean;
};

export default function AppTopbar({
    title = "SPMS",
    hideNotification = false,
}: Props) {
    return (
        <header className="mb-8 flex items-center justify-between">
            <h1 className="text-xl font-semibold text-white">{title}</h1>

            {!hideNotification && <NotificationDropdown />}
        </header>
    );
}