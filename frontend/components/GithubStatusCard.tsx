"use client";

import { GithubIntegration } from "@/lib/github-types";

type Props = {
    integration?: {
        status: string;
        organizationName?: string | null;
        connectedAt?: string | null;
    } | null;
};

export default function GithubStatusCard({ integration }: Props) {
    const status = integration?.status?.toLowerCase();

    const isConnected =
        !!integration &&
        !!integration.organizationName &&
        status !== "inactive" &&
        status !== "error";

    const formatDate = (date?: string | null) => {
        if (!date) return "-";
        return new Date(date).toLocaleString("en-US", {
            dateStyle: "medium",
            timeStyle: "short",
        });
    };

    return (
        <div className="rounded-2xl border border-blue-500/20 bg-blue-500/10 p-6 shadow-lg shadow-blue-950/20 backdrop-blur transition-all hover:border-blue-400/40 hover:bg-blue-500/15">
            {/* Title */}
            <div className="flex items-center justify-between mb-4">
                <h3 className="text-sm text-gray-400">GitHub Integration</h3>

                <span
                    className={`text-xs px-3 py-1 rounded-full font-medium ${isConnected
                        ? "bg-green-500/15 text-green-400"
                        : "bg-gray-700 text-gray-400"
                        }`}
                >
                    {isConnected ? "Connected" : "Not Connected"}
                </span>
            </div>

            {/* Content */}
            <div className="space-y-2 text-sm">
                <p className="text-gray-300">
                    <span className="text-gray-500">Organization:</span>{" "}
                    {integration?.organizationName || "-"}
                </p>

                <p className="text-gray-300">
                    <span className="text-gray-500">Connected At:</span>{" "}
                    {formatDate(integration?.connectedAt)}
                </p>
            </div>
        </div>
    );
}