"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { getToken, getUser } from "@/lib/auth";
import Sidebar from "@/components/Sidebar";
import { fetchAllGroupGrades, type GroupGradeSummary } from "@/lib/final-grading-api";

const PAGE_SIZE = 20;

function Spinner() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-950">
      <div className="h-6 w-6 animate-spin rounded-full border-b-2 border-blue-500" />
    </div>
  );
}

export default function CoordinatorGradesPage() {
  const router = useRouter();
  const [ready, setReady] = useState(false);

  useEffect(() => {
    const token = getToken();
    const user = getUser();
    if (!token || !user) { router.replace("/auth/login"); return; }
    if (user.requiresPasswordChange) { router.replace("/auth/change-password"); return; }
    if (user.role !== "coordinator") { router.replace("/dashboard"); return; }
    setReady(true);
  }, [router]);

  if (!ready) return <Spinner />;
  return <GradesContent />;
}

function GradesContent() {
  const [grades, setGrades] = useState<GroupGradeSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const data = await fetchAllGroupGrades();
      setGrades(data);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  // Filter by search
  const filtered = grades.filter(g =>
    g.groupName.toLowerCase().includes(search.toLowerCase()) ||
    String(g.groupId).includes(search)
  );

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const currentPage = Math.min(page, totalPages - 1);
  const paged = filtered.slice(currentPage * PAGE_SIZE, (currentPage + 1) * PAGE_SIZE);

  const published = grades.filter(g => g.published).length;
  const draft = grades.filter(g => !g.published).length;

  return (
    <div className="flex min-h-screen bg-gray-950">
      <Sidebar activePage="coordinator-grades" />
      <main className="flex-1 overflow-y-auto p-8">
        <div className="mb-6">
          <h1 className="text-lg font-semibold text-white">Group Grades</h1>
          <p className="mt-0.5 text-xs text-gray-500">Final project grades for all groups</p>
        </div>

        {loading ? (
          <div className="flex h-48 items-center justify-center">
            <div className="h-6 w-6 animate-spin rounded-full border-b-2 border-blue-500" />
          </div>
        ) : (
          <div className="space-y-5 max-w-5xl">

            {/* Summary stats */}
            <div className="grid grid-cols-3 gap-4">
              <div className="rounded-xl border border-white/8 bg-gray-900 px-5 py-4">
                <p className="text-xs text-gray-500 uppercase tracking-wider">Total</p>
                <p className="mt-1 text-2xl font-bold text-white">{grades.length}</p>
              </div>
              <div className="rounded-xl border border-green-500/20 bg-green-500/5 px-5 py-4">
                <p className="text-xs text-green-500 uppercase tracking-wider">Published</p>
                <p className="mt-1 text-2xl font-bold text-white">{published}</p>
              </div>
              <div className="rounded-xl border border-amber-500/20 bg-amber-500/5 px-5 py-4">
                <p className="text-xs text-amber-500 uppercase tracking-wider">Draft</p>
                <p className="mt-1 text-2xl font-bold text-white">{draft}</p>
              </div>
            </div>

            {/* Search */}
            <div className="relative">
              <svg className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" />
              </svg>
              <input
                type="text"
                placeholder="Search group name or ID…"
                value={search}
                onChange={e => { setSearch(e.target.value); setPage(0); }}
                className="w-full rounded-xl border border-white/8 bg-gray-900 pl-9 pr-4 py-2.5 text-sm text-white placeholder-gray-600 focus:outline-none focus:border-blue-500/50"
              />
            </div>

            {/* Table */}
            {grades.length === 0 ? (
              <div className="rounded-2xl border border-dashed border-white/10 bg-gray-900 p-16 text-center">
                <p className="text-sm font-medium text-white">No grades computed yet</p>
                <p className="mt-1 text-xs text-gray-500">Use Final Grading to calculate group grades.</p>
              </div>
            ) : filtered.length === 0 ? (
              <div className="rounded-2xl border border-dashed border-white/10 bg-gray-900 p-12 text-center">
                <p className="text-sm text-gray-400">No groups match "{search}"</p>
              </div>
            ) : (
              <>
                <div className="rounded-2xl border border-white/8 bg-gray-900 overflow-hidden">
                  <table className="w-full">
                    <thead>
                      <tr className="border-b border-white/5">
                        <th className="px-5 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">Group</th>
                        <th className="px-5 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-500">Team Grade</th>
                        <th className="px-5 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-500">Members</th>
                        <th className="px-5 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-500">Status</th>
                        <th className="px-5 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-500"></th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-white/5">
                      {paged.map(row => (
                        <tr key={row.groupId} className="hover:bg-white/[0.02] transition-colors">
                          <td className="px-5 py-3">
                            <div className="flex items-center gap-3">
                              <div className="h-8 w-8 rounded-full bg-blue-500/20 flex items-center justify-center shrink-0">
                                <span className="text-xs font-bold text-blue-300">
                                  {row.groupName.charAt(0).toUpperCase()}
                                </span>
                              </div>
                              <div>
                                <p className="text-sm font-medium text-white">{row.groupName}</p>
                                <p className="text-xs text-gray-500">ID: {row.groupId}</p>
                              </div>
                            </div>
                          </td>
                          <td className="px-5 py-3 text-right">
                            <span className="text-lg font-bold text-white">
                              {row.teamGrade != null ? Number(row.teamGrade).toFixed(2) : "—"}
                            </span>
                            <span className="text-xs text-gray-500 ml-1">/ 100</span>
                          </td>
                          <td className="px-5 py-3 text-right text-sm text-gray-400">
                            {row.studentCount}
                          </td>
                          <td className="px-5 py-3 text-right">
                            {row.published ? (
                              <span className="inline-flex items-center gap-1 rounded-full bg-green-500/15 px-2.5 py-0.5 text-xs font-medium text-green-400">
                                <span className="h-1.5 w-1.5 rounded-full bg-green-400" />
                                Published
                              </span>
                            ) : (
                              <span className="inline-flex items-center gap-1 rounded-full bg-amber-500/15 px-2.5 py-0.5 text-xs font-medium text-amber-400">
                                <span className="h-1.5 w-1.5 rounded-full bg-amber-400" />
                                Draft
                              </span>
                            )}
                          </td>
                          <td className="px-5 py-3 text-right">
                            <Link
                              href={`/coordinator/grades/${row.groupId}`}
                              className="text-xs font-medium text-blue-400 hover:text-blue-300 transition-colors"
                            >
                              View Details →
                            </Link>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>

                {/* Pagination */}
                {totalPages > 1 && (
                  <div className="flex items-center justify-between">
                    <p className="text-xs text-gray-500">
                      Showing {currentPage * PAGE_SIZE + 1}–{Math.min((currentPage + 1) * PAGE_SIZE, filtered.length)} of {filtered.length}
                    </p>
                    <div className="flex items-center gap-1">
                      <button
                        onClick={() => setPage(p => Math.max(0, p - 1))}
                        disabled={currentPage === 0}
                        className="px-3 py-1.5 rounded-lg text-xs text-gray-400 hover:text-white hover:bg-white/5 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                      >
                        ← Prev
                      </button>
                      {Array.from({ length: totalPages }, (_, i) => (
                        <button
                          key={i}
                          onClick={() => setPage(i)}
                          className={`w-7 h-7 rounded-lg text-xs font-medium transition-colors ${
                            i === currentPage
                              ? "bg-blue-600 text-white"
                              : "text-gray-400 hover:text-white hover:bg-white/5"
                          }`}
                        >
                          {i + 1}
                        </button>
                      ))}
                      <button
                        onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                        disabled={currentPage === totalPages - 1}
                        className="px-3 py-1.5 rounded-lg text-xs text-gray-400 hover:text-white hover:bg-white/5 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                      >
                        Next →
                      </button>
                    </div>
                  </div>
                )}
              </>
            )}
          </div>
        )}
      </main>
    </div>
  );
}
