"use client";

import { useEffect, useState } from "react";
import { toast } from "sonner";
import { getToken } from "@/lib/auth";
import { fetchGradingCriteria, type GradingCriterion, type SubmissionSummary } from "@/lib/submissions-api";

type Props = {
  open: boolean;
  submission: SubmissionSummary | null;
  onClose: () => void;
  onSubmitted?: (submissionId: number) => void;
};

const API = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api/v1";
const blockedGradeKeys = ["e", "E", "+", "-"];

type GradeSubmissionResponse = {
  gradeId: number;
  submissionId: number;
  reviewerId: number;
  score: number;
  comments: string | null;
  gradedAt: string;
  calculatedAverage: number | null;
  isGradingComplete: boolean;
};

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

  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw) as GradeSubmissionResponse | { message?: string; error?: string };
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
  const [gradeInput, setGradeInput] = useState("");
  const [comments, setComments] = useState("");
  const [criteria, setCriteria] = useState<GradingCriterion[]>([]);
  const [criteriaGrades, setCriteriaGrades] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!open) {
      setGradeInput("");
      setComments("");
      setCriteria([]);
      setCriteriaGrades({});
      setSubmitting(false);
    }
  }, [open]);

  useEffect(() => {
    if (!open || !submission) return;
    const deliverableType = submission.deliverableType === "REVISED_PROPOSAL" ? "PROPOSAL" : submission.deliverableType;
    fetchGradingCriteria(deliverableType)
      .then((items) => {
        setCriteria(items);
        setCriteriaGrades(Object.fromEntries(items.map((item) => [String(item.id), ""])));
      })
      .catch(() => {
        setCriteria([]);
        setCriteriaGrades({});
      });
  }, [open, submission]);

  if (!open || !submission) {
    return null;
  }

  const handleGradeChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const nextValue = event.target.value;

    if (nextValue === "") {
      setGradeInput("");
      return;
    }

    const parsed = Number(nextValue);
    if (Number.isNaN(parsed) || parsed < 0 || parsed > 100) {
      return;
    }

    setGradeInput(nextValue);
  };

  const handleKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
    if (blockedGradeKeys.includes(event.key)) {
      event.preventDefault();
    }
  };

  const handleOverlayClose = () => {
    if (!submitting) {
      onClose();
    }
  };

  const handleSubmit = async () => {
    if (criteria.length > 0 && criteria.some((criterion) => !criteriaGrades[String(criterion.id)])) {
      toast.error("Select a grade for every criterion.");
      return;
    }

    if (criteria.length === 0 && !gradeInput) {
      toast.error("Enter a grade between 0 and 100.");
      return;
    }

    const token = getToken();
    if (!token) {
      toast.error("Your session has expired. Please sign in again.");
      return;
    }

    setSubmitting(true);

    try {
      const response = await fetch(`${API}/submissions/${submission.id}/grades`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          ...(criteria.length > 0
            ? {
                criteriaScores: criteria.map((criterion) => ({
                  criterionId: criterion.id,
                  grade: criteriaGrades[String(criterion.id)],
                })),
              }
            : { grade: Number(gradeInput) }),
          feedback: comments.trim() || undefined,
        }),
      });

      const payload = await parseSubmissionResponse(response);

      if (!response.ok) {
        if (typeof payload === "string") {
          throw new Error(payload);
        }

        const errorPayload = payload as { message?: string; error?: string } | null;
        throw new Error(errorPayload?.message || errorPayload?.error || `Grade submission failed (${response.status}).`);
      }

      const successMessage =
        typeof payload === "object" && payload && "score" in payload
          ? `Grade ${payload.score} submitted successfully.`
          : "Grade submitted successfully.";

      toast.success(successMessage);
      // TODO(#74): refresh graded submission summaries from real backend data after
      // professor submission listing/detail endpoints are wired.
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
                  Review the deliverable metadata, download the file, and submit a final grade.
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

          <div className="flex-1 overflow-y-auto px-6 py-6">
            <div className="space-y-6">
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

              <div className="rounded-2xl border border-white/10 bg-gray-900/80 p-5">
                <div className="space-y-2">
                  <label htmlFor="gradingComments" className="text-sm font-medium text-gray-300">
                    Comments
                  </label>
                  <textarea
                    id="gradingComments"
                    value={comments}
                    onChange={(event) => setComments(event.target.value)}
                    rows={6}
                    placeholder="Leave committee feedback for this submission..."
                    disabled={submitting}
                    className="w-full rounded-xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-white placeholder-gray-600 outline-none transition focus:border-blue-500/50 focus:ring-2 focus:ring-blue-500/30 disabled:opacity-60"
                  />
                  <p className="text-xs text-gray-500">
                    Saved as written feedback alongside the grade. You can leave this blank if the backend allows it.
                  </p>
                </div>

                {criteria.length > 0 ? (
                  <div className="mt-5 space-y-3">
                    {criteria.map((criterion) => (
                      <DrawerCriterionPicker
                        key={criterion.id}
                        criterion={criterion}
                        value={criteriaGrades[String(criterion.id)] ?? ""}
                        onChange={(value) => setCriteriaGrades((prev) => ({ ...prev, [String(criterion.id)]: value }))}
                      />
                    ))}
                  </div>
                ) : (
                  <div className="mt-5 space-y-2">
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
                      Only numeric values from 0 to 100 are accepted.
                    </p>
                  </div>
                )}
              </div>
            </div>
          </div>

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
                disabled={submitting}
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

function DrawerCriterionPicker({
  criterion,
  value,
  onChange,
}: {
  criterion: GradingCriterion;
  value: string;
  onChange: (value: string) => void;
}) {
  const options = criterion.gradingType === "BINARY" ? ["S", "F"] : ["A", "B", "C", "D", "F"];
  return (
    <div className="rounded-xl border border-white/10 bg-white/[0.03] p-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-sm font-medium text-white">{criterion.name}</p>
          {criterion.description && <p className="mt-1 text-xs text-gray-500">{criterion.description}</p>}
        </div>
        <span className="text-xs font-semibold text-blue-300">{criterion.weight}%</span>
      </div>
      <div className="mt-3 flex flex-wrap gap-2">
        {options.map((option) => (
          <button
            key={option}
            type="button"
            onClick={() => onChange(option)}
            className={`flex h-8 min-w-8 items-center justify-center rounded-lg border px-2 text-xs font-semibold transition ${
              value === option
                ? "border-blue-400/40 bg-blue-500 text-white"
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

function InfoBlock({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl border border-white/8 bg-white/4 px-4 py-3">
      <p className="text-xs text-gray-500">{label}</p>
      <p className="mt-1 text-sm font-medium text-white">{value}</p>
    </div>
  );
}
