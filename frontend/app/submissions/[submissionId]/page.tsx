"use client";

import { startTransition, useCallback, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import {
  ArrowLeft,
  CheckCircle2,
  Download,
  FileText,
  GitBranch,
  RefreshCw,
  Star,
  Users,
} from "lucide-react";
import { toast } from "sonner";
import dynamic from "next/dynamic";
import Sidebar from "@/components/Sidebar";
import { getToken, getUser } from "@/lib/auth";
import {
  createReview,
  fetchCriteriaForDeliverableType,
  fetchSubmissionDetail,
  fetchSubmissionGrades,
  fetchSubmissionRevisionHistory,
  submitGrade,
  updateGrade,
  type DeliverableType,
  type GradeItem,
  type GradingCriteriaItem,
  type SubmissionDetail,
  type SubmissionGradeSummary,
  type SubmissionId,
  type SubmissionRevision,
  type SubmissionRevisionNode,
  type SubmissionStatus,
} from "@/lib/submissions-api";
import {
  fetchDemonstrationGrade,
  saveDemonstrationGrade,
} from "@/lib/final-grading-api";

const AnnotatedDocumentViewer = dynamic(
  () => import("@/components/AnnotatedDocumentViewer"),
  { ssr: false },
);

type AuthState = "loading" | "ready" | "denied";

const ALLOWED_ROLES = ["coordinator", "professor"];

export default function SubmissionDetailPage() {
  const router = useRouter();
  const [authState, setAuthState] = useState<AuthState>("loading");

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

    startTransition(() => {
      setAuthState(ALLOWED_ROLES.includes(user.role) ? "ready" : "denied");
    });
  }, [router]);

  if (authState === "loading") return <Spinner />;
  if (authState === "denied") return <AccessDenied />;

  return <SubmissionDetailWorkspace />;
}

function SubmissionDetailWorkspace() {
  const params = useParams();
  const submissionIdParam = params.submissionId;
  const submissionId = Array.isArray(submissionIdParam) ? submissionIdParam[0] : submissionIdParam;

  const [detail, setDetail] = useState<SubmissionDetail | null>(null);
  const [revisionHistory, setRevisionHistory] = useState<SubmissionRevision[] | SubmissionRevisionNode[]>([]);
  const [gradeSummary, setGradeSummary] = useState<SubmissionGradeSummary | null>(null);
  const [criteria, setCriteria] = useState<GradingCriteriaItem[]>([]);
  const [revisionWarning, setRevisionWarning] = useState<string | null>(null);
  const [gradeWarning, setGradeWarning] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (!submissionId) return;

    setLoading(true);
    setError(null);
    setRevisionWarning(null);
    setGradeWarning(null);

    try {
      const detailResponse = await fetchSubmissionDetail(submissionId);
      const nextDetail = detailResponse.data;
      setDetail(nextDetail);
      setRevisionHistory(nextDetail.revisionHistory ?? []);
      setGradeSummary(nextDetail.gradeSummary ?? null);

      const [revisionResult, gradesResult, criteriaData] = await Promise.allSettled([
        fetchSubmissionRevisionHistory(submissionId),
        fetchSubmissionGrades(submissionId),
        fetchCriteriaForDeliverableType(nextDetail.deliverableType as string),
      ]);

      if (criteriaData.status === "fulfilled") {
        setCriteria(criteriaData.value);
      }

      if (revisionResult.status === "fulfilled") {
        const revisions = revisionResult.value.data;
        if (revisions && revisions.length > 0) {
          setRevisionHistory(revisions);
        }
      } else {
        setRevisionWarning(
          revisionResult.reason instanceof Error
            ? revisionResult.reason.message
            : "Revision history could not be loaded.",
        );
      }

      if (gradesResult.status === "fulfilled") {
        setGradeSummary(gradesResult.value.data ?? nextDetail.gradeSummary ?? null);
      } else {
        setGradeWarning(
          gradesResult.reason instanceof Error
            ? gradesResult.reason.message
            : "Grade details could not be loaded.",
        );
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : "Submission detail could not be loaded.";
      setError(message);
      setDetail(null);
      toast.error(message);
    } finally {
      setLoading(false);
    }
  }, [submissionId]);

  useEffect(() => {
    load();
  }, [load]);

  const revisionTree = useMemo(
    () => buildRevisionTree(detail, revisionHistory),
    [detail, revisionHistory],
  );

  return (
    <div className="flex min-h-screen bg-gray-950 text-white">
      <Sidebar activePage="submissions" />
      <main className="flex min-w-0 flex-1 flex-col">
        <div className="flex shrink-0 items-center justify-between border-b border-white/5 px-8 py-4">
          <div className="min-w-0">
            <Link
              href="/professor/submissions"
              className="inline-flex items-center gap-2 text-xs text-gray-500 transition hover:text-blue-300"
            >
              <ArrowLeft className="h-3.5 w-3.5" />
              Back to submissions
            </Link>
            <h1 className="mt-2 truncate text-base font-semibold text-white">Submission Detail</h1>
            <p className="mt-0.5 text-xs text-gray-500">
              Review the document content and annotate sections with grading criteria
            </p>
          </div>
          <button
            type="button"
            onClick={load}
            disabled={loading}
            className="inline-flex items-center gap-2 rounded-lg border border-white/10 px-3 py-1.5 text-xs text-gray-400 transition hover:bg-white/5 hover:text-white disabled:cursor-not-allowed disabled:opacity-50"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${loading ? "animate-spin" : ""}`} />
            Refresh
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-8">
          {loading ? (
            <div className="flex h-64 items-center justify-center">
              <RefreshCw className="h-6 w-6 animate-spin text-blue-500" />
            </div>
          ) : error || !detail ? (
            <ErrorState message={error ?? "Submission detail was not returned by the API."} />
          ) : detail.deliverableType === "DEMONSTRATION" && !detail.assignedCommitteeId ? (
            /* Advisor-direct presentation grade — rubric grading panel */
            <div className="space-y-6">
              <SubmissionHeader detail={detail} />
              <PresentationGradePanel groupId={Number(detail.teamId)} onSaved={load} />
            </div>
          ) : (
            <div className="space-y-6">
              <SubmissionHeader detail={detail} />

              <div className="grid gap-6 xl:grid-cols-[minmax(0,1.45fr)_minmax(360px,0.8fr)]">
                <div className="space-y-6">
                  <OverviewPanel detail={detail} />

                  {/* Annotated document viewer — shown when markdown content exists */}
                  {detail.content && (
                    <section className="rounded-2xl border border-white/8 bg-gray-900 p-6">
                      <div className="mb-5 flex items-center gap-2">
                        <FileText className="h-4 w-4 text-purple-300" />
                        <h3 className="text-sm font-semibold text-white">Document Content</h3>
                        <span className="ml-auto text-xs text-gray-500">
                          Select text to annotate and link to grading criteria
                        </span>
                      </div>
                      <AnnotatedDocumentViewer
                        submissionId={detail.id}
                        htmlContent={detail.content}
                        criteria={criteria.map((c) => ({ id: c.id, name: c.name }))}
                      />
                    </section>
                  )}

                  <RevisionHistoryPanel
                    currentSubmissionId={detail.id}
                    tree={revisionTree}
                    warning={revisionWarning}
                  />
                </div>

                <div className="sticky top-8 self-start space-y-6 max-h-[calc(100vh-8rem)] overflow-y-auto">
                  <ReviewPanel detail={detail} onReviewed={load} />
                  <GradePanel summary={gradeSummary} warning={gradeWarning} submissionId={detail.id} criteria={criteria} onGraded={load} />
                  <FilePanel detail={detail} />
                </div>
              </div>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}

function SubmissionHeader({ detail }: { detail: SubmissionDetail }) {
  return (
    <div className="rounded-2xl border border-white/8 bg-gray-900 px-6 py-5">
      <div className="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2.5">
            <SubmissionStatusBadge status={detail.status} />
            <DeliverableTypeBadge type={detail.deliverableType} />
            <span className="rounded-full border border-white/10 bg-white/5 px-2.5 py-1 text-xs font-medium text-gray-300">
              Revision {detail.revisionNumber ?? 1}
            </span>
          </div>
          <h2 className="mt-4 truncate text-2xl font-semibold text-white">
            {detail.teamName ?? `Team #${detail.teamId}`}
          </h2>
          <p className="mt-2 max-w-2xl text-sm text-gray-400">
            Submitted {formatDateTime(detail.submittedAt)}
            {detail.deadline ? `, due ${formatDateTime(detail.deadline)}` : ""}
          </p>
        </div>

        <div className="grid min-w-[280px] grid-cols-2 gap-3">
          <Metric label="Reviews" value={`${detail.reviewSummary?.totalReviews ?? 0}`} />
          <Metric label="Average" value={formatGrade(detail.gradeSummary?.averageGrade)} />
        </div>
      </div>
    </div>
  );
}

