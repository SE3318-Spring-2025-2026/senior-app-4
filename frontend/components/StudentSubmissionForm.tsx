"use client";

import { useRef, useState } from "react";
import { toast } from "sonner";
import { getToken } from "@/lib/auth";
import { createRevisionSubmission, type SubmissionReview } from "@/lib/submissions-api";

type DeliverableType = "PROPOSAL" | "STATEMENT_OF_WORK";
type UploadStatus = "idle" | "dragging" | "uploading" | "success" | "error";

const deliverableOptions: { value: DeliverableType; label: string; hint: string }[] = [
  {
    value: "PROPOSAL",
    label: "Proposal",
    hint: "Initial project proposal document for committee review.",
  },
  {
    value: "STATEMENT_OF_WORK",
    label: "Statement of Work (SoW)",
    hint: "Formal scope, timeline, and deliverables after proposal approval.",
  },
];

const acceptedExtensions = [".pdf", ".doc", ".docx"];

interface SubmissionResponse {
  message?: string;
  data?: {
    id?: string;
    status?: string;
    submittedAt?: string;
  };
}

interface StudentSubmissionFormProps {
  groupId: string;
  groupName: string;
  disabled?: boolean;
  disabledReason?: string;
  mode?: "new" | "revision";
  parentSubmissionId?: string;
  reviewerComments?: SubmissionReview[];
  commentsLoading?: boolean;
  commentsError?: string;
}

