"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter, useParams } from "next/navigation";
import Link from "next/link";
import { getToken, getUser } from "@/lib/auth";
import Sidebar from "@/components/Sidebar";
import { fetchFinalGrades, type FinalGradeResponse } from "@/lib/final-grading-api";

type GradeData = FinalGradeResponse["data"];

function Spinner() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-950">
      <div className="h-6 w-6 animate-spin rounded-full border-b-2 border-blue-500" />
    </div>
  );
}

const DELIVERABLE_COLORS: Record<string, { border: string; bg: string; text: string; dot: string }> = {
  PROPOSAL:          { border: "border-blue-500/25",   bg: "bg-blue-500/10",   text: "text-blue-300",   dot: "bg-blue-400" },
  REVISED_PROPOSAL:  { border: "border-cyan-500/25",   bg: "bg-cyan-500/10",   text: "text-cyan-300",   dot: "bg-cyan-400" },
  STATEMENT_OF_WORK: { border: "border-purple-500/25", bg: "bg-purple-500/10", text: "text-purple-300", dot: "bg-purple-400" },
  DEMONSTRATION:     { border: "border-amber-500/25",  bg: "bg-amber-500/10",  text: "text-amber-300",  dot: "bg-amber-400" },
};

const DELIVERABLE_LABELS: Record<string, string> = {
  PROPOSAL: "Proposal",
  REVISED_PROPOSAL: "Revised Proposal",
  STATEMENT_OF_WORK: "Statement of Work",
  DEMONSTRATION: "Demonstration",
};

export default function CoordinatorGroupGradePage() {
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
  return <GroupGradeContent />;
}

