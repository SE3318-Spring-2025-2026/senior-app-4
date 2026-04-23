"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import CommitteeGradingDrawer from "@/components/CommitteeGradingDrawer";
import { getToken, getUser } from "@/lib/auth";
import { mockGroups } from "@/lib/mock-groups";
import { mockCommitteeSubmissions } from "@/lib/mock-submissions";
import { CommitteeSubmissionPreview } from "@/lib/submission-types";

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
  const [activeSubmission, setActiveSubmission] = useState<CommitteeSubmissionPreview | null>(null);
  const [recentlySubmittedId, setRecentlySubmittedId] = useState<number | null>(null);

  const token = getToken();
  const user = getUser();

  useEffect(() => {
    if (!token || !user) {
      router.replace("/auth/login");
      return;
    }

    if (user.requiresPasswordChange) {
      router.replace("/auth/change-password");
    }
  }, [router, token, user]);

  const rawGroupId = params.groupId;
  const groupId = Number(Array.isArray(rawGroupId) ? rawGroupId[0] : rawGroupId);
  const group = mockGroups.find((entry) => entry.groupId === groupId);

  const submissions = useMemo(
    () => mockCommitteeSubmissions.filter((entry) => entry.groupId === groupId),
    [groupId]
  );

  if (!token || !user || user.requiresPasswordChange) {
    return (
      <div className="min-h-screen bg-gray-950 flex items-center justify-center">
        <svg className="w-6 h-6 animate-spin text-blue-500" fill="none" viewBox="0 0 24 24">
          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
        </svg>
      </div>
    );
  }

  if (user.role !== "professor") {
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

  if (!group) {
    return (
      <main className="min-h-screen flex items-center justify-center bg-gray-950 px-6 text-white">
        <div className="max-w-md rounded-2xl border border-white/10 bg-gray-900/80 p-8 text-center">
          <h1 className="text-xl font-semibold">Group not found</h1>
          <p className="mt-3 text-sm text-gray-400">
            We could not find a grading workspace for this group.
          </p>
        </div>
      </main>
    );
  }

  return (
    <>
      <main className="min-h-screen bg-gray-950 px-6 py-10 text-white">
        <div className="mx-auto max-w-5xl space-y-8">
          <div className="space-y-4">
            <Link href={`/groups/${group.groupId}`} className="text-sm text-blue-400 hover:underline">
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
                  <p className="mt-1 text-sm font-medium text-white">{group.advisorName || "Not Assigned"}</p>
                </div>
              </div>
            </div>
          </div>

          {/* TODO(#74): replace mock submissions with real Process 3 professor-facing
              list/detail endpoints when submission retrieval is implemented. */}
          {submissions.length === 0 ? (
            <div className="rounded-2xl border border-dashed border-white/10 bg-gray-900/70 px-6 py-16 text-center shadow-lg shadow-black/20">
              <h2 className="text-xl font-semibold text-white">No submissions available</h2>
              <p className="mt-2 text-gray-400">
                This group does not have a grading-ready mock submission yet.
              </p>
            </div>
          ) : (
            <div className="grid gap-6">
              {submissions.map((submission) => {
                const readyForGrading = submission.status === "APPROVED";
                const wasSubmitted = recentlySubmittedId === submission.submissionId;

                return (
                  <article
                    key={submission.submissionId}
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
                          <h2 className="text-xl font-semibold text-white">{submission.fileName}</h2>
                          <p className="mt-2 text-sm text-gray-400">
                            Submitted by {submission.submittedBy} on {formatDate(submission.submittedAt)}.
                          </p>
                        </div>

                        <div className="grid gap-3 sm:grid-cols-3">
                          <MetaPill label="Committee" value={submission.assignedCommitteeName} />
                          <MetaPill label="Deadline" value={formatDate(submission.deadline)} />
                          <MetaPill label="Comments" value={`${submission.commentsCount}`} />
                        </div>
                      </div>

                      <div className="flex flex-col items-start gap-3 lg:items-end">
                        <a
                          href={submission.fileUrl}
                          target="_blank"
                          rel="noreferrer"
                          className="rounded-xl border border-white/10 bg-white/5 px-4 py-2 text-sm font-medium text-gray-200 transition hover:bg-white/10"
                        >
                          Download file
                        </a>

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
                            TODO: enable grading only after the real submission detail API confirms it is grade-ready.
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

function MetaPill({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl bg-white/5 px-4 py-3">
      <p className="text-xs text-gray-500">{label}</p>
      <p className="mt-1 text-sm font-medium text-white">{value}</p>
    </div>
  );
}