export default function StudentSubmissionForm({
  groupId,
  groupName,
  disabled = false,
  disabledReason,
  mode = "new",
  parentSubmissionId,
  reviewerComments = [],
  commentsLoading = false,
  commentsError,
}: StudentSubmissionFormProps) {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [status, setStatus] = useState<UploadStatus>("idle");
  const [deliverableType, setDeliverableType] = useState<DeliverableType | "">("");
  const [revisionDescription, setRevisionDescription] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [lastSubmission, setLastSubmission] = useState<SubmissionResponse["data"] | null>(null);
  const isRevisionMode = mode === "revision";

  const validateAndSetFile = (selectedFile: File) => {
    const lowerName = selectedFile.name.toLowerCase();
    const hasAllowedExtension = acceptedExtensions.some((extension) => lowerName.endsWith(extension));

    if (!hasAllowedExtension) {
      toast.error("Only PDF, DOC, or DOCX files are accepted.");
      return;
    }

    setFile(selectedFile);
    setStatus("idle");
    setLastSubmission(null);
  };

  const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFile = event.target.files?.[0];
    if (selectedFile) {
      validateAndSetFile(selectedFile);
    }
  };

  const handleDragOver = (event: React.DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    if (!disabled && status !== "uploading") {
      setStatus("dragging");
    }
  };

  const handleDragLeave = () => {
    if (status !== "uploading") {
      setStatus("idle");
    }
  };

  const handleDrop = (event: React.DragEvent<HTMLDivElement>) => {
    event.preventDefault();

    if (disabled || status === "uploading") {
      return;
    }

    setStatus("idle");

    const droppedFile = event.dataTransfer.files?.[0];
    if (droppedFile) {
      validateAndSetFile(droppedFile);
    }
  };

  const handleReset = () => {
    setFile(null);
    setDeliverableType("");
    setRevisionDescription("");
    setStatus("idle");
    setLastSubmission(null);

    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  };

  const handleSubmit = async () => {
    if (disabled) {
      toast.error(disabledReason ?? "You cannot submit from this account.");
      return;
    }

    if (isRevisionMode && !parentSubmissionId) {
      toast.error("Revision context is missing. Open the form from a revision request.");
      return;
    }

    if (!isRevisionMode && !deliverableType) {
      toast.error("Select a deliverable type before submitting.");
      return;
    }

    if (!file) {
      toast.error("Choose a file before submitting.");
      return;
    }

    if (!isRevisionMode && !process.env.NEXT_PUBLIC_API_URL) {
      toast.error("NEXT_PUBLIC_API_URL is not configured.");
      return;
    }

    const token = getToken();
    if (!token) {
      toast.error("Your session has expired. Please sign in again.");
      return;
    }

    setStatus("uploading");
    setLastSubmission(null);

    try {
      if (isRevisionMode) {
        const payload = await createRevisionSubmission(parentSubmissionId!, {
          file,
          description: revisionDescription,
        });

        setStatus("success");
        setLastSubmission(payload?.data ?? null);
        toast.success(payload?.message || "Revision submitted successfully.");

        if (fileInputRef.current) {
          fileInputRef.current.value = "";
        }

        setFile(null);
        setRevisionDescription("");
        return;
      }

      const formData = new FormData();
      formData.append("teamId", groupId);
      formData.append("deliverableType", deliverableType);
      formData.append("file", file);

      const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/submissions`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
        },
        body: formData,
      });

      const payload = (await response.json().catch(() => null)) as SubmissionResponse | null;

      if (!response.ok) {
        throw new Error(payload?.message || `Submission failed (${response.status}).`);
      }

      setStatus("success");
      setLastSubmission(payload?.data ?? null);
      toast.success(payload?.message || "Deliverable submitted successfully.");

      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }

      setFile(null);
      setDeliverableType("");
    } catch (error) {
      setStatus("error");
      toast.error(error instanceof Error ? error.message : "An unexpected error occurred.");
    }
  };

  return (
    <div className="space-y-6">
      <div className="rounded-2xl border border-white/8 bg-gray-900 overflow-hidden">
        <div className="border-b border-white/5 px-6 py-4 flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-blue-500/10 border border-blue-500/20 flex items-center justify-center">
            <svg className="h-4 w-4 text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={1.5}
                d="M13.5 16.5V6.75m0 0L9.75 10.5m3.75-3.75L17.25 10.5M21 16.5v1.125A2.625 2.625 0 0118.375 20.25H5.625A2.625 2.625 0 013 17.625V16.5"
              />
            </svg>
          </div>
          <div>
            <p className="text-sm font-semibold text-white">
              {isRevisionMode ? "Revision Submission" : "Deliverable Submission"}
            </p>
            <p className="text-xs text-gray-500">
              {isRevisionMode ? "Review comments first, then upload the revised document" : "Select a deliverable type and upload your file"}
            </p>
          </div>
        </div>

        <div className="grid gap-6 p-6 lg:grid-cols-[1.05fr_0.95fr]">
          <div className="space-y-5">
            <div className="rounded-xl border border-white/8 bg-white/4 px-4 py-3">
              <p className="text-xs text-gray-500">Selected group</p>
              <p className="mt-1 text-sm font-medium text-white">{groupName}</p>
            </div>

            {isRevisionMode && (
              <ReviewerCommentsPanel
                parentSubmissionId={parentSubmissionId}
                comments={reviewerComments}
                loading={commentsLoading}
                error={commentsError}
              />
            )}

            {!isRevisionMode && (
            <div className="space-y-2">
              <label htmlFor="deliverableType" className="text-sm font-medium text-gray-300">
                Deliverable type
              </label>
              <div className="rounded-xl border border-white/10 bg-white/5">
                <select
                  id="deliverableType"
                  value={deliverableType}
                  onChange={(event) => setDeliverableType(event.target.value as DeliverableType | "")}
                  disabled={disabled || status === "uploading"}
                  className="w-full rounded-xl border-0 bg-transparent px-4 py-3 text-sm text-white outline-none disabled:cursor-not-allowed disabled:opacity-60"
                >
                  <option value="" className="bg-gray-950 text-gray-400">
                    Select Proposal or SoW
                  </option>
                  {deliverableOptions.map((option) => (
                    <option key={option.value} value={option.value} className="bg-gray-950 text-white">
                      {option.label}
                    </option>
                  ))}
                </select>
              </div>
              <p className="text-xs text-gray-500">
                {deliverableType
                  ? deliverableOptions.find((option) => option.value === deliverableType)?.hint
                  : "Choose the document type that matches the current milestone."}
              </p>
            </div>
            )}

            {isRevisionMode && (
              <div className="space-y-2">
                <label htmlFor="revisionDescription" className="text-sm font-medium text-gray-300">
                  Revision notes
                </label>
                <textarea
                  id="revisionDescription"
                  value={revisionDescription}
                  onChange={(event) => setRevisionDescription(event.target.value)}
                  rows={4}
                  disabled={disabled || status === "uploading"}
                  placeholder="Summarize the changes made in this revised document..."
                  className="w-full rounded-xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-white placeholder-gray-600 outline-none transition focus:border-blue-500/50 focus:ring-2 focus:ring-blue-500/30 disabled:cursor-not-allowed disabled:opacity-60"
                />
              </div>
            )}

            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <label className="text-sm font-medium text-gray-300">Deliverable file</label>
                <span className="text-xs text-gray-500">Accepted: PDF, DOC, DOCX</span>
              </div>

              <div
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
                onDrop={handleDrop}
                onClick={() => {
                  if (!disabled && status !== "uploading") {
                    fileInputRef.current?.click();
                  }
                }}
                className={[
                  "rounded-2xl border-2 border-dashed px-6 py-12 text-center transition-all duration-200",
                  disabled || status === "uploading" ? "cursor-not-allowed opacity-70" : "cursor-pointer",
                  status === "dragging"
                    ? "border-blue-400 bg-blue-500/10"
                    : file
                      ? "border-blue-400/40 bg-blue-500/6"
                      : "border-white/10 bg-white/3 hover:border-blue-400/40 hover:bg-blue-500/5",
                ].join(" ")}
              >
                <input
                  ref={fileInputRef}
                  type="file"
                  accept={acceptedExtensions.join(",")}
                  className="hidden"
                  onChange={handleFileChange}
                />

                <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl border border-white/10 bg-white/5">
                  {file ? (
                    <svg className="h-6 w-6 text-blue-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12.75 11.25 15 15 9.75" />
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={1.5}
                        d="M21 12a9 9 0 11-18 0 9 9 0 0118 0Z"
                      />
                    </svg>
                  ) : (
                    <svg className="h-6 w-6 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={1.5}
                        d="M12 16.5V7.5m0 0-3 3m3-3 3 3M21 16.5v1.125A2.625 2.625 0 0118.375 20.25H5.625A2.625 2.625 0 013 17.625V16.5"
                      />
                    </svg>
                  )}
                </div>

                <div className="mt-5 space-y-1">
                  {file ? (
                    <>
                      <p className="text-sm font-semibold text-white">{file.name}</p>
                      <p className="text-xs text-gray-400">
                        {(file.size / 1024 / 1024).toFixed(2)} MB | Click or drop another file to replace it
                      </p>
                    </>
                  ) : (
                    <>
                      <p className="text-sm font-medium text-gray-200">
                        {status === "dragging" ? "Release to attach your file" : "Drag and drop your file here"}
                      </p>
                      <p className="text-xs text-gray-500">or click anywhere in this area to browse</p>
                    </>
                  )}
                </div>
              </div>
            </div>

            {disabledReason && (
              <div className="rounded-xl border border-amber-500/20 bg-amber-500/8 px-4 py-3 text-sm text-amber-100">
                {disabledReason}
              </div>
            )}

            <div className="flex flex-wrap items-center gap-3">
              <button
                type="button"
                onClick={handleSubmit}
                disabled={disabled || status === "uploading"}
                className={[
                  "inline-flex items-center gap-2 rounded-xl px-5 py-2.5 text-sm font-medium transition-all",
                  disabled || status === "uploading"
                    ? "cursor-not-allowed border border-white/5 bg-white/5 text-gray-500"
                    : "bg-blue-600 text-white shadow-lg shadow-blue-600/20 hover:bg-blue-500 active:scale-95",
                ].join(" ")}
              >
                {status === "uploading" ? (
                  <>
                    <svg className="h-4 w-4 animate-spin" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 0 1 8-8V0C5.373 0 0 5.373 0 12h4z" />
                    </svg>
                    Submitting...
                  </>
                ) : (
                  <>
                    <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={1.5}
                        d="M12 16.5V7.5m0 0-3 3m3-3 3 3M21 16.5v1.125A2.625 2.625 0 0118.375 20.25H5.625A2.625 2.625 0 013 17.625V16.5"
                      />
                    </svg>
                    {isRevisionMode ? "Submit revision" : "Submit deliverable"}
                  </>
                )}
              </button>

              {(file || deliverableType || revisionDescription) && status !== "uploading" && (
                <button
                  type="button"
                  onClick={handleReset}
                  className="rounded-xl border border-white/10 px-4 py-2.5 text-sm font-medium text-gray-400 transition-colors hover:bg-white/5 hover:text-white"
                >
                  Clear form
                </button>
              )}
            </div>
          </div>

          <div className="space-y-4">
            <div className="bg-gray-950/40 border border-white/8 rounded-2xl overflow-hidden">
              <div className="px-5 py-4 border-b border-white/5">
                <p className="text-sm font-semibold text-white">Submission Rules</p>
                <p className="text-xs text-gray-500 mt-1">
                  {isRevisionMode ? "This flow links the new upload to the requested revision." : "This page only covers the student-side upload flow."}
                </p>
              </div>
              <div className="p-5 space-y-4 text-sm text-gray-300">
                <div className="rounded-xl border border-white/8 bg-white/4 px-4 py-3">
                  <p className="text-xs text-gray-500">Deliverable types</p>
                  <p className="mt-1 text-white">
                    {isRevisionMode ? "Revision of requested deliverable" : "Proposal, Statement of Work"}
                  </p>
                </div>

                <div className="rounded-xl border border-white/8 bg-white/4 px-4 py-3">
                  <p className="text-xs text-gray-500">Submission target</p>
                  <p className="mt-1 font-mono text-blue-200">
                    {isRevisionMode ? "POST /submissions/{parentSubmissionId}/revisions" : "POST /submissions"}
                  </p>
                </div>

                <div className="rounded-xl border border-white/8 bg-white/4 px-4 py-3">
                  <p className="text-xs text-gray-500">Request payload</p>
                  <p className="mt-1">
                    {isRevisionMode ? (
                      <>
                        Multipart form with file and optional <span className="font-mono text-white">description</span>, linked to parent <span className="font-mono text-white">{parentSubmissionId ?? "unknown"}</span>.
                      </>
                    ) : (
                      <>
                        Multipart form with <span className="font-mono text-white">teamId</span>, <span className="font-mono text-white">deliverableType</span>, and file.
                      </>
                    )}
                  </p>
                </div>
              </div>
            </div>

            <div className="bg-gray-950/40 border border-white/8 rounded-2xl overflow-hidden">
              <div className="px-5 py-4 border-b border-white/5">
                <p className="text-sm font-semibold text-white">Validation Checklist</p>
              </div>

              <div className="p-5 space-y-3 text-sm text-gray-300">
                <div className="flex items-start gap-3">
                  <span className={`mt-1 h-2.5 w-2.5 rounded-full ${isRevisionMode ? (parentSubmissionId ? "bg-green-400" : "bg-gray-600") : (deliverableType ? "bg-green-400" : "bg-gray-600")}`} />
                  <span>{isRevisionMode ? "Parent submission is linked." : "Deliverable type is selected."}</span>
                </div>
                <div className="flex items-start gap-3">
                  <span className={`mt-1 h-2.5 w-2.5 rounded-full ${file ? "bg-green-400" : "bg-gray-600"}`} />
                  <span>A supported file is attached.</span>
                </div>
                <div className="flex items-start gap-3">
                  <span className={`mt-1 h-2.5 w-2.5 rounded-full ${!disabled ? "bg-green-400" : "bg-amber-400"}`} />
                  <span>{disabled ? "Submission is currently view-only." : "Submission can be sent."}</span>
                </div>
              </div>
            </div>

            {lastSubmission && (
              <div className="bg-gray-900 border border-green-500/20 rounded-2xl overflow-hidden">
                <div className="px-5 py-4 border-b border-green-500/10 flex items-center gap-3">
                  <div className="w-2 h-2 rounded-full bg-green-400" />
                  <p className="text-sm font-semibold text-white">Last Submission</p>
                </div>
                <div className="p-5 space-y-2 text-sm text-gray-300">
                  {lastSubmission.id && <p>Submission ID: <span className="font-mono text-white">{lastSubmission.id}</span></p>}
                  {lastSubmission.status && <p>Status: <span className="text-white">{lastSubmission.status}</span></p>}
                  {lastSubmission.submittedAt && <p>Submitted at: <span className="text-white">{new Date(lastSubmission.submittedAt).toLocaleString("en-US")}</span></p>}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

function ReviewerCommentsPanel({
  parentSubmissionId,
  comments,
  loading,
  error,
}: {
  parentSubmissionId?: string;
  comments: SubmissionReview[];
  loading: boolean;
  error?: string;
}) {
  return (
    <div className="rounded-2xl border border-orange-500/20 bg-orange-500/8 overflow-hidden">
      <div className="border-b border-orange-500/10 px-5 py-4">
        <p className="text-sm font-semibold text-white">Reviewer Comments</p>
        <p className="mt-1 text-xs text-orange-100/70">
          Parent submission: <span className="font-mono text-orange-50">{parentSubmissionId ?? "not linked"}</span>
        </p>
      </div>

      <div className="space-y-3 p-5">
        {loading && (
          <div className="flex items-center gap-3 rounded-xl border border-white/8 bg-white/4 px-4 py-3 text-sm text-gray-300">
            <svg className="h-4 w-4 animate-spin text-orange-300" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 0 1 8-8V0C5.373 0 0 5.373 0 12h4z" />
            </svg>
            Loading reviewer comments...
          </div>
        )}

        {!loading && error && (
          <div className="rounded-xl border border-amber-500/20 bg-amber-500/10 px-4 py-3 text-sm text-amber-100">
            {error}
          </div>
        )}

        {!loading && !error && comments.length === 0 && (
          <div className="rounded-xl border border-white/8 bg-white/4 px-4 py-3 text-sm text-gray-300">
            No reviewer comments are available yet.
          </div>
        )}

        {!loading && comments.map((comment) => (
          <article key={comment.id} className="rounded-xl border border-white/8 bg-gray-950/40 px-4 py-3">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <p className="text-sm font-medium text-white">{comment.reviewerName || "Committee reviewer"}</p>
              <span className="rounded-full border border-orange-400/20 bg-orange-400/10 px-2.5 py-1 text-xs font-medium text-orange-200">
                {comment.status === "REVISION_REQUESTED" ? "Revision requested" : "Approved"}
              </span>
            </div>
            <p className="mt-2 whitespace-pre-wrap text-sm leading-6 text-gray-300">{comment.comments}</p>
            <p className="mt-3 text-xs text-gray-500">{formatCommentDate(comment.reviewedAt)}</p>
          </article>
        ))}
      </div>
    </div>
  );
}

function formatCommentDate(value: string) {
  if (!value) return "Date unavailable";
  return new Date(value).toLocaleString("en-US", {
    day: "numeric",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}
