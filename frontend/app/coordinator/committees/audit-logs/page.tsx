"use client";

import Sidebar from "@/components/Sidebar";
import { toast } from "sonner";
import { fetchCommitteeAuditLogs } from "@/lib/committees-api";
import { CommitteeAuditLog } from "@/lib/committee-types";
import { useEffect, useMemo, useState } from "react";

export default function CommitteeAuditLogsPage() {
    const [logs, setLogs] = useState<CommitteeAuditLog[]>([]);
    const [loading, setLoading] = useState(true);

    const [actionFilter, setActionFilter] = useState("ALL");
    const [entityFilter, setEntityFilter] = useState("ALL");
    const [fromDate, setFromDate] = useState("");
    const [toDate, setToDate] = useState("");

    const [page, setPage] = useState(1);
    const [totalPages, setTotalPages] = useState(1);
    const pageSize = 20;

    async function loadLogs(nextPage = page) {
        setLoading(true);

        try {
            const response = await fetchCommitteeAuditLogs(
                nextPage - 1,
                pageSize,
                actionFilter,
                entityFilter,
                fromDate,
                toDate
            );

            setLogs(response.content);
            setTotalPages(response.totalPages);
        } catch (err) {
            toast.error(
                err instanceof Error ? err.message : "Failed to load audit logs."
            );
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        setPage(1);
        loadLogs(1);
    }, [actionFilter, entityFilter, fromDate, toDate]);

    const csvContent = useMemo(() => {
        const header = ["timestamp", "user", "action", "entityType", "description"];

        if (!logs || !Array.isArray(logs) || logs.length === 0) {
            return header.join(",");
        }

        const rows = logs.map((log) => [
            new Date(log.timestamp).toLocaleString(),
            log.userName,
            log.action,
            log.entityType,
            log.description.replaceAll(",", " "),
        ]);

        return [header, ...rows].map((row) => row.join(",")).join("\n");
    }, [logs]);

    function exportCsv() {
        const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
        const url = URL.createObjectURL(blob);

        const link = document.createElement("a");
        link.href = url;
        link.download = "committee-audit-logs.csv";
        link.click();

        URL.revokeObjectURL(url);
    }

    function handlePageChange(nextPage: number) {
        if (nextPage < 1 || nextPage > totalPages) return;

        setPage(nextPage);
        loadLogs(nextPage);
    }

    return (
        <div className="min-h-screen bg-gray-950 flex">
            <Sidebar activePage="committee-audit-logs" />

            <main className="flex-1 min-w-0 px-6 py-10 text-white">
                <div className="mx-auto max-w-6xl space-y-8">
                    <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
                        <div>
                            <h1 className="text-3xl font-bold">Committee Audit Logs</h1>
                            <p className="mt-2 text-gray-400">
                                Track committee-related platform operations and history.
                            </p>
                        </div>

                        <button
                            onClick={exportCsv}
                            disabled={logs.length === 0}
                            className="rounded-xl bg-blue-600 px-5 py-3 text-sm font-semibold text-white hover:bg-blue-500 disabled:opacity-50"
                        >
                            Export CSV
                        </button>
                    </div>

                    <div className="rounded-2xl border border-white/10 bg-gray-900/70 p-6">
                        <div className="mb-6 grid grid-cols-1 gap-4 md:grid-cols-4">
                            <select
                                value={actionFilter}
                                onChange={(e) => setActionFilter(e.target.value)}
                                className="rounded-xl border border-white/10 bg-gray-950 px-4 py-3 text-sm text-white outline-none focus:border-blue-500"
                            >
                                <option value="ALL">All Actions</option>
                                <option value="COMMITTEE_CREATED">COMMITTEE_CREATED</option>
                                <option value="COMMITTEE_UPDATED">COMMITTEE_UPDATED</option>
                                <option value="JURY_ASSIGNED">JURY_ASSIGNED</option>
                                <option value="GRADE_SUBMITTED">GRADE_SUBMITTED</option>
                            </select>

                            <select
                                value={entityFilter}
                                onChange={(e) => setEntityFilter(e.target.value)}
                                className="rounded-xl border border-white/10 bg-gray-950 px-4 py-3 text-sm text-white outline-none focus:border-blue-500"
                            >
                                <option value="ALL">All Entity Types</option>
                                <option value="COMMITTEE">COMMITTEE</option>
                                <option value="COMMITTEE_ASSIGNMENT">COMMITTEE_ASSIGNMENT</option>
                                <option value="SUBMISSION">SUBMISSION</option>
                            </select>

                            <div>
                                <label className="mb-1 block pl-3 text-xs font-medium text-gray-400">
                                    From Date
                                </label>
                                <input
                                    type="date"
                                    value={fromDate}
                                    onChange={(e) => setFromDate(e.target.value)}
                                    className="w-full rounded-xl border border-white/10 bg-gray-950 px-4 py-3 text-sm text-white outline-none focus:border-blue-500"
                                />
                            </div>

                            <div>
                                <label className="mb-1 block pl-3 text-xs font-medium text-gray-400">
                                    To Date
                                </label>
                                <input
                                    type="date"
                                    value={toDate}
                                    onChange={(e) => setToDate(e.target.value)}
                                    className="w-full rounded-xl border border-white/10 bg-gray-950 px-4 py-3 text-sm text-white outline-none focus:border-blue-500"
                                />
                            </div>
                        </div>

                        {loading ? (
                            <div className="space-y-3">
                                {Array.from({ length: 5 }).map((_, index) => (
                                    <div
                                        key={index}
                                        className="h-14 animate-pulse rounded-xl border border-white/10 bg-white/5"
                                    />
                                ))}
                            </div>
                        ) : logs.length === 0 ? (
                            <div className="rounded-2xl border border-dashed border-white/10 bg-white/5 px-6 py-16 text-center">
                                <p className="text-gray-400">No audit logs found.</p>
                            </div>
                        ) : (
                            <>
                                <div className="overflow-x-auto">
                                    <table className="w-full text-left text-sm">
                                        <thead>
                                            <tr className="border-b border-white/10 text-xs uppercase tracking-wider text-gray-500">
                                                <th className="px-4 py-3">Timestamp</th>
                                                <th className="px-4 py-3">User</th>
                                                <th className="px-4 py-3">Action</th>
                                                <th className="px-4 py-3">Entity Type</th>
                                                <th className="px-4 py-3">Description</th>
                                            </tr>
                                        </thead>

                                        <tbody className="divide-y divide-white/5">
                                            {logs.map((log) => (
                                                <tr key={log.id} className="hover:bg-white/5">
                                                    <td className="px-4 py-4 text-gray-300">
                                                        {new Date(log.timestamp).toLocaleString()}
                                                    </td>
                                                    <td className="px-4 py-4 text-white">
                                                        {log.userName}
                                                    </td>
                                                    <td className="px-4 py-4 text-blue-300">
                                                        {log.action}
                                                    </td>
                                                    <td className="px-4 py-4 text-gray-300">
                                                        {log.entityType}
                                                    </td>
                                                    <td className="px-4 py-4 text-gray-400">
                                                        {log.description}
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>

                                <div className="mt-8 flex items-center justify-center gap-3">
                                    <button
                                        onClick={() => handlePageChange(page - 1)}
                                        disabled={page <= 1}
                                        className="rounded-xl border border-white/10 bg-gray-900 px-4 py-2 text-sm text-gray-300 disabled:cursor-not-allowed disabled:opacity-40 hover:bg-white/5"
                                    >
                                        Previous
                                    </button>

                                    <span className="text-sm text-gray-400">
                                        Page <span className="text-white">{page}</span> /{" "}
                                        <span className="text-white">{totalPages}</span>
                                    </span>

                                    <button
                                        onClick={() => handlePageChange(page + 1)}
                                        disabled={page >= totalPages}
                                        className="rounded-xl border border-white/10 bg-gray-900 px-4 py-2 text-sm text-gray-300 disabled:cursor-not-allowed disabled:opacity-40 hover:bg-white/5"
                                    >
                                        Next
                                    </button>
                                </div>
                            </>
                        )}
                    </div>
                </div>
            </main>
        </div>
    );
}