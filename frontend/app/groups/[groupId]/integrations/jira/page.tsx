"use client";

import { useState, useEffect } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { toast } from "sonner";
import ConfirmDialog from "@/components/ConfirmDialog";
import AppTopbar from "@/components/AppTopbar";
import apiClient from "@/lib/client";

export default function JiraIntegrationPage() {
    const params = useParams();
    const groupId = Number(params.groupId);

    const [integration, setIntegration] = useState<{ jiraSpaceUrl: string; projectKey: string; status: string } | null>(null);
    const [loading, setLoading] = useState(true);
    
    // Form states
    const [spaceUrl, setSpaceUrl] = useState("");
    const [projectKey, setProjectKey] = useState("");
    const [apiKey, setApiKey] = useState("");
    const [bindLoading, setBindLoading] = useState(false);

    // Unbind states
    const [showConfirm, setShowConfirm] = useState(false);
    const [unbindLoading, setUnbindLoading] = useState(false);

    useEffect(() => {
        const fetchIntegration = async () => {
            try {
                const res = await apiClient.get(`/groups/${groupId}/integrations/jira`);
                if (res.data && res.data.data) {
                    setIntegration(res.data.data);
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
        
        if (!spaceUrl.trim() || !projectKey.trim() || !apiKey.trim()) {
            toast.error("Space URL, Project Key, and API Key are required.");
            return;
        }

        setBindLoading(true);
        try {
            const res = await apiClient.post(`/groups/${groupId}/integrations/jira`, {
                jiraSpaceUrl: spaceUrl.trim(),
                projectKey: projectKey.trim(),
                apiKey: apiKey.trim()
            });
            
            setIntegration(res.data?.data || { jiraSpaceUrl: spaceUrl, projectKey, status: "ACTIVE" });
            setSpaceUrl("");
            setProjectKey("");
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
            <main className="min-h-screen bg-gray-950 px-6 py-10 text-white relative">
                <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top_left,_var(--tw-gradient-stops))] from-indigo-900/20 via-gray-950 to-gray-950 -z-10 pointer-events-none" />
                
                <div className="mx-auto max-w-4xl space-y-8 relative">
                    <AppTopbar title="JIRA Integration" />

                    <Link
                        href={`/groups/${groupId}`}
                        className="text-sm text-indigo-400 hover:text-indigo-300 hover:underline transition-colors"
                    >
                        ← Back to group
                    </Link>

                    <div>
                        <h1 className="text-3xl font-bold bg-gradient-to-r from-white to-indigo-300 bg-clip-text text-transparent">JIRA Integration</h1>
                        <p className="mt-2 text-gray-400 text-sm">
                            Connect your group with a JIRA space to sync issues and progress seamlessly.
                        </p>
                    </div>

                    {loading ? (
                        <div className="flex justify-center py-10">
                            <div className="inline-block w-8 h-8 border-4 border-white/20 border-t-indigo-500 rounded-full animate-spin"></div>
                        </div>
                    ) : (
                        <>
                            {integration ? (
                                <div className="rounded-2xl border border-emerald-500/30 bg-emerald-500/10 p-6 shadow-xl shadow-black/30 backdrop-blur-xl flex items-start gap-4 animate-in slide-in-from-top-2 duration-300">
                                    <div className="w-10 h-10 rounded-full bg-emerald-500/20 flex items-center justify-center shrink-0">
                                        <svg className="w-6 h-6 text-emerald-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                                        </svg>
                                    </div>
                                    <div>
                                        <h3 className="text-lg font-semibold text-white">JIRA Integration is Active</h3>
                                        <p className="mt-1 text-sm text-gray-400">
                                            Space URL: <span className="text-gray-300 font-mono bg-white/5 px-2 py-0.5 rounded">{integration.jiraSpaceUrl}</span>
                                        </p>
                                        <p className="mt-1 text-sm text-gray-400">
                                            Project Key: <span className="text-gray-300 font-mono bg-white/5 px-2 py-0.5 rounded">{integration.projectKey}</span>
                                        </p>
                                        <div className="mt-3 inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-emerald-500/10 border border-emerald-500/20 shadow-sm">
                                            <div className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
                                            <span className="text-xs font-medium text-emerald-400">ACTIVE</span>
                                        </div>
                                    </div>
                                </div>
                            ) : (
                                <div className="rounded-2xl border border-white/10 bg-gray-900/60 p-8 shadow-2xl hover:shadow-indigo-500/10 hover:bg-gray-800/80 backdrop-blur-xl transition-all duration-300">
                                    <form onSubmit={handleBind} className="space-y-5">
                                        <div>
                                            <label className="block text-sm font-medium text-gray-300 mb-1.5">
                                                JIRA Space URL <span className="text-red-500">*</span>
                                            </label>
                                            <input
                                                type="url"
                                                required
                                                placeholder="https://your-domain.atlassian.net"
                                                value={spaceUrl}
                                                onChange={(e) => setSpaceUrl(e.target.value)}
                                                className="w-full bg-black/30 border border-white/10 text-white rounded-xl px-4 py-3 focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500 outline-none transition-all"
                                            />
                                        </div>
                                        <div>
                                            <label className="block text-sm font-medium text-gray-300 mb-1.5">
                                                Project Key <span className="text-red-500">*</span>
                                            </label>
                                            <input
                                                type="text"
                                                required
                                                placeholder="e.g. SPMS"
                                                value={projectKey}
                                                onChange={(e) => setProjectKey(e.target.value)}
                                                className="w-full bg-black/30 border border-white/10 text-white rounded-xl px-4 py-3 focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500 outline-none transition-all"
                                            />
                                        </div>
                                        <div>
                                            <label className="block text-sm font-medium text-gray-300 mb-1.5">
                                                API Token <span className="text-red-500">*</span>
                                            </label>
                                            <input
                                                type="password"
                                                required
                                                placeholder="Paste your Atlassian API token here"
                                                value={apiKey}
                                                onChange={(e) => setApiKey(e.target.value)}
                                                className="w-full bg-black/30 border border-white/10 text-white rounded-xl px-4 py-3 focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500 outline-none transition-all"
                                            />
                                        </div>
                                        <div className="pt-4">
                                            <button
                                                type="submit"
                                                disabled={bindLoading || !spaceUrl || !projectKey || !apiKey}
                                                className="w-full bg-gradient-to-br from-indigo-500 to-purple-600 hover:from-indigo-600 hover:to-purple-700 text-white px-6 py-3.5 rounded-xl font-semibold shadow-lg shadow-indigo-500/30 transition-all active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                                            >
                                                {bindLoading ? (
                                                    <><div className="w-4 h-4 border-2 border-white/20 border-t-white rounded-full animate-spin" /> Connecting...</>
                                                ) : "Connect JIRA Space"}
                                            </button>
                                        </div>
                                    </form>
                                </div>
                            )}

                            {integration && (
                                <div className="rounded-2xl border border-red-500/10 bg-gray-900/60 p-6 shadow-xl shadow-black/20 backdrop-blur-xl transition-all duration-300 mt-6">
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
                                            className="rounded-xl bg-red-500/10 border border-red-500/20 px-6 py-3 text-sm font-semibold text-red-400 transition-all hover:bg-red-500/20 hover:border-red-500/40 active:scale-[0.98] shrink-0"
                                        >
                                            Disconnect Space
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
