"use client";

import { GithubIntegration } from "@/lib/github-types";
import { formatDate } from "@/lib/format-utils";

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

    return (
        <div className="bg-gray-900/80 border border-white/10 rounded-2xl p-6 shadow-lg">
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