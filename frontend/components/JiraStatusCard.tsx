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
    const isConnected = integration?.status === "active";

    return (
        <div className="bg-gray-900/80 border border-white/10 rounded-2xl p-6 shadow-lg">
            {/* Title */}
            <div className="flex items-center justify-between mb-4">
                <h3 className="text-sm text-gray-400">JIRA Integration</h3>

                <span
                    className={`text-xs px-3 py-1 rounded-full font-medium ${
                        isConnected
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
