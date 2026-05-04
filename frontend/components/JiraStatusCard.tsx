"use client";

import { formatDate } from "@/lib/format-utils";

type JiraIntegrationData = {
    status: string;
    jiraSpaceUrl: string | null;
    projectKey: string | null;
    connectedAt: string | null;
    message?: string | null;
};

type Props = {
    integration?: JiraIntegrationData;
};

export default function JiraStatusCard({ integration }: Props) {
    const status = integration?.status?.toLowerCase();
    const isConnected = status === "active";
    const isError = status === "error";

    const badgeClass = isConnected
        ? "bg-green-500/15 text-green-400"
        : isError
        ? "bg-red-500/15 text-red-400"
        : "bg-gray-700 text-gray-400";

    const badgeLabel = isConnected ? "Connected" : isError ? "Error" : "Not Connected";

    return (
        <div className="rounded-2xl border border-indigo-500/20 bg-indigo-500/10 p-6 shadow-lg shadow-indigo-950/20 backdrop-blur transition-all hover:border-indigo-400/40 hover:bg-indigo-500/15">
            {/* Title */}
            <div className="flex items-center justify-between mb-4">
                <h3 className="text-sm text-gray-400">JIRA Integration</h3>

                <span className={`text-xs px-3 py-1 rounded-full font-medium ${badgeClass}`}>
                    {badgeLabel}
                </span>
            </div>

            {/* Content */}
            <div className="space-y-2 text-sm">
                <p className="text-gray-300">
                    <span className="text-gray-500">Space URL:</span>{" "}
                    {integration?.jiraSpaceUrl || "-"}
                </p>

                <p className="text-gray-300">
                    <span className="text-gray-500">Project Key:</span>{" "}
                    {integration?.projectKey || "-"}
                </p>

                <p className="text-gray-300">
                    <span className="text-gray-500">Connected At:</span>{" "}
                    {formatDate(integration?.connectedAt)}
                </p>
            </div>
        </div>
    );
}
