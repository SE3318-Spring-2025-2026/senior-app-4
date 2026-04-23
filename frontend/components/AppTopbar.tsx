"use client";

import NotificationDropdown from "@/components/NotificationDropdown";

type Props = {
    hideNotification?: boolean;
};

export default function AppTopbar({
    hideNotification = false,
}: Props) {
    return (
        <header className="mb-8 flex items-center justify-end">
            {!hideNotification && <NotificationDropdown />}
        </header>
    );
}
