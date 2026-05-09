"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { getToken, getUser } from "@/lib/auth";
import Sidebar from "@/components/Sidebar";
import { fetchCurrentUserGroupId } from "@/lib/groups-api";
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

export default function GradesPage() {
  const router = useRouter();
  const [ready, setReady] = useState(false);

  useEffect(() => {
    const token = getToken();
    const user = getUser();
    if (!token || !user) { router.replace("/auth/login"); return; }
    if (user.requiresPasswordChange) { router.replace("/auth/change-password"); return; }
    if (user.role !== "student") { router.replace("/dashboard"); return; }
    setReady(true);
  }, [router]);

  if (!ready) return <Spinner />;
  return <GradesContent />;
}

function GradesContent() {
  const [data, setData] = useState<GradeData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const user = getUser();

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const groupId = await fetchCurrentUserGroupId();
      if (!groupId) { setError("no-group"); return; }
      const res = await fetchFinalGrades(groupId);
      setData(res.data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not load grades.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const myGrade = data?.students?.find(s => String(s.userId) === String(user?.userId));
  const spRatio = myGrade?.spRatio ?? 0;

  return (
    <div className="flex min-h-screen bg-gray-950">
      <Sidebar activePage="grades" />
      <main className="flex-1 overflow-y-auto p-8">
        <div className="mb-6">
          <h1 className="text-lg font-semibold text-white">My Grades</h1>
          <p className="mt-0.5 text-xs text-gray-500">Final project grades published by your coordinator</p>
        </div>

        {loading ? (
          <div className="flex h-48 items-center justify-center">
            <div className="h-6 w-6 animate-spin rounded-full border-b-2 border-blue-500" />
          </div>
        ) : error === "no-group" ? (
          <EmptyState title="No group found" subtitle="Join a group first to see your grades." />
        ) : error ? (
          <EmptyState title="Could not load grades" subtitle={error} />
        ) : !data?.published ? (
          <EmptyState
            title="Grades not published yet"
            subtitle="Your coordinator hasn't published the final grades yet. You'll receive a notification when they're ready."
            icon="clock"
          />
        ) : (
          <div className="space-y-6 max-w-4xl">

            {/* Top row: personal + team */}
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="rounded-2xl border border-blue-500/20 bg-gradient-to-br from-blue-600/15 to-blue-500/5 p-6">
                <p className="text-xs font-medium uppercase tracking-widest text-blue-400">Your Final Grade</p>
                <div className="mt-3 flex items-end gap-2">
                  <span className="text-5xl font-bold text-white">
                    {myGrade?.finalGrade != null ? Number(myGrade.finalGrade).toFixed(2) : "—"}
                  </span>
                  <span className="mb-1.5 text-lg text-gray-500">/ 100</span>
                </div>
                <p className="mt-2 text-xs text-blue-300/70">
                  Story point contribution: {(spRatio * 100).toFixed(1)}%
                </p>
              </div>

              <div className="rounded-2xl border border-white/8 bg-gray-900 p-6">
                <p className="text-xs font-medium uppercase tracking-widest text-gray-500">Team Grade</p>
                <div className="mt-3 flex items-end gap-2">
                  <span className="text-5xl font-bold text-white">
                    {data.teamGrade != null ? Number(data.teamGrade).toFixed(2) : "—"}
                  </span>
                  <span className="mb-1.5 text-lg text-gray-500">/ 100</span>
                </div>
                <p className="mt-2 text-xs text-gray-600">{data.groupName}</p>
              </div>
            </div>

            {/* Per-deliverable breakdown */}
            {data.deliverables && data.deliverables.length > 0 && (
              <div>
                <h2 className="mb-3 text-xs font-medium uppercase tracking-widest text-gray-500">Grade by Deliverable</h2>
                <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                  {data.deliverables.map((d) => {
                    const color = DELIVERABLE_COLORS[d.deliverableType] ?? DELIVERABLE_COLORS.PROPOSAL;
                    const myContribution = Number(d.contribution) * spRatio;
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

                        {/* My contribution */}
                        <div className="mb-4">
                          <p className="text-xs text-gray-500 mb-1">Your contribution</p>
                          <p className="text-2xl font-bold text-white">{myContribution.toFixed(2)}</p>
                        </div>

                        <div className="space-y-1.5 border-t border-white/8 pt-3">
                          <Row label="Raw score" value={Number(d.rawGrade).toFixed(1)} />
                          <Row label="Scalar" value={Number(d.scalar).toFixed(3)} />
                          <Row label="Team contribution" value={Number(d.contribution).toFixed(2)} />
                          <Row label="Scrum avg" value={Number(d.scrumAverage).toFixed(1)} />
                          <Row label="Code review avg" value={Number(d.codeReviewAverage).toFixed(1)} />
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}

            {/* Team members */}
            {data.students && data.students.length > 0 && (
              <div>
                <h2 className="mb-3 text-xs font-medium uppercase tracking-widest text-gray-500">Team Members</h2>
                <div className="rounded-2xl border border-white/8 bg-gray-900 overflow-hidden">
                  <table className="w-full">
                    <thead>
                      <tr className="border-b border-white/5">
                        <th className="px-5 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">Member</th>
                        <th className="px-5 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-500">SP Ratio</th>
                        <th className="px-5 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-500">Final Grade</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-white/5">
                      {data.students.map((s) => {
                        const isMe = String(s.userId) === String(user?.userId);
                        return (
                          <tr key={s.userId} className={isMe ? "bg-blue-500/5" : "hover:bg-white/[0.02]"}>
                            <td className="px-5 py-3">
                              <div className="flex items-center gap-3">
                                <div className={`h-7 w-7 rounded-full flex items-center justify-center text-xs font-bold shrink-0 ${isMe ? "bg-blue-500/30 text-blue-300" : "bg-white/10 text-gray-400"}`}>
                                  {s.fullName?.charAt(0).toUpperCase() ?? "?"}
                                </div>
                                <div>
                                  <p className={`text-sm font-medium ${isMe ? "text-blue-200" : "text-white"}`}>
                                    {s.fullName} {isMe && <span className="text-xs text-blue-400">(you)</span>}
                                  </p>
                                  {s.githubUsername && <p className="text-xs text-gray-500">@{s.githubUsername}</p>}
                                </div>
                              </div>
                            </td>
                            <td className="px-5 py-3 text-right text-sm text-gray-400">
                              {s.spRatio != null ? `${(Number(s.spRatio) * 100).toFixed(1)}%` : "—"}
                            </td>
                            <td className="px-5 py-3 text-right">
                              <span className={`text-sm font-semibold ${isMe ? "text-blue-300" : "text-white"}`}>
                                {s.finalGrade != null ? Number(s.finalGrade).toFixed(2) : "—"}
                              </span>
                            </td>
                          </tr>
                        );
                      })}
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

function EmptyState({ title, subtitle, icon = "grade" }: { title: string; subtitle: string; icon?: "clock" | "grade" }) {
  return (
    <div className="rounded-2xl border border-dashed border-white/10 bg-gray-900 p-16 text-center">
      {icon === "clock" ? (
        <svg className="mx-auto mb-3 h-8 w-8 text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 6v6h4.5m4.5 0a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      ) : (
        <svg className="mx-auto mb-3 h-8 w-8 text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M10.5 6h9.75M10.5 12h9.75M10.5 18h9.75M3.75 6h2.25M3.75 12h2.25M3.75 18h2.25" />
        </svg>
      )}
      <p className="text-sm font-medium text-white">{title}</p>
      <p className="mt-1 text-xs text-gray-500">{subtitle}</p>
    </div>
  );
}