function OverviewPanel({ detail }: { detail: SubmissionDetail }) {
  return (
    <section className="rounded-2xl border border-white/8 bg-gray-900 p-6">
      <div className="mb-5 flex items-center gap-2">
        <FileText className="h-4 w-4 text-blue-300" />
        <h3 className="text-sm font-semibold text-white">Submission Overview</h3>
      </div>
      <div className="grid gap-3 sm:grid-cols-2">
        <InfoTile label="Submission ID" value={String(detail.id)} />
        <InfoTile label="Team ID" value={String(detail.teamId)} />
        <InfoTile label="Committee" value={detail.assignedCommitteeName ?? valueOrDash(detail.assignedCommitteeId)} />
        <InfoTile label="Parent Submission" value={valueOrDash(detail.parentSubmissionId)} />
        <InfoTile label="Submitted" value={formatDateTime(detail.submittedAt)} />
        <InfoTile label="Deadline" value={detail.deadline ? formatDateTime(detail.deadline) : "-"} />
      </div>
    </section>
  );
}

function RevisionHistoryPanel({
  currentSubmissionId,
  tree,
  warning,
}: {
  currentSubmissionId: SubmissionId;
  tree: SubmissionRevisionNode[];
  warning: string | null;
}) {
  return (
    <section className="rounded-2xl border border-white/8 bg-gray-900 p-6">
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-center gap-2">
          <GitBranch className="h-4 w-4 text-blue-300" />
          <h3 className="text-sm font-semibold text-white">Revision History</h3>
        </div>
        <span className="rounded-full border border-white/10 bg-white/5 px-2.5 py-1 text-xs text-gray-400">
          {countRevisionNodes(tree)} node{countRevisionNodes(tree) === 1 ? "" : "s"}
        </span>
      </div>

      {warning && (
        <div className="mt-4 rounded-xl border border-amber-500/20 bg-amber-500/10 px-4 py-3 text-xs text-amber-100/80">
          {warning}
        </div>
      )}

      {tree.length === 0 ? (
        <div className="mt-5 rounded-xl border border-dashed border-white/10 bg-white/4 px-4 py-8 text-center">
          <p className="text-sm font-medium text-white">No revision history yet</p>
          <p className="mt-1 text-xs text-gray-500">The original submission will appear here once the detail API returns revision data.</p>
        </div>
      ) : (
        <div className="mt-5 space-y-3">
          {tree.map((node) => (
            <RevisionTreeNode
              key={String(node.id)}
              node={node}
              currentSubmissionId={currentSubmissionId}
              depth={0}
            />
          ))}
        </div>
      )}
    </section>
  );
}

