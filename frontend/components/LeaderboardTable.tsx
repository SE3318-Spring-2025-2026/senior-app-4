"use client";

import React, { useCallback, useEffect, useState } from "react";
import { ChevronUp, ChevronDown, RefreshCw, AlertTriangle, ChevronLeft, ChevronRight } from "lucide-react";
import { getLeaderboard, LeaderboardStudent } from "@/lib/analytics-api";

type SortColumn = "name" | "targetSp" | "storyPoints" | "ratio";
type SortDirection = "asc" | "desc";

export default function LeaderboardTable() {
  const [students, setStudents] = useState<LeaderboardStudent[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const [sortColumn, setSortColumn] = useState<SortColumn | null>(null);
  const [sortDirection, setSortDirection] = useState<SortDirection>("asc");

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchLeaderboard = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      let sortParam = "";
      if (sortColumn) {
        sortParam = `${sortColumn},${sortDirection}`;
      }

      const response = await getLeaderboard({
        page,
        size: 10,
        sort: sortParam || undefined,
      });

      setStudents(response.content || []);
      setTotalPages(response.totalPages);
      setTotalElements(response.totalElements);
    } catch (err: unknown) {
      setError("Failed to load leaderboard data.");
      setStudents([]);
    } finally {
      setLoading(false);
    }
  }, [page, sortColumn, sortDirection]);

  useEffect(() => {
    fetchLeaderboard();
  }, [fetchLeaderboard]);

  const computeTargetSp = (accomplished: number, ratio: number) => {
    if (ratio === 0) return 0;
    return Math.round(accomplished / ratio);
  };

  const sortedStudents = React.useMemo(() => {
    if (!sortColumn) return students;
    return [...students].sort((a, b) => {
      let valA, valB;
      if (sortColumn === "targetSp") {
        valA = computeTargetSp(a.storyPoints, a.ratio);
        valB = computeTargetSp(b.storyPoints, b.ratio);
      } else {
        valA = a[sortColumn];
        valB = b[sortColumn];
      }

      if (valA < valB) return sortDirection === "asc" ? -1 : 1;
      if (valA > valB) return sortDirection === "asc" ? 1 : -1;
      return 0;
    });
  }, [students, sortColumn, sortDirection]);

  const handleSort = (column: SortColumn) => {
    if (sortColumn === column) {
      setSortDirection((prev) => (prev === "asc" ? "desc" : "asc"));
    } else {
      setSortColumn(column);
      setSortDirection("asc");
    }
  };

  const getRatioColor = (ratio: number) => {
    if (ratio >= 0.9) return "text-emerald-400";
    if (ratio < 0.4) return "text-red-400";
    return "text-gray-300";
  };

  const renderSortIcon = (column: SortColumn) => {
    if (sortColumn !== column) return null;
    return sortDirection === "asc" ? (
      <ChevronUp className="ml-1 h-3.5 w-3.5 inline" />
    ) : (
      <ChevronDown className="ml-1 h-3.5 w-3.5 inline" />
    );
  };

  const canPrev = page > 0 && !loading;
  const canNext = page + 1 < totalPages && !loading;

  return (
    <div className="rounded-2xl border border-white/5 bg-white/[0.02] backdrop-blur-sm">
      <div className="flex items-center justify-between border-b border-white/5 px-6 py-4">
        <div>
          <h2 className="text-lg font-semibold text-white">Student Performance Leaderboard</h2>
          <p className="mt-1 text-xs text-gray-500">
            Real-time analytics on student performance and story points
          </p>
        </div>
        <button
          onClick={fetchLeaderboard}
          disabled={loading}
          className="flex items-center gap-1.5 rounded-lg border border-white/10 bg-white/5 px-3 py-1.5 text-xs text-gray-300 hover:bg-white/10 disabled:opacity-50 transition-colors"
        >
          <RefreshCw className={`h-3.5 w-3.5 ${loading ? "animate-spin" : ""}`} />
          Refresh
        </button>
      </div>

      <div className="overflow-x-auto">
        <table className="min-w-full" data-testid="student-performance-leaderboard">
          <thead>
            <tr className="border-b border-white/5 bg-white/[0.02] select-none">
              <th
                className="px-6 py-3 text-left text-[11px] font-medium uppercase tracking-widest text-gray-500 cursor-pointer hover:text-gray-300 transition-colors"
                onClick={() => handleSort("name")}
                data-testid="sort-name"
              >
                Student Name {renderSortIcon("name")}
              </th>
              <th
                className="px-6 py-3 text-left text-[11px] font-medium uppercase tracking-widest text-gray-500 cursor-pointer hover:text-gray-300 transition-colors"
                onClick={() => handleSort("targetSp")}
                data-testid="sort-target-sp"
              >
                Target / Assigned SP {renderSortIcon("targetSp")}
              </th>
              <th
                className="px-6 py-3 text-left text-[11px] font-medium uppercase tracking-widest text-gray-500 cursor-pointer hover:text-gray-300 transition-colors"
                onClick={() => handleSort("storyPoints")}
                data-testid="sort-story-points"
              >
                Accomplished SP {renderSortIcon("storyPoints")}
              </th>
              <th
                className="px-6 py-3 text-left text-[11px] font-medium uppercase tracking-widest text-gray-500 cursor-pointer hover:text-gray-300 transition-colors"
                onClick={() => handleSort("ratio")}
                data-testid="sort-ratio"
              >
                Success Ratio {renderSortIcon("ratio")}
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-white/5">
            {loading && students.length === 0 ? (
              <tr>
                <td colSpan={4} className="px-6 py-12 text-center">
                  <RefreshCw className="mx-auto h-6 w-6 animate-spin text-blue-400" />
                  <p className="mt-2 text-xs text-gray-500">Loading leaderboard...</p>
                </td>
              </tr>
            ) : error ? (
              <tr>
                <td colSpan={4} className="px-6 py-12 text-center">
                  <AlertTriangle className="mx-auto h-6 w-6 text-red-400" />
                  <p className="mt-2 text-xs text-red-300">{error}</p>
                </td>
              </tr>
            ) : sortedStudents.length === 0 ? (
              <tr>
                <td colSpan={4} className="px-6 py-12 text-center text-xs text-gray-500">
                  No students found in the leaderboard.
                </td>
              </tr>
            ) : (
              sortedStudents.map((student) => {
                const targetSp = computeTargetSp(student.storyPoints, student.ratio);
                const ratioPercent = Math.round(student.ratio * 100);

                return (
                  <tr key={student.studentId} className="hover:bg-white/[0.03] transition-colors" data-testid="leaderboard-row">
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-white">
                      {student.name}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-400">
                      {targetSp} SP
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-300">
                      {student.storyPoints} SP
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium" data-testid="leaderboard-ratio">
                      <span className={getRatioColor(student.ratio)}>
                        {ratioPercent}%
                      </span>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {!error && totalPages > 0 && (
        <div className="flex items-center justify-between border-t border-white/5 px-6 py-3">
          <p className="text-xs text-gray-500">
            Page <span className="text-gray-300">{page + 1}</span> of{" "}
            <span className="text-gray-300">{totalPages}</span>
            {totalElements > 0 && (
              <>
                {" · "}
                <span className="text-gray-300">{totalElements}</span> total
              </>
            )}
          </p>
          <div className="flex items-center gap-1">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={!canPrev}
              aria-label="Previous page"
              className="inline-flex h-8 w-8 items-center justify-center rounded-lg border border-white/10 bg-white/5 text-gray-300 hover:bg-white/10 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
            >
              <ChevronLeft className="h-4 w-4" />
            </button>

            {Array.from({ length: totalPages }).map((_, i) => (
              <button
                key={i}
                onClick={() => setPage(i)}
                disabled={loading}
                className={`inline-flex h-8 w-8 items-center justify-center rounded-lg border ${page === i
                  ? "border-blue-500/50 bg-blue-500/10 text-blue-400"
                  : "border-white/10 bg-white/5 text-gray-400 hover:bg-white/10"
                  } text-xs transition-colors`}
              >
                {i + 1}
              </button>
            ))}

            <button
              onClick={() => setPage((p) => p + 1)}
              disabled={!canNext}
              aria-label="Next page"
              className="inline-flex h-8 w-8 items-center justify-center rounded-lg border border-white/10 bg-white/5 text-gray-300 hover:bg-white/10 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
            >
              <ChevronRight className="h-4 w-4" />
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
