"use client";

import { useState, useEffect } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { toast } from "sonner";
import ConfirmDialog from "@/components/ConfirmDialog";
import AppTopbar from "@/components/AppTopbar";
import { fetchJiraIntegration, bindJiraIntegration, type JiraIntegrationApiResponse } from "@/lib/integrations-api";
import { fetchGroupDetail } from "@/lib/groups-api";
import { getUser } from "@/lib/auth";
import apiClient from "@/lib/client";

export default function JiraIntegrationPage() {
    const params = useParams();
    const groupId = Number(params.groupId);

    const currentUser = getUser();

    const [group, setGroup] = useState<any>(null);
    const [integration, setIntegration] = useState<JiraIntegrationApiResponse | null>(null);
    const [loading, setLoading] = useState(true);

    // Form states
    const [jiraSpaceUrl, setJiraSpaceUrl] = useState("");
    const [email, setEmail] = useState("");
    const [apiKey, setApiKey] = useState("");
    const [projectKey, setProjectKey] = useState("");
    const [bindLoading, setBindLoading] = useState(false);

    // Unbind states
    const [showConfirm, setShowConfirm] = useState(false);
    const [unbindLoading, setUnbindLoading] = useState(false);

    useEffect(() => {
        let cancelled = false;

        const loadData = async () => {
            try {
                const [groupData, integrationData] = await Promise.all([
                    fetchGroupDetail(groupId),
                    fetchJiraIntegration(groupId),
                ]);

                if (cancelled) return;
                setGroup(groupData);
                setIntegration(integrationData);
            } catch (err) {
                console.error("Failed to load JIRA integration page:", err);
            } finally {
                if (!cancelled) setLoading(false);
            }
        };

        if (!Number.isNaN(groupId)) loadData();

        return () => { cancelled = true; };
    }, [groupId]);

    const isLeader = group?.leaderId === currentUser?.userId;

    const isConnected =
        !!integration?.data?.jiraSpaceUrl &&
        integration?.data?.status !== "inactive" &&
        integration?.data?.status !== "error";

    async function handleBind(e: React.FormEvent) {
        e.preventDefault();

        if (!jiraSpaceUrl.trim() || !email.trim() || !apiKey.trim() || !projectKey.trim()) {
            toast.error("All fields are required.");
            return;
        }

        setBindLoading(true);
        try {
            await bindJiraIntegration(groupId, jiraSpaceUrl.trim(), email.trim(), apiKey.trim(), projectKey.trim());

            const updated = await fetchJiraIntegration(groupId);
            setIntegration(updated);

            setJiraSpaceUrl("");
            setEmail("");
            setApiKey("");
            setProjectKey("");
            toast.success("JIRA space successfully connected.");
        } catch (error: any) {
            toast.error(error.message || "Failed to connect JIRA.");
        } finally {
            setBindLoading(false);
        }
    }

    async function handleUnbind() {
        setUnbindLoading(true);
        try {
            await apiClient.delete(`/groups/${groupId}/integrations/jira`);
            const updated = await fetchJiraIntegration(groupId);
            setIntegration(updated);
            toast.success("JIRA integration unbound successfully.");
            setShowConfirm(false);
        } catch (error) {
            console.error(error);
        } finally {
            setUnbindLoading(false);
        }
    }

    if (loading) {
        return (
            <main className="min-h-screen bg-gray-950 flex items-center justify-center text-white">
                Loading...
            </main>
        );
    }

    return (
        <>
            <main className="min-h-screen bg-gray-950 px-6 py-10 text-white">
                <div className="mx-auto max-w-4xl space-y-8">
                    <AppTopbar />

                    <Link
                        href={`/groups/${groupId}`}
                        className="text-sm text-blue-400 hover:underline"
                    >
                        ← Back to group
                    </Link>

                    <div>
                        <h1 className="text-3xl font-bold">JIRA Integration</h1>
                        <p className="mt-2 text-gray-400">
                            Connect your group with a JIRA space to sync issues and progress.
                        </p>
                    </div>

                    {isConnected && (
                        <div className="rounded-2xl border border-green-500/20 bg-green-500/5 p-6 shadow-lg shadow-black/20 backdrop-blur flex items-start gap-4">
                            <div className="w-10 h-10 rounded-full bg-green-500/20 flex items-center justify-center shrink-0">
                                <svg className="w-6 h-6 text-green-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                                </svg>
                            </div>
                            <div className="space-y-1">
                                <h3 className="text-lg font-semibold text-white">JIRA Space Connected</h3>
                                <p className="text-sm text-gray-400">
                                    Space URL:{" "}
                                    <span className="text-gray-300 font-mono bg-white/5 px-2 py-0.5 rounded">
                                        {integration?.data?.jiraSpaceUrl}
                                    </span>
                                </p>
                                <p className="text-sm text-gray-400">
                                    Project Key:{" "}
                                    <span className="text-gray-300 font-mono bg-white/5 px-2 py-0.5 rounded">
                                        {integration?.data?.projectKey}
                                    </span>
                                </p>
                                <div className="mt-3 inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-green-500/10 border border-green-500/20">
                                    <div className="w-2 h-2 rounded-full bg-green-400 animate-pulse" />
                                    <span className="text-xs font-medium text-green-400">ACTIVE</span>
                                </div>
                            </div>
                        </div>
                    )}

                    {isLeader ? (
                        <>
                            {!isConnected && (
                                <div className="rounded-2xl border border-white/10 bg-gray-900/70 p-6 shadow-lg shadow-black/20 backdrop-blur">
                                    <h3 className="text-lg font-semibold text-white mb-4">Connect JIRA Account</h3>
                                    <form onSubmit={handleBind} className="space-y-4">
                                        <div>
                                            <label className="block text-sm font-medium text-gray-300 mb-1.5">JIRA Space URL</label>
                                            <input
                                                type="url"
                                                required
                                                placeholder="https://your-domain.atlassian.net"
                                                value={jiraSpaceUrl}
                                                onChange={(e) => setJiraSpaceUrl(e.target.value)}
                                                className="w-full bg-black/40 border border-white/10 text-white rounded-xl px-4 py-3 focus:ring-1 focus:ring-blue-500 outline-none transition-all"
                                            />
                                        </div>
                                        <div>
                                            <label className="block text-sm font-medium text-gray-300 mb-1.5">Atlassian Email</label>
                                            <input
                                                type="email"
                                                required
                                                placeholder="you@example.com"
                                                value={email}
                                                onChange={(e) => setEmail(e.target.value)}
                                                className="w-full bg-black/40 border border-white/10 text-white rounded-xl px-4 py-3 focus:ring-1 focus:ring-blue-500 outline-none transition-all"
                                            />
                                        </div>
                                        <div>
                                            <label className="block text-sm font-medium text-gray-300 mb-1.5">Project Key</label>
                                            <input
                                                type="text"
                                                required
                                                placeholder="e.g. PROJ"
                                                value={projectKey}
                                                onChange={(e) => setProjectKey(e.target.value)}
                                                className="w-full bg-black/40 border border-white/10 text-white rounded-xl px-4 py-3 focus:ring-1 focus:ring-blue-500 outline-none transition-all"
                                            />
                                        </div>
                                        <div>
                                            <label className="block text-sm font-medium text-gray-300 mb-1.5">API Token</label>
                                            <input
                                                type="password"
                                                required
                                                placeholder="Paste your Atlassian API token here"
                                                value={apiKey}
                                                onChange={(e) => setApiKey(e.target.value)}
                                                className="w-full bg-black/40 border border-white/10 text-white rounded-xl px-4 py-3 focus:ring-1 focus:ring-blue-500 outline-none transition-all"
                                            />
                                        </div>
                                        <div className="pt-2">
                                            <button
                                                type="submit"
                                                disabled={bindLoading}
                                                className="w-full sm:w-auto bg-blue-600 text-white px-6 py-3 rounded-xl font-medium hover:bg-blue-500 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                                            >
                                                {bindLoading ? (
                                                    <><div className="w-4 h-4 border-2 border-white/20 border-t-white rounded-full animate-spin" /> Connecting...</>
                                                ) : "Connect JIRA"}
                                            </button>
                                        </div>
                                    </form>
                                </div>
                            )}

                            {isConnected && (
                                <div className="rounded-2xl border border-white/10 bg-gray-900/70 p-6 shadow-lg shadow-black/20 backdrop-blur">
                                    <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                                        <div>
                                            <h3 className="text-lg font-semibold text-white">
                                                Disconnect JIRA Space
                                            </h3>
                                            <p className="mt-1 text-sm text-gray-400">
                                                Remove the current JIRA integration and stop syncing.
                                            </p>
                                        </div>

                                        <button
                                            onClick={() => setShowConfirm(true)}
                                            className="rounded-xl bg-red-600 px-4 py-3 text-sm font-medium text-white transition hover:bg-red-500 shrink-0"
                                        >
                                            Disconnect
                                        </button>
                                    </div>
                                </div>
                            )}
                        </>
                    ) : (
                        <div className="rounded-2xl border border-white/10 bg-gray-900/70 p-6 text-sm text-gray-400 shadow-lg shadow-black/20 backdrop-blur">
                            Only the group leader can manage JIRA integration settings.
                        </div>
                    )}
                </div>
            </main>

            <ConfirmDialog
                open={showConfirm}
                title="Disconnect JIRA integration?"
                message="This will remove the current JIRA integration binding from your group. Are you sure you want to continue?"
                confirmText="Yes, disconnect"
                cancelText="Cancel"
                loading={unbindLoading}
                onConfirm={handleUnbind}
                onCancel={() => setShowConfirm(false)}
            />
        </>
    );
}