function RevisionTreeNode({
  node,
  currentSubmissionId,
  depth,
}: {
  node: SubmissionRevisionNode;
  currentSubmissionId: SubmissionId;
  depth: number;
}) {
  const current = sameId(node.id, currentSubmissionId);
  const children = sortRevisionNodes(node.children ?? []);

  return (
    <div className="relative">
      <div className="flex gap-3">
        {depth > 0 && (
          <div className="w-6 shrink-0">
            <div className="ml-3 h-6 w-px bg-white/10" />
            <div className="h-px w-6 bg-white/10" />
          </div>
        )}
        <div className={`flex-1 rounded-xl border px-4 py-3 ${current ? "border-blue-500/30 bg-blue-500/10" : "border-white/10 bg-white/4"}`}>
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="flex flex-wrap items-center gap-2">
              <span className="rounded-full bg-gray-950 px-2.5 py-1 text-xs font-medium text-gray-300">
                Rev. {node.revisionNumber}
              </span>
              <SubmissionStatusBadge status={node.status} />
              {current && (
                <span className="rounded-full border border-blue-400/30 bg-blue-400/10 px-2.5 py-1 text-xs font-medium text-blue-200">
                  Current
                </span>
              )}
            </div>
            <span className="text-xs text-gray-500">{formatDateTime(node.submittedAt)}</span>
          </div>
          <div className="mt-3 grid gap-3 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-end">
            <div className="min-w-0">
              <p className="truncate text-xs text-gray-500">Submission {String(node.id)}</p>
            </div>
            {node.fileUrl && (
              <a
                href={node.fileUrl}
                target="_blank"
                rel="noreferrer"
                className="inline-flex items-center justify-center gap-2 rounded-lg border border-white/10 px-3 py-2 text-xs font-medium text-gray-300 transition hover:bg-white/5 hover:text-white"
              >
                <Download className="h-3.5 w-3.5" />
                File
              </a>
            )}
          </div>
        </div>
      </div>

      {children.length > 0 && (
        <div className="ml-9 mt-3 space-y-3 border-l border-white/10 pl-3">
          {children.map((child) => (
            <RevisionTreeNode
              key={String(child.id)}
              node={child}
              currentSubmissionId={currentSubmissionId}
              depth={depth + 1}
            />
          ))}
        </div>
      )}
    </div>
  );
}

