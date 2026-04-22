"use client";

import { useState, useEffect } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { toast } from "sonner";
import ConfirmDialog from "@/components/ConfirmDialog";
import AppTopbar from "@/components/AppTopbar";
import { useNotifications } from "@/components/NotificationProvider";
import apiClient from "@/lib/client";

export default function JiraIntegrationPage() {
    const params = useParams();
    const groupId = Number(params.groupId);

    const { unreadOrPendingCount } = useNotifications();

    const [integration, setIntegration] = useState<{ spaceUrl: string; status: string } | null>(null);
    const [loading, setLoading] = useState(true);
    
    // Form states
    const [spaceUrl, setSpaceUrl] = useState("");
    const [apiKey, setApiKey] = useState("");
    const [bindLoading, setBindLoading] = useState(false);

    // Unbind states
    const [showConfirm, setShowConfirm] = useState(false);
    const [unbindLoading, setUnbindLoading] = useState(false);

    useEffect(() => {
        const fetchIntegration = async () => {
            try {
                const res = await apiClient.get(`/groups/${groupId}/integrations/jira`);
                if (res.data) {
                    setIntegration(res.data);
                }
            } catch (err: any) {
                // If 404, it means no integration exists, which is fine
                if (err.response?.status !== 404) {
                    console.error("Failed to load Jira integration", err);
                }
            } finally {
                setLoading(false);
            }
        };

        fetchIntegration();
    }, [groupId]);

    async function handleBind(e: React.FormEvent) {
        e.preventDefault();
        
        if (!spaceUrl.trim() || !apiKey.trim()) {
            toast.error("Space URL and API Key are required.");
            return;
        }

        setBindLoading(true);
        try {
            const res = await apiClient.post(`/groups/${groupId}/integrations/jira`, {
                spaceUrl: spaceUrl.trim(),
                apiKey: apiKey.trim()
            });
            
            setIntegration(res.data || { spaceUrl, status: "ACTIVE" });
            setSpaceUrl("");
            setApiKey("");
            toast.success("JIRA space successfully connected.");
        } catch (error: any) {
            console.error(error);
        } finally {
            setBindLoading(false);
        }
    }

    async function handleUnbind() {
        setUnbindLoading(true);
        try {
            await apiClient.delete(`/groups/${groupId}/integrations/jira`);
            setIntegration(null);
            toast.success("JIRA integration unbound successfully.");
            setShowConfirm(false);
        } catch (error) {
            console.error(error);
        } finally {
            setUnbindLoading(false);
        }
    }

    return (
        <>
            <main className="min-h-screen bg-gray-950 px-6 py-10 text-white">
                <div className="mx-auto max-w-4xl space-y-8">
                    <AppTopbar
                        title="JIRA Integration"
                        notificationCount={unreadOrPendingCount}
                    />

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

                    {loading ? (
                        <div className="flex justify-center py-10">
                            <div className="inline-block w-8 h-8 border-4 border-white/20 border-t-blue-500 rounded-full animate-spin"></div>
                        </div>
                    ) : (
                        <>
                            {integration ? (
                                <div className="rounded-2xl border border-green-500/20 bg-green-500/5 p-6 shadow-lg shadow-black/20 backdrop-blur flex items-start gap-4">
                                    <div className="w-10 h-10 rounded-full bg-green-500/20 flex items-center justify-center shrink-0">
                                        <svg className="w-6 h-6 text-green-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                                        </svg>
                                    </div>
                                    <div>
                                        <h3 className="text-lg font-semibold text-white">JIRA Space Connected</h3>
                                        <p className="mt-1 text-sm text-gray-400">
                                            Currently syncing with: <span className="text-gray-300 font-mono bg-white/5 px-2 py-0.5 rounded">{integration.spaceUrl}</span>
                                        </p>
                                        <div className="mt-3 inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-green-500/10 border border-green-500/20">
                                            <div className="w-2 h-2 rounded-full bg-green-400 animate-pulse" />
                                            <span className="text-xs font-medium text-green-400">ACTIVE</span>
                                        </div>
                                    </div>
                                </div>
                            ) : (
                                <div className="rounded-2xl border border-white/10 bg-gray-900/70 p-6 shadow-lg shadow-black/20 backdrop-blur">
                                    <h3 className="text-lg font-semibold text-white mb-4">Connect JIRA Account</h3>
                                    <form onSubmit={handleBind} className="space-y-4">
                                        <div>
                                            <label className="block text-sm font-medium text-gray-300 mb-1.5">JIRA Space URL</label>
                                            <input
                                                type="url"
                                                required
                                                placeholder="https://your-domain.atlassian.net"
                                                value={spaceUrl}
                                                onChange={(e) => setSpaceUrl(e.target.value)}
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

                            {integration && (
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
