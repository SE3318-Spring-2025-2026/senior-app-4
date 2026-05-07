"use client";

import { useEffect, useMemo, useState } from "react";
import { toast } from "sonner";
import { getToken } from "@/lib/auth";
import {
  fetchCriteriaForDeliverableType,
  type GradingCriteriaItem,
  type SubmissionSummary,
} from "@/lib/submissions-api";

type Props = {
  open: boolean;
  submission: SubmissionSummary | null;
  onClose: () => void;
  onSubmitted?: (submissionId: number) => void;
};

const API = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api/v1";
const blockedGradeKeys = ["e", "E", "+", "-"];

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

function formatDate(value: string) {
  return new Date(value).toLocaleString("en-US", {
    day: "numeric",
    month: "long",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function getStatusTone(status: SubmissionSummary["status"]) {
  switch (status) {
    case "APPROVED":
      return "bg-green-500/10 border border-green-500/20 text-green-300";
    case "UNDER_REVIEW":
      return "bg-amber-500/10 border border-amber-500/20 text-amber-300";
    case "GRADED":
      return "bg-blue-500/10 border border-blue-500/20 text-blue-300";
    case "REVISION_REQUESTED":
      return "bg-red-500/10 border border-red-500/20 text-red-300";
    default:
      return "bg-white/5 border border-white/10 text-gray-300";
  }
}

async function parseSubmissionResponse(response: Response) {
  const raw = await response.text();
  if (!raw) return null;
  try {
    return JSON.parse(raw) as { message?: string; error?: string; score?: number };
  } catch {
    return raw;
  }
}

export default function CommitteeGradingDrawer({
  open,
  submission,
  onClose,
  onSubmitted,
}: Props) {
  const [criteria, setCriteria] = useState<GradingCriteriaItem[]>([]);
  const [criteriaLoading, setCriteriaLoading] = useState(false);
  // criterion-based inputs: criterionId → letter (SOFT) or "100"/"0" (BINARY)
  const [criterionInputs, setCriterionInputs] = useState<Record<number, string>>({});
  // flat-score fallback
  const [gradeInput, setGradeInput] = useState("");
  const [comments, setComments] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!open || !submission) {
      setCriteria([]);
      setCriterionInputs({});
      setGradeInput("");
      setComments("");
      setSubmitting(false);
      return;
    }

    let cancelled = false;
    setCriteriaLoading(true);

    fetchCriteriaForDeliverableType(submission.deliverableType).then((data) => {
      if (cancelled) return;
      setCriteria(data);
      const init: Record<number, string> = {};
      data.forEach((c) => { init[c.id] = ""; });
      setCriterionInputs(init);
    }).finally(() => {
      if (!cancelled) setCriteriaLoading(false);
    });

    return () => { cancelled = true; };
  }, [open, submission]);

  const hasCriteria = criteria.length > 0;

  const weightedPreview = useMemo(() => {
    if (!hasCriteria) return null;
    let totalWeight = 0;
    let weightedSum = 0;
    for (const c of criteria) {
      const val = criterionInputs[c.id];
      if (!val) return null;
      const score = c.gradingType === "SOFT" ? letterToScore(val) : Number(val);
      totalWeight += c.weight;
      weightedSum += score * c.weight;
    }
    if (totalWeight === 0) return null;
    return (weightedSum / totalWeight).toFixed(1);
  }, [criteria, criterionInputs, hasCriteria]);

  const canSubmit = hasCriteria
    ? criteria.every((c) => Boolean(criterionInputs[c.id]))
    : gradeInput !== "";

  if (!open || !submission) return null;

  const handleGradeChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const nextValue = event.target.value;
    if (nextValue === "") { setGradeInput(""); return; }
    const parsed = Number(nextValue);
    if (Number.isNaN(parsed) || parsed < 0 || parsed > 100) return;
    setGradeInput(nextValue);
  };

  const handleKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
    if (blockedGradeKeys.includes(event.key)) event.preventDefault();
  };

  const handleOverlayClose = () => {
    if (!submitting) onClose();
  };

  const handleSubmit = async () => {
    const token = getToken();
    if (!token) {
      toast.error("Your session has expired. Please sign in again.");
      return;
    }

    setSubmitting(true);
    try {
      let body: object;
      if (hasCriteria) {
        const criterionScores = criteria.map((c) => ({
          criterionId: c.id,
          score: c.gradingType === "SOFT" ? letterToScore(criterionInputs[c.id]) : Number(criterionInputs[c.id]),
        }));
        body = { feedback: comments.trim() || undefined, criterionScores };
      } else {
        body = { grade: Number(gradeInput), feedback: comments.trim() || undefined };
      }

      const response = await fetch(`${API}/submissions/${submission.id}/grades`, {
        method: "POST",
        headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
        body: JSON.stringify(body),
      });

      const payload = await parseSubmissionResponse(response);

      if (!response.ok) {
        if (typeof payload === "string") throw new Error(payload);
        const err = payload as { message?: string; error?: string } | null;
        throw new Error(err?.message || err?.error || `Grade submission failed (${response.status}).`);
      }

      const score = hasCriteria ? weightedPreview : gradeInput;
      toast.success(`Grade ${score} submitted successfully.`);
      onSubmitted?.(submission.id);
      onClose();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Failed to submit the grade.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex justify-end bg-black/60">
      <button
        type="button"
        aria-label="Close grading drawer"
        onClick={handleOverlayClose}
        className="flex-1 cursor-default"
      />

      <aside className="relative h-full w-full max-w-xl border-l border-white/10 bg-gray-950 shadow-2xl shadow-black/50">
        <div className="flex h-full flex-col">
          {/* Header */}
          <div className="border-b border-white/5 px-6 py-5">
            <div className="flex items-start justify-between gap-4">
              <div>
                <p className="text-xs font-medium uppercase tracking-[0.2em] text-blue-300/80">
                  Committee Grading
                </p>
                <h2 className="mt-2 text-2xl font-semibold text-white">
                  Grade {submission.teamName ?? `Team #${submission.teamId}`}
                </h2>
                <p className="mt-2 text-sm text-gray-400">
                  Review the deliverable metadata and submit a committee score.
                </p>
              </div>
              <button
                type="button"
                onClick={handleOverlayClose}
                disabled={submitting}
                className="rounded-xl border border-white/10 bg-white/5 px-3 py-2 text-sm text-gray-300 transition hover:bg-white/10 disabled:opacity-50"
              >
                Close
              </button>
            </div>
          </div>

          {/* Body */}
          <div className="flex-1 overflow-y-auto px-6 py-6">
            <div className="space-y-6">
              {/* Submission metadata */}
              <div className="rounded-2xl border border-white/10 bg-gray-900/80 p-5">
                <div className="flex flex-wrap items-center gap-3">
                  <span className={["rounded-full px-3 py-1 text-xs font-medium", getStatusTone(submission.status)].join(" ")}>
                    {submission.status}
                  </span>
                  <span className="rounded-full border border-white/10 bg-white/5 px-3 py-1 text-xs font-medium text-gray-300">
                    {submission.deliverableType}
                  </span>
                </div>
                <div className="mt-5 grid gap-4 sm:grid-cols-2">
                  <InfoBlock label="Submission ID" value={`#${submission.id}`} />
                  <InfoBlock
                    label="Committee ID"
                    value={submission.assignedCommitteeId == null ? "Not assigned" : `#${submission.assignedCommitteeId}`}
                  />
                  <InfoBlock label="Submitted at" value={formatDate(submission.submittedAt)} />
                  {submission.deadline && (
                    <InfoBlock label="Deadline" value={formatDate(submission.deadline)} />
                  )}
                  {submission.revisionNumber != null && (
                    <InfoBlock label="Revision" value={`v${submission.revisionNumber}`} />
                  )}
                </div>
              </div>

              {/* Grading form */}
              <div className="rounded-2xl border border-white/10 bg-gray-900/80 p-5 space-y-4">
                {criteriaLoading ? (
                  <div className="flex items-center justify-center py-6">
                    <svg className="h-5 w-5 animate-spin text-blue-500" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                    </svg>
                  </div>
                ) : hasCriteria ? (
                  <>
                    <p className="text-xs font-medium text-gray-400 uppercase tracking-wider">Evaluation Rubric</p>
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
                                disabled={submitting}
                                className={`px-4 py-2 rounded-lg text-sm font-medium transition border disabled:opacity-50 ${
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
                                disabled={submitting}
                                className={`px-3 py-2 rounded-lg text-sm font-medium transition border disabled:opacity-50 ${
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
                  </>
                ) : (
                  <div className="space-y-2">
                    <label htmlFor="gradingScore" className="text-sm font-medium text-gray-300">
                      Final grade
                    </label>
                    <input
                      id="gradingScore"
                      type="number"
                      min="0"
                      max="100"
                      step="0.1"
                      inputMode="decimal"
                      value={gradeInput}
                      onChange={handleGradeChange}
                      onKeyDown={handleKeyDown}
                      placeholder="0 - 100"
                      disabled={submitting}
                      className="w-full rounded-xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-white placeholder-gray-600 outline-none transition focus:border-blue-500/50 focus:ring-2 focus:ring-blue-500/30 disabled:opacity-60"
                    />
                    <p className="text-xs text-gray-500">
                      No grading criteria defined for this deliverable type — enter a direct score.
                    </p>
                  </div>
                )}

                {/* Comments */}
                <div className="space-y-2">
                  <label htmlFor="gradingComments" className="text-sm font-medium text-gray-300">
                    Comments
                  </label>
                  <textarea
                    id="gradingComments"
                    value={comments}
                    onChange={(event) => setComments(event.target.value)}
                    rows={4}
                    placeholder="Leave committee feedback for this submission..."
                    disabled={submitting}
                    className="w-full rounded-xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-white placeholder-gray-600 outline-none transition focus:border-blue-500/50 focus:ring-2 focus:ring-blue-500/30 disabled:opacity-60"
                  />
                </div>
              </div>
            </div>
          </div>

          {/* Footer */}
          <div className="border-t border-white/5 px-6 py-5">
            <div className="flex items-center justify-end gap-3">
              <button
                type="button"
                onClick={handleOverlayClose}
                disabled={submitting}
                className="rounded-xl border border-white/10 bg-white/5 px-4 py-2.5 text-sm text-gray-300 transition hover:bg-white/10 disabled:opacity-50"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleSubmit}
                disabled={submitting || !canSubmit}
                className="inline-flex items-center gap-2 rounded-xl bg-blue-600 px-5 py-2.5 text-sm font-medium text-white transition hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {submitting ? (
                  <>
                    <svg className="h-4 w-4 animate-spin" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                    </svg>
                    Submitting...
                  </>
                ) : (
                  "Submit grade"
                )}
              </button>
            </div>
          </div>
        </div>
      </aside>
    </div>
  );
}

function InfoBlock({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl border border-white/8 bg-white/4 px-4 py-3">
      <p className="text-xs text-gray-500">{label}</p>
      <p className="mt-1 text-sm font-medium text-white">{value}</p>
    </div>
  );
}