function ReviewPanel({ detail, onReviewed }: { detail: SubmissionDetail; onReviewed: () => void }) {
  const latestStatus = detail.reviewSummary?.latestStatus;
  const [modalOpen, setModalOpen] = useState(false);
  const [feedback, setFeedback] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const canRequestRevision =
    detail.status !== "GRADED" &&
    detail.status !== "SUPERSEDED" &&
    detail.status !== "REJECTED" &&
    detail.status !== "REVISION_REQUESTED";

  const handleRequestRevision = async () => {
    if (!feedback.trim()) {
      toast.error("Feedback is required when requesting a revision.");
      return;
    }
    setSubmitting(true);
    try {
      await createReview(detail.id, { decision: "REVISION_REQUESTED", comments: feedback.trim() });
      toast.success("Revision requested. The student has been notified.");
      setModalOpen(false);
      setFeedback("");
      onReviewed();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Failed to request revision.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <section className="rounded-2xl border border-white/8 bg-gray-900 p-6">
        <div className="mb-5 flex items-center gap-2">
          <CheckCircle2 className="h-4 w-4 text-green-300" />
          <h3 className="text-sm font-semibold text-white">Review Summary</h3>
        </div>
        <div className="grid gap-3">
          <InfoTile label="Total reviews" value={`${detail.reviewSummary?.totalReviews ?? 0}`} />
          <InfoTile label="Latest decision" value={latestStatus ? reviewDecisionLabel(latestStatus) : "Pending"} />
        </div>

        {canRequestRevision && (
          <div className="mt-4 border-t border-white/8 pt-4">
            <button
              type="button"
              onClick={() => setModalOpen(true)}
              className="inline-flex w-full items-center justify-center gap-2 rounded-xl border border-orange-500/30 bg-orange-500/10 px-4 py-2.5 text-sm font-medium text-orange-300 transition hover:bg-orange-500/20"
            >
              <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L10.582 16.07a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931z" />
              </svg>
              Request Revision
            </button>
          </div>
        )}
      </section>

      {/* Request Revision Modal */}
      {modalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 px-4">
          <div className="w-full max-w-md rounded-2xl border border-white/10 bg-gray-900 p-6 shadow-2xl">
            <h2 className="text-base font-semibold text-white">Request Revision</h2>
            <p className="mt-1 text-xs text-gray-500">
              Your feedback will be sent to the student as a notification. They will be able to submit one revision.
            </p>
            <div className="mt-4">
              <label className="mb-1.5 block text-xs font-medium text-gray-400">
                Feedback <span className="text-red-400">*</span>
              </label>
              <textarea
                value={feedback}
                onChange={(e) => setFeedback(e.target.value)}
                placeholder="Explain what needs to be revised..."
                rows={5}
                className="w-full rounded-lg border border-white/10 bg-white/5 px-3 py-2.5 text-sm text-white placeholder-gray-600 focus:border-orange-500/50 focus:outline-none resize-none"
              />
            </div>
            <div className="mt-4 flex gap-3">
              <button
                type="button"
                onClick={() => { setModalOpen(false); setFeedback(""); }}
                disabled={submitting}
                className="flex-1 rounded-xl border border-white/10 px-4 py-2.5 text-sm font-medium text-gray-400 transition hover:bg-white/5 disabled:opacity-50"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleRequestRevision}
                disabled={submitting || !feedback.trim()}
                className="flex-1 rounded-xl bg-orange-500 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-orange-400 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {submitting ? "Sending…" : "Send Request"}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}

const SOFT_GRADE_OPTIONS = [
  { label: "A", score: 100 },
  { label: "B", score: 80 },
  { label: "C", score: 60 },
  { label: "D", score: 50 },
  { label: "F", score: 0 },
] as const;

function letterToScore(letter: string): number {
  return SOFT_GRADE_OPTIONS.find((o) => o.label === letter)?.score ?? 0;
}

function GradePanel({
  summary,
  warning,
  submissionId,
  criteria,
  onGraded,
}: {
  summary: SubmissionGradeSummary | null;
  warning: string | null;
  submissionId: SubmissionId;
  criteria: GradingCriteriaItem[];
  onGraded: () => void;
}) {
  const grades = summary?.grades ?? [];
  const user = getUser();
  const myGrade = grades.find((g) => String(g.professorId) === String(user?.userId));

  const hasCriteria = criteria.length > 0;

  // flat-score fallback state
  const [gradeInput, setGradeInput] = useState<string>(myGrade ? String(myGrade.grade) : "");
  // criterion-based state: criterionId → letter/score string
  const [criterionInputs, setCriterionInputs] = useState<Record<number, string>>(() => {
    const init: Record<number, string> = {};
    criteria.forEach((c) => { init[c.id] = ""; });
    return init;
  });
  const [feedbackInput, setFeedbackInput] = useState<string>(myGrade?.feedback ?? "");
  const [criteriaGrades, setCriteriaGrades] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);
  const hasRubric = criteria.length > 0;
  const rubricScore = hasRubric ? calculateRubricScore(criteria, criteriaGrades) : null;

  useEffect(() => {
    setGradeInput(myGrade ? String(myGrade.grade) : "");
    setFeedbackInput(myGrade?.feedback ?? "");
    const scoreByCriterion = new Map((myGrade?.criteriaScores ?? []).map((score) => [String(score.criteriaId), score.score]));
    setCriteriaGrades(Object.fromEntries(
      criteria.map((criterion) => [
        String(criterion.id),
        scoreByCriterion.has(String(criterion.id))
          ? scoreToGrade(scoreByCriterion.get(String(criterion.id)) ?? 0, criterion.gradingType)
          : "",
      ]),
    ));
  }, [myGrade, criteria]);

  // live weighted score preview
  const weightedPreview = useMemo(() => {
    if (!hasCriteria) return null;
    let totalWeight = 0;
    let weightedSum = 0;
    let allFilled = true;
    for (const c of criteria) {
      const val = criterionInputs[c.id];
      if (!val) { allFilled = false; break; }
      const score = c.gradingType === "SOFT" ? letterToScore(val) : Number(val);
      totalWeight += c.weight;
      weightedSum += score * c.weight;
    }
    if (!allFilled || totalWeight === 0) return null;
    return (weightedSum / totalWeight).toFixed(1);
  }, [criteria, criterionInputs, hasCriteria]);

  const canSubmit = hasCriteria
    ? criteria.every((c) => Boolean(criterionInputs[c.id]))
    : gradeInput !== "";

  const handleSubmit = async () => {
    setSubmitting(true);
    try {
      let payload;
      if (hasCriteria) {
        const criteriaScores = criteria.map((c) => {
          const val = criterionInputs[c.id];
          return {
            criterionId: c.id,
            score: c.gradingType === "SOFT" ? letterToScore(val) : Number(val),
          };
        });
        payload = { feedback: feedbackInput || undefined, criteriaScores: criterionScores };
      } else {
        const gradeNum = parseFloat(gradeInput);
        if (isNaN(gradeNum) || gradeNum < 0 || gradeNum > 100) {
          toast.error("Grade must be between 0 and 100.");
          return;
        }
        payload = { grade: gradeNum, feedback: feedbackInput || undefined };
      }
      if (myGrade) {
        await updateGrade(submissionId, Number(myGrade.id), payload);
      } else {
        await submitGrade(submissionId, payload);
      }
      toast.success(myGrade ? "Grade updated." : "Grade submitted.");
      onGraded();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Failed to submit grade.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section className="rounded-2xl border border-white/8 bg-gray-900 p-6">
      <div className="mb-5 flex items-center gap-2">
        <Star className="h-4 w-4 text-amber-300" />
        <h3 className="text-sm font-semibold text-white">Grading Summary</h3>
      </div>

      {warning && (
        <div className="mb-4 rounded-xl border border-amber-500/20 bg-amber-500/10 px-4 py-3 text-xs text-amber-100/80">
          {warning}
        </div>
      )}

      {summary && (
        <div className="mb-5 grid grid-cols-3 gap-3">
          <Metric label="Average" value={formatGrade(summary.averageGrade)} />
          <Metric label="Submitted" value={`${summary.gradeCount}/${summary.totalCommitteeMembers}`} />
          <Metric label="Complete" value={summary.isGradingComplete ? "Yes" : "No"} />
        </div>
      )}

      {grades.length > 0 && (
        <div className="mb-5 overflow-hidden rounded-xl border border-white/10">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-white/5 text-left">
                <th className="px-4 py-3 text-xs font-medium uppercase tracking-wider text-gray-500">Professor</th>
                <th className="px-4 py-3 text-xs font-medium uppercase tracking-wider text-gray-500">Grade</th>
                <th className="px-4 py-3 text-xs font-medium uppercase tracking-wider text-gray-500">Graded</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5">
              {grades.map((grade) => (
                <GradeRow key={String(grade.id)} grade={grade} />
              ))}
            </tbody>
          </table>
        </div>
      )}

      <div className="space-y-3 border-t border-white/8 pt-5">
        <p className="text-xs font-medium text-gray-400">{myGrade ? "Update your grade" : "Submit your grade"}</p>

        {hasCriteria ? (
          <div className="space-y-3">
            {criteria.map((c) => (
              <div key={c.id} className="rounded-xl border border-white/10 bg-white/4 p-4">
                <div className="flex items-start justify-between gap-3 mb-3">
                  <div>
                    <p className="text-sm font-medium text-white">{c.name}</p>
                    {c.description && <p className="text-xs text-gray-500 mt-0.5">{c.description}</p>}
                  </div>
                  <div className="flex items-center gap-2 flex-shrink-0">
                    <span className={`px-2 py-0.5 rounded text-xs font-medium ${c.gradingType === "BINARY" ? "bg-amber-500/10 text-amber-300 border border-amber-500/20" : "bg-blue-500/10 text-blue-300 border border-blue-500/20"}`}>
                      {c.gradingType === "BINARY" ? "Binary" : "Soft"}
                    </span>
                    <span className="text-xs text-gray-500">{c.weight}%</span>
                  </div>
                </div>
                {c.gradingType === "BINARY" ? (
                  <div className="flex gap-2">
                    {[{ label: "S", score: "100" }, { label: "F", score: "0" }].map(({ label, score }) => (
                      <button
                        key={label}
                        type="button"
                        onClick={() => setCriterionInputs((prev) => ({ ...prev, [c.id]: score }))}
                        className={`px-4 py-2 rounded-lg text-sm font-medium transition border ${
                          criterionInputs[c.id] === score
                            ? label === "S"
                              ? "bg-green-500/20 border-green-500/40 text-green-300"
                              : "bg-red-500/20 border-red-500/40 text-red-300"
                            : "border-white/10 bg-white/5 text-gray-400 hover:bg-white/10"
                        }`}
                      >
                        {label} ({score})
                      </button>
                    ))}
                  </div>
                ) : (
                  <div className="flex gap-2 flex-wrap">
                    {SOFT_GRADE_OPTIONS.map(({ label, score }) => (
                      <button
                        key={label}
                        type="button"
                        onClick={() => setCriterionInputs((prev) => ({ ...prev, [c.id]: label }))}
                        className={`px-3 py-2 rounded-lg text-sm font-medium transition border ${
                          criterionInputs[c.id] === label
                            ? "bg-blue-500/20 border-blue-500/40 text-blue-200"
                            : "border-white/10 bg-white/5 text-gray-400 hover:bg-white/10"
                        }`}
                      >
                        {label} ({score})
                      </button>
                    ))}
                  </div>
                )}
              </div>
            ))}
            {weightedPreview !== null && (
              <div className="rounded-xl border border-amber-500/20 bg-amber-500/10 px-4 py-3 flex items-center justify-between">
                <span className="text-xs text-amber-200">Weighted score preview</span>
                <span className="text-sm font-semibold text-amber-300">{weightedPreview} / 100</span>
              </div>
            )}
          </div>
        ) : (
          <div className="flex items-center gap-3">
            <input
              type="number"
              min={0}
              max={100}
              step={0.5}
              value={gradeInput}
              onChange={(e) => setGradeInput(e.target.value)}
              placeholder="0–100"
              className="w-24 rounded-lg border border-white/10 bg-white/5 px-3 py-2 text-sm text-white placeholder-gray-600 focus:border-blue-500/50 focus:outline-none"
            />
            <span className="text-xs text-gray-500">/ 100</span>
          </div>
        )}

        <textarea
          value={feedbackInput}
          onChange={(e) => setFeedbackInput(e.target.value)}
          placeholder="Feedback (optional)"
          rows={3}
          className="w-full rounded-lg border border-white/10 bg-white/5 px-3 py-2 text-sm text-white placeholder-gray-600 focus:border-blue-500/50 focus:outline-none resize-none"
        />
        <button
          onClick={handleSubmit}
          disabled={submitting || !canSubmit}
          className="inline-flex items-center gap-2 rounded-lg bg-amber-500 px-4 py-2 text-sm font-medium text-gray-950 transition hover:bg-amber-400 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {submitting ? "Submitting…" : myGrade ? "Update Grade" : "Submit Grade"}
        </button>
      </div>
    </section>
  );
}

function GradeRow({ grade }: { grade: GradeItem }) {
  return (
    <tr className="bg-white/[0.02]">
      <td className="px-4 py-3">
        <p className="font-medium text-white">{grade.professorName}</p>
        {grade.feedback && <p className="mt-1 line-clamp-2 text-xs text-gray-500">{grade.feedback}</p>}
        {grade.criteriaScores && grade.criteriaScores.length > 0 && (
          <div className="mt-2 flex flex-wrap gap-1.5">
            {grade.criteriaScores.map((score) => (
              <span key={String(score.criteriaId)} className="rounded-full border border-white/10 bg-white/5 px-2 py-0.5 text-[11px] text-gray-400">
                {score.criteriaName}: {formatGrade(score.score)}
              </span>
            ))}
          </div>
        )}
      </td>
      <td className="px-4 py-3 text-gray-300">{formatGrade(grade.grade)}</td>
      <td className="px-4 py-3 text-xs text-gray-500">{formatDateTime(grade.gradedAt)}</td>
    </tr>
  );
}

function CriterionPicker({
  criterion,
  value,
  onChange,
}: {
  criterion: GradingCriteriaItem;
  value: string;
  onChange: (value: string) => void;
}) {
  const options = criterion.gradingType === "BINARY" ? ["S", "F"] : ["A", "B", "C", "D", "F"];
  return (
    <div className="rounded-lg border border-white/10 bg-white/[0.03] p-3">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-sm font-medium text-white">{criterion.name}</p>
          {criterion.description && <p className="mt-1 text-xs text-gray-500">{criterion.description}</p>}
        </div>
        <span className="shrink-0 text-xs font-semibold text-blue-300">{criterion.weight}%</span>
      </div>
      <div className="mt-3 flex flex-wrap gap-2">
        {options.map((option) => (
          <button
            key={option}
            type="button"
            onClick={() => onChange(option)}
            className={`flex h-8 min-w-8 items-center justify-center rounded-lg border px-2 text-xs font-semibold transition ${
              value === option
                ? "border-amber-400/40 bg-amber-400 text-gray-950"
                : "border-white/10 bg-white/5 text-gray-400 hover:text-white"
            }`}
          >
            {option}
          </button>
        ))}
      </div>
    </div>
  );
}

function calculateRubricScore(criteria: GradingCriteriaItem[], values: Record<string, string>) {
  if (criteria.some((criterion) => !values[String(criterion.id)])) return null;
  return criteria.reduce((sum, criterion) => {
    const raw = gradeToScore(values[String(criterion.id)], criterion.gradingType);
    return sum + (raw * criterion.weight) / 100;
  }, 0);
}

function gradeToScore(grade: string, gradingType?: "SOFT" | "BINARY" | null) {
  if (gradingType === "BINARY") {
    return grade === "S" ? 100 : 0;
  }
  switch (grade) {
    case "A": return 100;
    case "B": return 80;
    case "C": return 60;
    case "D": return 50;
    default: return 0;
  }
}

function scoreToGrade(score: number, gradingType?: "SOFT" | "BINARY" | null) {
  if (gradingType === "BINARY") return score >= 100 ? "S" : "F";
  if (score >= 100) return "A";
  if (score >= 80) return "B";
  if (score >= 60) return "C";
  if (score >= 50) return "D";
  return "F";
}

const PRES_SOFT_OPTIONS = [
  { label: "A", score: 100 },
  { label: "B", score: 80 },
  { label: "C", score: 60 },
  { label: "D", score: 50 },
  { label: "F", score: 0 },
] as const;

function presLetterToScore(letter: string): number {
  return PRES_SOFT_OPTIONS.find((o) => o.label === letter)?.score ?? 0;
}

function PresentationGradePanel({ groupId, onSaved }: { groupId: number; onSaved: () => void }) {
  const [criteria, setCriteria] = useState<GradingCriteriaItem[]>([]);
  const [inputs, setInputs] = useState<Record<number, string>>({});
  const [savedGrade, setSavedGrade] = useState<number | null>(null);
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    Promise.all([
      fetchCriteriaForDeliverableType("DEMONSTRATION"),
      fetchDemonstrationGrade(groupId),
    ]).then(([crit, existing]) => {
      if (cancelled) return;
      setCriteria(crit);
      setInputs(Object.fromEntries(crit.map((c) => [c.id, ""])));
      setSavedGrade(existing);
    }).catch(() => undefined).finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [groupId]);

  const weightedScore: number | null = (() => {
    if (criteria.some((c) => !inputs[c.id])) return null;
    let totalWeight = 0;
    let weightedSum = 0;
    for (const c of criteria) {
      const val = inputs[c.id];
      const score = c.gradingType === "BINARY" ? (val === "100" ? 100 : 0) : presLetterToScore(val);
      totalWeight += c.weight;
      weightedSum += score * c.weight;
    }
    return totalWeight === 0 ? null : weightedSum / totalWeight;
  })();

  const allFilled = criteria.length > 0 && criteria.every((c) => !!inputs[c.id]);

  const handleSave = async () => {
    if (weightedScore === null) { toast.error("Fill all criteria first."); return; }
    setSaving(true);
    try {
      const grade = parseFloat(weightedScore.toFixed(2));
      await saveDemonstrationGrade(groupId, grade);
      setSavedGrade(grade);
      toast.success("Presentation grade saved.");
      onSaved();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Failed to save grade.");
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="flex h-32 items-center justify-center rounded-2xl border border-white/8 bg-gray-900">
        <RefreshCw className="h-5 w-5 animate-spin text-blue-500" />
      </div>
    );
  }

  return (
    <div className="rounded-2xl border border-white/8 bg-gray-900 p-6">
      <div className="mb-6 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Star className="h-4 w-4 text-purple-300" />
          <h3 className="text-sm font-semibold text-white">Presentation Grade (Sunum Puanı)</h3>
        </div>
        {savedGrade !== null && (
          <span className="rounded-full border border-purple-500/30 bg-purple-500/10 px-3 py-1 text-xs font-medium text-purple-300">
            Current: {savedGrade.toFixed(1)} / 100
          </span>
        )}
      </div>

      {criteria.length === 0 ? (
        <div className="rounded-xl border border-dashed border-white/10 bg-white/4 px-4 py-8 text-center">
          <p className="text-sm font-medium text-white">No rubric criteria defined</p>
          <p className="mt-1 text-xs text-gray-500">Add DEMONSTRATION criteria in the coordinator rubrics page first.</p>
        </div>
      ) : (
        <div className="space-y-4">
          {criteria.map((c) => (
            <div key={c.id} className="rounded-xl border border-white/8 bg-white/[0.03] p-4">
              <div className="mb-3 flex items-start justify-between gap-3">
                <div>
                  <p className="text-sm font-medium text-white">{c.name}</p>
                  {c.description && <p className="mt-0.5 text-xs text-gray-500">{c.description}</p>}
                </div>
                <div className="flex shrink-0 items-center gap-2">
                  <span className={`rounded border px-2 py-0.5 text-xs font-medium ${c.gradingType === "BINARY" ? "border-amber-500/20 bg-amber-500/10 text-amber-300" : "border-blue-500/20 bg-blue-500/10 text-blue-300"}`}>
                    {c.gradingType === "BINARY" ? "Binary" : "Soft"}
                  </span>
                  <span className="text-xs text-gray-500">{c.weight}%</span>
                </div>
              </div>
              <div className="flex flex-wrap gap-2">
                {c.gradingType === "BINARY" ? (
                  [{ label: "S", score: "100" }, { label: "F", score: "0" }].map(({ label, score }) => (
                    <button key={label} type="button"
                      onClick={() => setInputs((prev) => ({ ...prev, [c.id]: score }))}
                      className={`rounded-lg border px-5 py-2 text-sm font-medium transition ${inputs[c.id] === score
                        ? label === "S" ? "border-green-500/40 bg-green-500/20 text-green-300" : "border-red-500/40 bg-red-500/20 text-red-300"
                        : "border-white/10 bg-white/5 text-gray-400 hover:bg-white/10"}`}>
                      {label}
                    </button>
                  ))
                ) : (
                  PRES_SOFT_OPTIONS.map(({ label }) => (
                    <button key={label} type="button"
                      onClick={() => setInputs((prev) => ({ ...prev, [c.id]: label }))}
                      className={`rounded-lg border px-4 py-2 text-sm font-medium transition ${inputs[c.id] === label
                        ? "border-purple-500/40 bg-purple-500/20 text-purple-200"
                        : "border-white/10 bg-white/5 text-gray-400 hover:bg-white/10"}`}>
                      {label}
                    </button>
                  ))
                )}
              </div>
            </div>
          ))}

          {weightedScore !== null && (
            <div className="flex items-center justify-between rounded-xl border border-purple-500/20 bg-purple-500/10 px-4 py-3">
              <span className="text-xs text-purple-200">Weighted score preview</span>
              <span className="text-sm font-semibold text-purple-300">{weightedScore.toFixed(1)} / 100</span>
            </div>
          )}

          <button type="button" onClick={handleSave} disabled={saving || !allFilled}
            className="w-full rounded-xl bg-purple-600 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-purple-500 disabled:cursor-not-allowed disabled:opacity-50">
            {saving ? "Saving…" : "Save Presentation Grade"}
          </button>
        </div>
      )}
    </div>
  );
}

function FilePanel({ detail }: { detail: SubmissionDetail }) {
  return (
    <section className="rounded-2xl border border-white/8 bg-gray-900 p-6">
      <div className="mb-5 flex items-center gap-2">
        <Download className="h-4 w-4 text-blue-300" />
        <h3 className="text-sm font-semibold text-white">Deliverable File</h3>
      </div>

      {detail.fileUrl ? (
        <div className="rounded-xl border border-blue-500/20 bg-blue-500/10 p-4">
          <p className="truncate text-sm font-medium text-white">{detail.fileName ?? "Submission file"}</p>
          <a
            href={detail.fileUrl}
            target="_blank"
            rel="noreferrer"
            className="mt-4 inline-flex items-center gap-2 rounded-lg bg-white px-4 py-2 text-sm font-medium text-gray-950 transition hover:bg-gray-100"
          >
            <Download className="h-4 w-4" />
            Download
          </a>
        </div>
      ) : (
        <div className="rounded-xl border border-dashed border-white/10 bg-white/4 px-4 py-8 text-center">
          <p className="text-sm font-medium text-white">No file attached</p>
          <p className="mt-1 text-xs text-gray-500">This submission uses the markdown editor — see Document Content above.</p>
        </div>
      )}
    </section>
  );
}

function InfoTile({ label, value }: { label: string; value: string }) {
  return (
    <div className="min-w-0 rounded-xl border border-white/8 bg-white/4 px-4 py-3">
      <p className="text-xs text-gray-500">{label}</p>
      <p className="mt-1 truncate text-sm font-medium text-white">{value}</p>
    </div>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl border border-white/8 bg-white/4 px-4 py-3 text-center">
      <p className="text-lg font-semibold text-white">{value}</p>
      <p className="mt-1 text-xs text-gray-500">{label}</p>
    </div>
  );
}

function SubmissionStatusBadge({ status }: { status: SubmissionStatus }) {
  const styles: Record<SubmissionStatus, string> = {
    PENDING_REVIEW: "bg-yellow-500/15 text-yellow-400 border-yellow-500/20",
    UNDER_REVIEW: "bg-blue-500/15 text-blue-400 border-blue-500/20",
    REVISION_REQUESTED: "bg-orange-500/15 text-orange-400 border-orange-500/20",
    APPROVED: "bg-green-500/15 text-green-400 border-green-500/20",
    GRADED: "bg-purple-500/15 text-purple-400 border-purple-500/20",
    REJECTED: "bg-red-500/15 text-red-400 border-red-500/20",
    SUBMITTED: "bg-gray-500/15 text-gray-400 border-gray-500/20",
    SUPERSEDED: "bg-gray-700/15 text-gray-500 border-gray-700/20",
  };
  const labels: Record<SubmissionStatus, string> = {
    PENDING_REVIEW: "Pending Review",
    UNDER_REVIEW: "Under Review",
    REVISION_REQUESTED: "Revision Requested",
    APPROVED: "Approved",
    GRADED: "Graded",
    REJECTED: "Rejected",
    SUBMITTED: "Submitted",
    SUPERSEDED: "Superseded",
  };

  return (
    <span className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-medium ${styles[status]}`}>
      {labels[status]}
    </span>
  );
}

function DeliverableTypeBadge({ type }: { type: DeliverableType }) {
  const labels: Record<string, string> = {
    PROPOSAL: "Proposal",
    REVISED_PROPOSAL: "Revised Proposal",
    STATEMENT_OF_WORK: "SoW",
    DEMONSTRATION: "Demonstration",
  };
  return (
    <span className="inline-flex rounded-full border border-white/10 bg-white/8 px-2.5 py-1 text-xs font-medium text-gray-300">
      {labels[type] ?? type}
    </span>
  );
}

function ErrorState({ message }: { message: string }) {
  return (
    <div className="flex h-64 items-center justify-center">
      <div className="max-w-lg rounded-2xl border border-red-500/20 bg-red-500/10 p-8 text-center">
        <h2 className="text-lg font-semibold text-white">Submission detail unavailable</h2>
        <p className="mt-3 text-sm text-red-100/80">{message}</p>
      </div>
    </div>
  );
}

function Spinner() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-950">
      <RefreshCw className="h-6 w-6 animate-spin text-blue-500" />
    </div>
  );
}

function AccessDenied() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-950 px-6">
      <div className="max-w-md rounded-2xl border border-red-500/20 bg-red-500/10 p-8 text-center">
        <Users className="mx-auto h-8 w-8 text-red-300" />
        <h1 className="mt-4 text-lg font-semibold text-white">Access restricted</h1>
        <p className="mt-2 text-sm text-red-100/80">Only coordinators and advisors can access submission details.</p>
      </div>
    </div>
  );
}

// ── Revision tree helpers ─────────────────────────────────────────────────────

function buildRevisionTree(
  detail: SubmissionDetail | null,
  history: SubmissionRevision[] | SubmissionRevisionNode[],
): SubmissionRevisionNode[] {
  const source = history.length > 0 ? history : detail ? [detailToRevision(detail)] : [];
  const flattened = flattenRevisionNodes(source);
  const withCurrent =
    detail && !flattened.some((node) => sameId(node.id, detail.id))
      ? [...flattened, detailToRevision(detail)]
      : flattened;

  if (withCurrent.length === 0) return [];
  if (
    source.some(
      (node) =>
        Array.isArray((node as SubmissionRevisionNode).children) &&
        (node as SubmissionRevisionNode).children!.length > 0,
    )
  ) {
    return sortRevisionNodes(normalizeNestedNodes(source));
  }

  const nodes = withCurrent.map((node) => ({ ...node, children: [] as SubmissionRevisionNode[] }));
  const byId = new Map(nodes.map((node) => [String(node.id), node]));
  const roots: SubmissionRevisionNode[] = [];
  let hasExplicitParent = false;

  for (const node of nodes) {
    const parentId = node.parentSubmissionId;
    const parent = parentId == null ? null : byId.get(String(parentId));
    if (parent) {
      hasExplicitParent = true;
      parent.children = [...(parent.children ?? []), node];
    } else {
      roots.push(node);
    }
  }

  if (hasExplicitParent) return sortRevisionNodes(roots);

  const ordered = sortRevisionNodes(nodes);
  for (let i = 0; i < ordered.length - 1; i++) {
    ordered[i].children = [ordered[i + 1]];
  }
  return ordered.length > 0 ? [ordered[0]] : [];
}

function normalizeNestedNodes(nodes: (SubmissionRevision | SubmissionRevisionNode)[]): SubmissionRevisionNode[] {
  return nodes.map((node) => ({
    ...node,
    children: normalizeNestedNodes((node as SubmissionRevisionNode).children ?? []),
  }));
}

function flattenRevisionNodes(nodes: (SubmissionRevision | SubmissionRevisionNode)[]): SubmissionRevision[] {
  return nodes.flatMap((node) => [node, ...flattenRevisionNodes((node as SubmissionRevisionNode).children ?? [])]);
}

function detailToRevision(detail: SubmissionDetail): SubmissionRevisionNode {
  return {
    id: detail.id,
    parentSubmissionId: detail.parentSubmissionId,
    revisionNumber: detail.revisionNumber ?? 1,
    status: detail.status,
    submittedAt: detail.submittedAt,
    deliverableType: detail.deliverableType,
    fileUrl: detail.fileUrl,
    description: detail.fileName ?? null,
    children: [],
  };
}

function sortRevisionNodes(nodes: SubmissionRevisionNode[]): SubmissionRevisionNode[] {
  return [...nodes].sort((a, b) => {
    const d = (a.revisionNumber ?? 0) - (b.revisionNumber ?? 0);
    if (d !== 0) return d;
    return new Date(a.submittedAt).getTime() - new Date(b.submittedAt).getTime();
  });
}

function countRevisionNodes(nodes: SubmissionRevisionNode[]): number {
  return nodes.reduce((total, node) => total + 1 + countRevisionNodes(node.children ?? []), 0);
}

function sameId(a: SubmissionId, b: SubmissionId) {
  return String(a) === String(b);
}

function formatDateTime(iso: string) {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return "-";
  return date.toLocaleString("en-US", {
    day: "numeric",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function formatGrade(value?: number | null) {
  return typeof value === "number" ? value.toFixed(1) : "-";
}

function valueOrDash(value?: SubmissionId | null) {
  return value == null ? "-" : String(value);
}

function reviewDecisionLabel(value: string) {
  const labels: Partial<Record<SubmissionStatus, string>> = {
    APPROVED: "Approved",
    REVISION_REQUESTED: "Revision Requested",
    UNDER_REVIEW: "Under Review",
    GRADED: "Graded",
    PENDING_REVIEW: "Pending Review",
  };
  return labels[value as SubmissionStatus] ?? value;
}
