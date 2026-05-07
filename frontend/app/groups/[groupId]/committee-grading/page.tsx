"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import CommitteeGradingDrawer from "@/components/CommitteeGradingDrawer";
import { getToken, getUser } from "@/lib/auth";
import { fetchGroupDetail, type ApiGroupDetail } from "@/lib/groups-api";
import { fetchSubmissions, type SubmissionSummary } from "@/lib/submissions-api";

type PageState = "loading" | "unauthorized" | "ready" | "error";

function formatDate(value: string) {
  return new Date(value).toLocaleString("en-US", {
    day: "numeric",
    month: "long",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export default function CommitteeGradingPage() {
  const router = useRouter();
  const params = useParams();
  const [pageState, setPageState] = useState<PageState>("loading");
  const [group, setGroup] = useState<ApiGroupDetail | null>(null);
  const [submissions, setSubmissions] = useState<SubmissionSummary[]>([]);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [activeSubmission, setActiveSubmission] = useState<SubmissionSummary | null>(null);
  const [recentlySubmittedId, setRecentlySubmittedId] = useState<number | null>(null);

  const rawGroupId = params.groupId;
  const groupId = Number(Array.isArray(rawGroupId) ? rawGroupId[0] : rawGroupId);

  useEffect(() => {
    const token = getToken();
    const user = getUser();

    if (!token || !user) {
      router.replace("/auth/login");
      return;
    }
    if (user.requiresPasswordChange) {
      router.replace("/auth/change-password");
      return;
    }
    if (user.role !== "professor") {
      setPageState("unauthorized");
      return;
    }

    let cancelled = false;

    async function load() {
      try {
        const [groupData, subsData] = await Promise.all([
          fetchGroupDetail(groupId),
          fetchSubmissions({ teamId: String(groupId), size: 50 }),
        ]);
        if (!cancelled) {
          setGroup(groupData);
          setSubmissions(subsData.data ?? []);
          setPageState("ready");
        }
      } catch (err) {
        if (!cancelled) {
          setErrorMsg(err instanceof Error ? err.message : "Failed to load data.");
          setPageState("error");
        }
      }
    }

    load();
    return () => { cancelled = true; };
  }, [router, groupId]);

  if (pageState === "loading") {
    return (
      <div className="min-h-screen bg-gray-950 flex items-center justify-center">
        <svg className="w-6 h-6 animate-spin text-blue-500" fill="none" viewBox="0 0 24 24">
          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
        </svg>
      </div>
    );
  }

  if (pageState === "unauthorized") {
    return (
      <main className="min-h-screen flex items-center justify-center bg-gray-950 px-6 text-white">
        <div className="max-w-md rounded-2xl border border-red-500/20 bg-red-500/8 p-8 text-center">
          <h1 className="text-xl font-semibold">Access restricted</h1>
          <p className="mt-3 text-sm text-red-100/80">
            Only professors can access the committee grading workspace.
          </p>
        </div>
      </main>
    );
  }

  if (pageState === "error" || !group) {
    return (
      <main className="min-h-screen flex items-center justify-center bg-gray-950 px-6 text-white">
        <div className="max-w-md rounded-2xl border border-white/10 bg-gray-900/80 p-8 text-center">
          <h1 className="text-xl font-semibold">Group not found</h1>
          <p className="mt-3 text-sm text-gray-400">
            {errorMsg ?? "We could not find a grading workspace for this group."}
          </p>
        </div>
      </main>
    );
  }

  const advisor = group.members.find((m) => m.role === "ADVISOR");

  return (
    <>
      <main className="min-h-screen bg-gray-950 px-6 py-10 text-white">
        <div className="mx-auto max-w-5xl space-y-8">
          <div className="space-y-4">
            <Link href={`/groups/${group.id}`} className="text-sm text-blue-400 hover:underline">
              {"<- Back to group"}
            </Link>

            <div className="rounded-2xl border border-white/10 bg-gray-900/70 p-6 shadow-lg shadow-black/20 backdrop-blur">
              <p className="text-sm text-gray-400">Committee workspace</p>
              <h1 className="mt-2 text-3xl font-bold text-white">Committee Grading</h1>
              <p className="mt-2 text-sm text-gray-400">
                Review approved submissions, open the grading drawer, and submit the committee score.
              </p>
              <div className="mt-5 grid gap-4 sm:grid-cols-2">
                <div className="rounded-xl bg-white/5 px-4 py-3">
                  <p className="text-xs text-gray-500">Group</p>
                  <p className="mt-1 text-sm font-medium text-white">{group.groupName}</p>
                </div>
                <div className="rounded-xl bg-white/5 px-4 py-3">
                  <p className="text-xs text-gray-500">Advisor</p>
                  <p className="mt-1 text-sm font-medium text-white">
                    {advisor ? advisor.fullName : "Not Assigned"}
                  </p>
                </div>
              </div>
            </div>
          </div>

          {submissions.length === 0 ? (
            <div className="rounded-2xl border border-dashed border-white/10 bg-gray-900/70 px-6 py-16 text-center shadow-lg shadow-black/20">
              <h2 className="text-xl font-semibold text-white">No submissions available</h2>
              <p className="mt-2 text-gray-400">This group has no submissions yet.</p>
            </div>
          ) : (
            <div className="grid gap-6">
              {submissions.map((submission) => {
                const readyForGrading = submission.status === "APPROVED";
                const wasSubmitted = recentlySubmittedId === submission.id;

                return (
                  <article
                    key={submission.id}
                    className="rounded-2xl border border-white/10 bg-gray-900/70 p-6 shadow-lg shadow-black/20 backdrop-blur"
                  >
                    <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                      <div className="space-y-3">
                        <div className="flex flex-wrap items-center gap-3">
                          <span className="rounded-full bg-blue-500/10 px-3 py-1 text-xs font-medium text-blue-300">
                            {submission.deliverableType}
                          </span>
                          <span className="rounded-full border border-white/10 bg-white/5 px-3 py-1 text-xs font-medium text-gray-300">
                            {submission.status}
                          </span>
                          {wasSubmitted && (
                            <span className="rounded-full border border-green-500/20 bg-green-500/10 px-3 py-1 text-xs font-medium text-green-300">
                              Submitted just now
                            </span>
                          )}
                        </div>

                        <div>
                          <h2 className="text-xl font-semibold text-white">
                            {submission.deliverableType} — #{submission.id}
                          </h2>
                          <p className="mt-2 text-sm text-gray-400">
                            Submitted on {formatDate(submission.submittedAt)}.
                          </p>
                        </div>

                        <div className="grid gap-3 sm:grid-cols-2">
                          {submission.deadline && (
                            <MetaPill label="Deadline" value={formatDate(submission.deadline)} />
                          )}
                          {submission.revisionNumber != null && (
                            <MetaPill label="Revision" value={`v${submission.revisionNumber}`} />
                          )}
                        </div>
                      </div>

                      <div className="flex flex-col items-start gap-3 lg:items-end">
                        <button
                          type="button"
                          onClick={() => setActiveSubmission(submission)}
                          disabled={!readyForGrading}
                          className={[
                            "rounded-xl px-4 py-2.5 text-sm font-medium transition-all",
                            readyForGrading
                              ? "bg-blue-600 text-white shadow-lg shadow-blue-600/20 hover:bg-blue-500"
                              : "cursor-not-allowed border border-white/10 bg-white/5 text-gray-500",
                          ].join(" ")}
                        >
                          Open grading drawer
                        </button>

                        {!readyForGrading && (
                          <p className="max-w-xs text-right text-xs text-amber-300/80">
                            Grading is only available for APPROVED submissions.
                          </p>
                        )}
                      </div>
                    </div>
                  </article>
                );
              })}
            </div>
          )}
        </div>
      </main>

      <CommitteeGradingDrawer
        open={Boolean(activeSubmission)}
        submission={activeSubmission}
        onClose={() => setActiveSubmission(null)}
        onSubmitted={(submissionId) => setRecentlySubmittedId(submissionId)}
      />
    </>
  );
}

function MetaPill({ label, value }: { readonly label: string; readonly value: string }) {
  return (
    <div className="rounded-xl bg-white/5 px-4 py-3">
      <p className="text-xs text-gray-500">{label}</p>
      <p className="mt-1 text-sm font-medium text-white">{value}</p>
    </div>
  );
}