function GroupGradeContent() {
  const params = useParams();
  const groupId = Number(params.groupId);

  const [data, setData] = useState<GradeData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await fetchFinalGrades(groupId);
      setData(res.data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not load grades.");
    } finally {
      setLoading(false);
    }
  }, [groupId]);

  useEffect(() => { load(); }, [load]);

  return (
    <div className="flex min-h-screen bg-gray-950">
      <Sidebar activePage="coordinator-grades" />
      <main className="flex-1 overflow-y-auto p-8">
        <div className="mb-6 flex items-center gap-4">
          <Link
            href="/coordinator/grades"
            className="flex items-center gap-1.5 text-xs text-gray-500 hover:text-gray-300 transition-colors"
          >
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M15.75 19.5L8.25 12l7.5-7.5" />
            </svg>
            All Groups
          </Link>
          <span className="text-gray-700">/</span>
          <div>
            <h1 className="text-lg font-semibold text-white">
              {data?.groupName ?? `Group ${groupId}`}
            </h1>
            <p className="mt-0.5 text-xs text-gray-500">Final grade breakdown</p>
          </div>
        </div>

        {loading ? (
          <div className="flex h-48 items-center justify-center">
            <div className="h-6 w-6 animate-spin rounded-full border-b-2 border-blue-500" />
          </div>
        ) : error ? (
          <div className="rounded-2xl border border-dashed border-white/10 bg-gray-900 p-16 text-center">
            <p className="text-sm font-medium text-white">Could not load grades</p>
            <p className="mt-1 text-xs text-gray-500">{error}</p>
          </div>
        ) : !data ? null : (
          <div className="space-y-6 max-w-5xl">

            {/* Team grade + status */}
            <div className="grid gap-4 sm:grid-cols-3">
              <div className="sm:col-span-2 rounded-2xl border border-blue-500/20 bg-gradient-to-br from-blue-600/15 to-blue-500/5 p-6">
                <p className="text-xs font-medium uppercase tracking-widest text-blue-400">Team Grade</p>
                <div className="mt-3 flex items-end gap-2">
                  <span className="text-5xl font-bold text-white">
                    {data.teamGrade != null ? Number(data.teamGrade).toFixed(2) : "—"}
                  </span>
                  <span className="mb-1.5 text-lg text-gray-500">/ 100</span>
                </div>
                <p className="mt-2 text-xs text-blue-300/70">{data.groupName}</p>
              </div>

              <div className="rounded-2xl border border-white/8 bg-gray-900 p-6 flex flex-col justify-between">
                <p className="text-xs font-medium uppercase tracking-widest text-gray-500">Publication</p>
                <div className="mt-3">
                  {data.published ? (
                    <span className="inline-flex items-center gap-2 rounded-full bg-green-500/15 px-3 py-1.5 text-sm font-medium text-green-400">
                      <span className="h-2 w-2 rounded-full bg-green-400" />
                      Published
                    </span>
                  ) : (
                    <span className="inline-flex items-center gap-2 rounded-full bg-amber-500/15 px-3 py-1.5 text-sm font-medium text-amber-400">
                      <span className="h-2 w-2 rounded-full bg-amber-400" />
                      Not Published
                    </span>
                  )}
                  {data.publishedAt && (
                    <p className="mt-2 text-xs text-gray-600">
                      {new Date(data.publishedAt).toLocaleDateString("tr-TR", {
                        day: "numeric", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit"
                      })}
                    </p>
                  )}
                </div>
              </div>
            </div>

            {/* Per-deliverable breakdown */}
            {data.deliverables && data.deliverables.length > 0 && (
              <div>
                <h2 className="mb-3 text-xs font-medium uppercase tracking-widest text-gray-500">Grade by Deliverable</h2>
                <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                  {data.deliverables.map((d) => {
                    const color = DELIVERABLE_COLORS[d.deliverableType] ?? DELIVERABLE_COLORS.PROPOSAL;
                    const pct = Number(d.finalWeight);
                    return (
                      <div key={d.deliverableType} className={`rounded-2xl border ${color.border} ${color.bg} p-5`}>
                        <div className="flex items-center gap-2 mb-4">
                          <span className={`h-2 w-2 rounded-full ${color.dot}`} />
                          <p className={`text-xs font-semibold uppercase tracking-wide ${color.text}`}>
                            {DELIVERABLE_LABELS[d.deliverableType] ?? d.deliverableType}
                          </p>
                          <span className="ml-auto text-xs text-gray-500">{pct.toFixed(0)}% weight</span>
                        </div>

                        <div className="mb-4">
                          <p className="text-xs text-gray-500 mb-1">Team contribution</p>
                          <p className="text-2xl font-bold text-white">{Number(d.contribution).toFixed(2)}</p>
                        </div>

                        <div className="space-y-1.5 border-t border-white/8 pt-3">
                          <Row label="Raw score" value={Number(d.rawGrade).toFixed(1)} />
                          <Row label="Scalar" value={Number(d.scalar).toFixed(3)} />
                          <Row label="Scaled grade" value={Number(d.scaledGrade).toFixed(2)} />
                          <Row label="Scrum avg" value={Number(d.scrumAverage).toFixed(1)} />
                          <Row label="Code review avg" value={Number(d.codeReviewAverage).toFixed(1)} />
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}

            {/* Individual student grades */}
            {data.students && data.students.length > 0 && (
              <div>
                <h2 className="mb-3 text-xs font-medium uppercase tracking-widest text-gray-500">
                  Individual Grades ({data.students.length} members)
                </h2>
                <div className="rounded-2xl border border-white/8 bg-gray-900 overflow-hidden">
                  <table className="w-full">
                    <thead>
                      <tr className="border-b border-white/5">
                        <th className="px-5 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">Student</th>
                        <th className="px-5 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-500">SP Ratio</th>
                        <th className="px-5 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-500">Final Grade</th>
                        <th className="px-5 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-500">Status</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-white/5">
                      {data.students.map((s) => (
                        <tr key={s.userId} className="hover:bg-white/[0.02] transition-colors">
                          <td className="px-5 py-3">
                            <div className="flex items-center gap-3">
                              <div className="h-8 w-8 rounded-full bg-white/8 flex items-center justify-center shrink-0">
                                <span className="text-xs font-bold text-gray-300">
                                  {s.fullName?.charAt(0).toUpperCase() ?? "?"}
                                </span>
                              </div>
                              <div>
                                <p className="text-sm font-medium text-white">{s.fullName}</p>
                                {s.githubUsername && (
                                  <p className="text-xs text-gray-500">@{s.githubUsername}</p>
                                )}
                              </div>
                            </div>
                          </td>
                          <td className="px-5 py-3 text-right text-sm text-gray-400">
                            {s.spRatio != null ? `${(Number(s.spRatio) * 100).toFixed(1)}%` : "—"}
                          </td>
                          <td className="px-5 py-3 text-right">
                            <span className="text-lg font-bold text-white">
                              {s.finalGrade != null ? Number(s.finalGrade).toFixed(2) : "—"}
                            </span>
                          </td>
                          <td className="px-5 py-3 text-right">
                            {s.published ? (
                              <span className="inline-flex items-center gap-1 rounded-full bg-green-500/15 px-2 py-0.5 text-xs font-medium text-green-400">
                                Published
                              </span>
                            ) : (
                              <span className="inline-flex items-center gap-1 rounded-full bg-gray-500/15 px-2 py-0.5 text-xs font-medium text-gray-500">
                                Draft
                              </span>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </div>
        )}
      </main>
    </div>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between">
      <span className="text-xs text-gray-500">{label}</span>
      <span className="text-xs font-medium text-gray-300">{value}</span>
    </div>
  );
}
