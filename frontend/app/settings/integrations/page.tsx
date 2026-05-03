"use client";

import { useState, useEffect } from "react";
import AppTopbar from "@/components/AppTopbar";
import IntegrationSettingsForm from "@/components/IntegrationSettingsForm";
import { fetchGithubIntegration, fetchJiraIntegration } from "@/lib/integrations-api";
import { fetchCurrentUserGroupId } from "@/lib/groups-api";

interface IntegrationStatus {
    github: "CONNECTED" | "DISCONNECTED";
    jira: "CONNECTED" | "DISCONNECTED";
}

export default function IntegrationSettingsPage() {
    const [groupId, setGroupId] = useState<number | null>(null);
    const [status, setStatus] = useState<IntegrationStatus | null>(null);
    const [statusLoading, setStatusLoading] = useState(true);

    async function loadStatus(gid: number) {
        try {
            const [github, jira] = await Promise.all([
                fetchGithubIntegration(gid),
                fetchJiraIntegration(gid),
            ]);
            setStatus({
                github: github.data?.status === "active" ? "CONNECTED" : "DISCONNECTED",
                jira: jira.data?.status === "active" ? "CONNECTED" : "DISCONNECTED",
            });
        } catch {
            // endpoints unreachable — leave status null
        } finally {
            setStatusLoading(false);
        }
    }

    useEffect(() => {
        fetchCurrentUserGroupId().then((gid) => {
            setGroupId(gid);
            if (gid) loadStatus(gid);
            else setStatusLoading(false);
        });
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    return (
        <main className="min-h-screen bg-gray-950 px-6 py-10 text-white">
            <div className="mx-auto max-w-3xl space-y-8">
                <AppTopbar />

                <div>
                    <h1 className="text-3xl font-bold">Integration Settings</h1>
                    <p className="mt-2 text-gray-400">
                        Manage your GitHub and JIRA integration keys.
                    </p>
                </div>

                {/* Live Status Indicators */}
                <div className="rounded-2xl border border-white/10 bg-gray-900/70 p-6 shadow-lg shadow-black/20 backdrop-blur">
                    <h2 className="text-lg font-semibold text-white mb-4">Connection Status</h2>

                    {statusLoading ? (
                        <div className="flex gap-4">
                            {["GitHub", "JIRA"].map((name) => (
                                <div key={name} className="flex items-center gap-2">
                                    <div className="w-2.5 h-2.5 rounded-full bg-gray-600 animate-pulse" />
                                    <span className="text-sm text-gray-400">{name}</span>
                                </div>
                            ))}
                        </div>
                    ) : (
                        <div className="flex flex-wrap gap-4">
                            <div
                                data-testid="status-github"
                                className={`flex items-center gap-2 px-3 py-1.5 rounded-lg border text-sm font-medium text-white ${
                                    status?.github === "CONNECTED"
                                        ? "bg-green-500 border-green-600"
                                        : "bg-red-500 border-red-600"
                                }`}
                            >
                                <div className="w-2 h-2 rounded-full bg-white/70" />
                                GitHub — {status?.github ?? "UNKNOWN"}
                            </div>

                            <div
                                data-testid="status-jira"
                                className={`flex items-center gap-2 px-3 py-1.5 rounded-lg border text-sm font-medium text-white ${
                                    status?.jira === "CONNECTED"
                                        ? "bg-green-500 border-green-600"
                                        : "bg-red-500 border-red-600"
                                }`}
                            >
                                <div className="w-2 h-2 rounded-full bg-white/70" />
                                JIRA — {status?.jira ?? "UNKNOWN"}
                            </div>
                        </div>
                    )}
                </div>

                {/* Integration Settings Form */}
                <IntegrationSettingsForm
                    groupId={groupId}
                    onSuccess={() => groupId ? loadStatus(groupId) : undefined}
                />
            </div>
        </main>
    );
}
