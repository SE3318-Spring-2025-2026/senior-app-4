"use client";

import { useEffect, useState, useCallback } from "react";
import { fetchLeaderboard, StudentPerformanceDto, LeaderboardResponse } from "@/lib/analytics-api";
import { ArrowUp, ArrowDown, ChevronLeft, ChevronRight } from "lucide-react";

type SortField = "name" | "assignedSp" | "accomplishedSp" | "ratio";
type SortDir = "asc" | "desc";

export default function LeaderboardTable() {
    const [data, setData] = useState<StudentPerformanceDto[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(1);
    const [sortField, setSortField] = useState<SortField>("ratio");
    const [sortDir, setSortDir] = useState<SortDir>("desc");

    const loadData = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const res = await fetchLeaderboard(page, 10, sortField, sortDir);
            setData(res.content);
            setTotalPages(res.totalPages || 1);
        } catch (err) {
            console.error("Failed to load leaderboard:", err);
            setError("Failed to load performance data from server.");
            setData([]);
        } finally {
            setLoading(false);
        }
    }, [page, sortField, sortDir]);

    useEffect(() => {
        loadData();
    }, [loadData]);

    const handleSort = (field: SortField) => {
        if (sortField === field) {
            setSortDir(sortDir === "asc" ? "desc" : "asc");
        } else {
            setSortField(field);
            setSortDir("desc");
        }
    };

    const formatRatio = (ratio: number) => {
        return (ratio * 100).toFixed(1) + "%";
    };

    const getRatioStyle = (ratio: number) => {
        if (ratio >= 0.95) return "text-green-400 bg-green-400/10 border-green-400/20";
        if (ratio < 0.40) return "text-red-400 bg-red-400/10 border-red-400/20";
        return "text-gray-300 bg-white/5 border-white/10";
    };

    const SortIcon = ({ field }: { field: SortField }) => {
        if (sortField !== field) return null;
        return sortDir === "asc" ? <ArrowUp className="w-3 h-3 ml-1 inline" /> : <ArrowDown className="w-3 h-3 ml-1 inline" />;
    };

    return (
        <div className="bg-gray-900 border border-white/8 rounded-2xl overflow-hidden shadow-xl">
            <div className="px-6 py-4 border-b border-white/5 flex items-center justify-between">
                <div>
                    <h2 className="text-lg font-semibold text-white">Student Leaderboard</h2>
                    <p className="text-sm text-gray-400 mt-0.5">Performance ranking based on assigned and accomplished Story Points</p>
                </div>
                <button
                    onClick={loadData}
                    className="px-3 py-1.5 rounded-lg text-xs font-medium bg-white/5 border border-white/10 text-gray-300 hover:text-white hover:bg-white/10 transition-colors"
                >
                    Refresh
                </button>
            </div>

            <div className="overflow-x-auto">
                <table className="w-full min-w-[700px]">
                    <thead>
                        <tr className="border-b border-white/5">
                            <th 
                                className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:text-gray-300 select-none transition-colors"
                                onClick={() => handleSort("name")}
                            >
                                Student Name <SortIcon field="name" />
                            </th>
                            <th 
                                className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:text-gray-300 select-none transition-colors"
                                onClick={() => handleSort("assignedSp")}
                            >
                                Target SP <SortIcon field="assignedSp" />
                            </th>
                            <th 
                                className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:text-gray-300 select-none transition-colors"
                                onClick={() => handleSort("accomplishedSp")}
                            >
                                Accomplished SP <SortIcon field="accomplishedSp" />
                            </th>
                            <th 
                                className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:text-gray-300 select-none transition-colors"
                                onClick={() => handleSort("ratio")}
                            >
                                Ratio <SortIcon field="ratio" />
                            </th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-white/5">
                        {loading ? (
                            Array.from({ length: 5 }).map((_, idx) => (
                                <tr key={`skeleton-${idx}`} className="animate-pulse">
                                    <td className="px-6 py-4"><div className="h-4 bg-gray-800 rounded w-3/4"></div></td>
                                    <td className="px-6 py-4"><div className="h-4 bg-gray-800 rounded w-1/2"></div></td>
                                    <td className="px-6 py-4"><div className="h-4 bg-gray-800 rounded w-1/2"></div></td>
                                    <td className="px-6 py-4"><div className="h-6 bg-gray-800 rounded-full w-16"></div></td>
                                </tr>
                            ))
                        ) : error ? (
                            <tr>
                                <td colSpan={4} className="px-6 py-8 text-center text-sm text-red-400">
                                    {error}
                                </td>
                            </tr>
                        ) : data.length === 0 ? (
                            <tr>
                                <td colSpan={4} className="px-6 py-8 text-center text-sm text-gray-500">
                                    No performance data available.
                                </td>
                            </tr>
                        ) : (
                            data.map((student) => (
                                <tr key={student.studentId} className="hover:bg-white/[0.02] transition-colors">
                                    <td className="px-6 py-4 text-sm font-medium text-white">
                                        {student.name}
                                    </td>
                                    <td className="px-6 py-4 text-sm text-gray-400">
                                        {student.assignedSp} SP
                                    </td>
                                    <td className="px-6 py-4 text-sm text-gray-400">
                                        {student.accomplishedSp} SP
                                    </td>
                                    <td className="px-6 py-4 text-sm">
                                        <span className={`inline-flex items-center px-2.5 py-1 rounded-full border text-xs font-medium ${getRatioStyle(student.ratio)}`}>
                                            {formatRatio(student.ratio)}
                                        </span>
                                    </td>
                                </tr>
                            ))
                        )}
                    </tbody>
                </table>
            </div>

            {/* Pagination */}
            <div className="px-6 py-4 border-t border-white/5 flex items-center justify-between">
                <span className="text-sm text-gray-500">
                    Page {page + 1} of {Math.max(1, totalPages)}
                </span>
                <div className="flex gap-2">
                    <button
                        disabled={page === 0 || loading}
                        onClick={() => setPage(page - 1)}
                        className="p-1.5 rounded-lg border border-white/10 text-gray-400 hover:text-white hover:bg-white/5 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                    >
                        <ChevronLeft className="w-5 h-5" />
                    </button>
                    <button
                        disabled={page >= totalPages - 1 || loading}
                        onClick={() => setPage(page + 1)}
                        className="p-1.5 rounded-lg border border-white/10 text-gray-400 hover:text-white hover:bg-white/5 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                    >
                        <ChevronRight className="w-5 h-5" />
                    </button>
                </div>
            </div>
        </div>
    );
}
