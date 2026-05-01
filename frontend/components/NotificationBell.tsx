"use client";

type Props = {
    count: number;
};

export default function NotificationBell({ count }: Props) {
    return (
        <div className="relative inline-flex items-center justify-center rounded-xl border border-white/10 bg-gray-900/70 p-3 shadow-lg shadow-black/20 backdrop-blur">
            <span className="text-lg">🔔</span>

            {count > 0 && (
                <span className="absolute -right-1 -top-1 inline-flex min-w-[20px] items-center justify-center rounded-full bg-red-500 px-1.5 py-0.5 text-xs font-semibold text-white">
                    {count}
                </span>
            )}
        </div>
    );
}