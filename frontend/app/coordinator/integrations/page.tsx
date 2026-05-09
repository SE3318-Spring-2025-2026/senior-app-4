"use client";

import { useEffect, useState } from "react";
import { toast } from "sonner";
import { format } from "date-fns";
import Sidebar from "@/components/Sidebar";
import { RefreshCw } from "lucide-react";
import { triggerScrumSync, fetchSyncRecords } from "@/lib/integrations-api";

interface SyncRecord {
    id: number;
    issueKey: string;
    assigneeGithubUsername: string | null;
    syncedAt: string;
    storyPoints: number | null;
}

export default function CoordinatorIntegrationsPage() {
    const [records, setRecords] = useState<SyncRecord[]>([]);
    const [loading, setLoading] = useState(true);
    const [syncing, setSyncing] = useState(false);

    useEffect(() => {
        loadRecords();
    }, []);

    async function loadRecords() {
        setLoading(true);
        try {
            const data = await fetchSyncRecords();
            
            // Extract records safely whether it's an array directly or wrapped in an object
            let recordsData: SyncRecord[] = [];
            if (Array.isArray(data)) {
                recordsData = data;
            } else if (data && typeof data === 'object') {
                recordsData = Array.isArray(data.content) ? data.content : (Array.isArray(data.data) ? data.data : []);
            }

            // Sort by syncedAt descending
            const sorted = recordsData.sort((a: SyncRecord, b: SyncRecord) => 
                new Date(b.syncedAt).getTime() - new Date(a.syncedAt).getTime()
            );
            setRecords(sorted);
        } catch (error: any) {
            toast.error(error.message || "Failed to load sync records.");
        } finally {
            setLoading(false);
        }
    }

    async function handleSyncAll() {
        setSyncing(true);
        try {
            await triggerScrumSync();
            toast.success("Global synchronization started successfully!");
            // Polling or manual refresh is expected
        } catch (error: any) {
            toast.error(error.message || "Failed to start global sync.");
        } finally {
            setSyncing(false);
        }
    }

    return (
        <div className="flex min-h-screen bg-gray-950">
            <Sidebar activePage="integrations" />
            <main className="flex-1 flex flex-col min-w-0">
                <div className="border-b border-white/5 px-8 py-4 flex items-center justify-between">
                    <div>
                        <h1 className="text-base font-semibold text-white">Integrations</h1>
                        <p className="text-xs text-gray-500 mt-0.5">Manage Jira & GitHub synchronization</p>
                    </div>
                    <div className="flex items-center gap-3">
                        <button
                            onClick={handleSyncAll}
                            disabled={syncing}
                            className="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-500 transition-colors disabled:opacity-50 flex items-center gap-2"
                        >
                            <RefreshCw className={`w-4 h-4 ${syncing ? "animate-spin" : ""}`} />
                            Sync All Now
                        </button>
                    </div>
                </div>

                <div className="flex-1 p-8">
                    <div className="max-w-6xl mx-auto">
                        <div className="bg-gray-900 border border-white/10 rounded-2xl overflow-hidden shadow-xl shadow-black/20">
                            <div className="px-6 py-4 border-b border-white/5 flex justify-between items-center bg-gray-900/50">
                                <h2 className="text-lg font-semibold text-white">Synchronized Issues</h2>
                                <button
                                    onClick={loadRecords}
                                    disabled={loading}
                                    className="text-gray-400 hover:text-white transition-colors p-2"
                                    title="Refresh Records"
                                >
                                    <RefreshCw className={`w-4 h-4 ${loading ? "animate-spin" : ""}`} />
                                </button>
                            </div>

                            <div className="overflow-x-auto">
                                <table className="w-full text-left border-collapse">
                                    <thead>
                                        <tr className="border-b border-white/5 text-gray-400 text-sm">
                                            <th className="p-4 font-medium">Issue Key</th>
                                            <th className="p-4 font-medium">Assignee (GitHub)</th>
                                            <th className="p-4 font-medium">Story Points</th>
                                            <th className="p-4 font-medium">Synced At</th>
                                        </tr>
                                    </thead>
                                    <tbody className="divide-y divide-white/5 text-sm">
                                        {loading ? (
                                            <tr>
                                                <td colSpan={4} className="p-8 text-center text-gray-500">
                                                    Loading records...
                                                </td>
                                            </tr>
                                        ) : records.length === 0 ? (
                                            <tr>
                                                <td colSpan={4} className="p-8 text-center text-gray-500">
                                                    No synchronization records found.
                                                </td>
                                            </tr>
                                        ) : (
                                            records.map((r) => (
                                                <tr key={r.id} className="hover:bg-white/5 transition-colors">
                                                    <td className="p-4 text-white font-medium">{r.issueKey}</td>
                                                    <td className="p-4 text-gray-300">{r.assigneeGithubUsername || <span className="text-gray-600 italic">Unassigned</span>}</td>
                                                    <td className="p-4 text-gray-300">{r.storyPoints !== null ? r.storyPoints : <span className="text-gray-600 italic">-</span>}</td>
                                                    <td className="p-4 text-gray-400">
                                                        {format(new Date(r.syncedAt), "MMM d, yyyy HH:mm")}
                                                    </td>
                                                </tr>
                                            ))
                                        )}
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                </div>
            </main>
        </div>
    );
}
