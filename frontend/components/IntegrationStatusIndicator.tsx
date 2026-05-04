"use client";

type Props = {
    label: string;
    connected: boolean;
    connectedAt: string | null;
    loading?: boolean;
    "data-testid"?: string;
};

export default function IntegrationStatusIndicator({
    label,
    connected,
    connectedAt,
    loading = false,
    "data-testid": testId,
}: Props) {
    if (loading) {
        return (
            <div
                data-testid={testId ? `${testId}-skeleton` : undefined}
                className="animate-pulse flex items-center gap-2 px-3 py-2 rounded-lg bg-white/5 border border-white/10"
            >
                <div className="w-2 h-2 rounded-full bg-gray-700 flex-shrink-0" />
                <div className="space-y-1">
                    <div className="h-2.5 w-20 rounded bg-gray-700" />
                    <div className="h-2 w-28 rounded bg-gray-700" />
                </div>
            </div>
        );
    }

    return (
        <div
            data-testid={testId}
            className={`flex items-center gap-2 px-3 py-2 rounded-lg border transition-all ${
                connected
                    ? "bg-green-500/10 border-green-500/20"
                    : "bg-red-500/10 border-red-500/20"
            }`}
        >
            <div
                className={`w-2 h-2 rounded-full flex-shrink-0 ${
                    connected ? "bg-green-400 animate-pulse" : "bg-red-400"
                }`}
            />
            <div className="min-w-0">
                <p
                    className={`text-xs font-semibold leading-tight ${
                        connected ? "text-green-400" : "text-red-400"
                    }`}
                >
                    {label} — {connected ? "Connected" : "No Connection"}
                </p>
                <p className="text-xs text-gray-500 leading-tight mt-0.5">
                    {connectedAt
                        ? `Connected At: ${formatRelative(connectedAt)}`
                        : "Never connected"}
                </p>
            </div>
        </div>
    );
}

function formatRelative(isoString: string): string {
    const date = new Date(isoString);
    if (Number.isNaN(date.getTime())) return "unknown";

    const diff = Date.now() - date.getTime();
    if (diff < 0) return "just now";

    const minutes = Math.floor(diff / 60_000);
    if (minutes < 1) return "just now";
    if (minutes < 60) return `${minutes} mins ago`;

    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours}h ago`;

    const days = Math.floor(hours / 24);
    if (days < 30) return `${days}d ago`;

    return date.toLocaleDateString("en-US", { day: "2-digit", month: "short", year: "numeric" });
}
