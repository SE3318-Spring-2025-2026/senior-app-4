import { API_BASE, buildHeaders, parseError } from "@/lib/api-utils";

// ── Config Types ───────────────────────────────────────────────────────────

export interface ValidationConfigData {
  reviewWeight: number;
  implementationWeight: number;
  openaiModel: string;
  maxDiffLines: number;
  excludedFilePatterns: string[];
}

export interface ValidationConfigResponse {
  status: string;
  data: ValidationConfigData;
}

export interface ValidationConfigRequest {
  reviewWeight: number;
  implementationWeight: number;
  openaiModel?: string;
  maxDiffLines?: number;
  excludedFilePatterns?: string[];
}

// ── Trigger & Job Types ────────────────────────────────────────────────────

export interface TriggerValidationRequest {
  teamId?: string;
  issueKeys?: string[];
}

export interface TriggerValidationData {
  jobId: number;
  sprintId: number;
  teamId: number | null;
  issueCount: number;
  status: string;
  createdAt: string;
}

export interface TriggerValidationResponse {
  status: string;
  message: string;
  data: TriggerValidationData;
}

export type JobStatus =
  | "QUEUED"
  | "IN_PROGRESS"
  | "COMPLETED"
  | "PARTIALLY_COMPLETED"
  | "FAILED";

export type JobCurrentStep =
  | "LOADING_CONTEXT"
  | "FETCHING_PR_DETAILS"
  | "FETCHING_DIFFS"
  | "AI_REVIEW_VERIFICATION"
  | "AI_IMPLEMENTATION_VALIDATION"
  | "STORING_RESULTS"
  | null;

export interface JobStatusData {
  jobId: number;
  sprintId: number;
  jobStatus: JobStatus;
  currentStep: JobCurrentStep;
  progressPercentage: number;
  message: string;
  issuesTotal: number;
  issuesCompleted: number;
  issuesFailed: number;
  failureReason: string | null;
  startedAt: string;
  completedAt: string | null;
}

export interface JobStatusResponse {
  status: string;
  data: JobStatusData;
}

// ── API — Config ───────────────────────────────────────────────────────────

export async function fetchValidationConfig(): Promise<ValidationConfigResponse> {
  const res = await fetch(`${API_BASE}/ai-validation/config`, {
    headers: buildHeaders(),
  });
  if (!res.ok) throw new Error(await parseError(res));
  return res.json();
}

export async function updateValidationConfig(
  payload: ValidationConfigRequest
): Promise<ValidationConfigResponse> {
  const res = await fetch(`${API_BASE}/ai-validation/config`, {
    method: "PUT",
    headers: buildHeaders(),
    body: JSON.stringify(payload),
  });
  if (!res.ok) throw new Error(await parseError(res));
  return res.json();
}

// ── API — Trigger & Jobs ───────────────────────────────────────────────────

/** POST /ai-validation/sprints/{sprintId}/trigger → 202 */
export async function triggerValidation(
  sprintId: string,
  body?: TriggerValidationRequest
): Promise<TriggerValidationResponse> {
  const res = await fetch(`${API_BASE}/ai-validation/sprints/${sprintId}/trigger`, {
    method: "POST",
    headers: buildHeaders(),
    body: body ? JSON.stringify(body) : undefined,
  });
  if (!res.ok) {
    const data = await res.json().catch(() => ({})) as { message?: string; error?: string; existingJobId?: number };
    throw Object.assign(new Error(data.message || "Failed to trigger validation."), {
      errorCode: data.error,
      existingJobId: data.existingJobId ?? null,
    });
  }
  return res.json();
}

/** GET /ai-validation/jobs/{jobId} */
export async function getJobStatus(jobId: number): Promise<JobStatusResponse> {
  const res = await fetch(`${API_BASE}/ai-validation/jobs/${jobId}`, {
    headers: buildHeaders(),
  });
  if (!res.ok) throw new Error(await parseError(res));
  return res.json();
}

/** POST /ai-validation/jobs/{jobId}/retry → 202 */
export async function retryJob(jobId: number): Promise<TriggerValidationResponse> {
  const res = await fetch(`${API_BASE}/ai-validation/jobs/${jobId}/retry`, {
    method: "POST",
    headers: buildHeaders(),
  });
  if (!res.ok) throw new Error(await parseError(res));
  return res.json();
}

/**
 * GET /ai-validation/sprints/{sprintId}/active-job
 * Returns null on 204 (no active job).
 * Throws with .httpStatus on 403/404.
 */
export async function getActiveJobForSprint(
  sprintId: string
): Promise<JobStatusResponse | null> {
  const res = await fetch(`${API_BASE}/ai-validation/sprints/${sprintId}/active-job`, {
    headers: buildHeaders(),
  });
  if (res.status === 204) return null;
  if (!res.ok) {
    const data = await res.json().catch(() => ({})) as { message?: string; error?: string };
    throw Object.assign(new Error(data.message || "Failed to fetch active job."), {
      errorCode: data.error,
      httpStatus: res.status,
    });
  }
  return res.json();
}
