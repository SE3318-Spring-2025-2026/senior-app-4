import { getToken } from "@/lib/auth";

const API_BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api/v1";

export type DeliverableType = "PROPOSAL" | "REVISED_PROPOSAL" | "STATEMENT_OF_WORK";
export type SubmissionStatus = "PENDING_REVIEW" | "UNDER_REVIEW" | "REVISION_REQUESTED" | "APPROVED" | "GRADED";
export type DeadlineStatus = "APPROACHING" | "OVERDUE";

export interface SubmissionSummary {
  id: string;
  teamId: string;
  teamName: string;
  deliverableType: DeliverableType;
  status: SubmissionStatus;
  assignedCommitteeId: string | null;
  revisionNumber: number;
  submittedAt: string;
  deadline: string | null;
  isOverdue: boolean;
}

export interface SubmissionReview {
  id: string;
  reviewerId: string;
  reviewerName: string;
  comments: string;
  status: "APPROVED" | "REVISION_REQUESTED";
  reviewedAt: string;
}

export interface ReviewListResponse {
  status: string;
  data: SubmissionReview[];
}

export interface RevisionCreateResponse {
  status: string;
  message?: string;
  data?: {
    id?: string;
    parentSubmissionId?: string;
    revisionNumber?: number;
    status?: SubmissionStatus;
    submittedAt?: string;
  };
}

export interface PaginationMeta {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface SubmissionsListResponse {
  status: string;
  data: SubmissionSummary[];
  pagination: PaginationMeta;
}

export interface SubmissionFilters {
  teamId?: string;
  status?: SubmissionStatus | "";
  committeeId?: string;
  deliverableType?: DeliverableType | "";
  deadlineStatus?: DeadlineStatus | "";
  page?: number;
  size?: number;
}

function buildHeaders(): Record<string, string> {
  const h: Record<string, string> = { "Content-Type": "application/json" };
  const token = getToken();
  if (token) h["Authorization"] = `Bearer ${token}`;
  return h;
}

async function parseError(res: Response): Promise<string> {
  try {
    const data = await res.json();
    return data?.message || data?.error || `Request failed (${res.status})`;
  } catch {
    return `Request failed (${res.status})`;
  }
}

export async function fetchSubmissions(filters: SubmissionFilters = {}): Promise<SubmissionsListResponse> {
  const params = new URLSearchParams();
  if (filters.teamId) params.append("teamId", filters.teamId);
  if (filters.status) params.append("status", filters.status);
  if (filters.committeeId) params.append("committeeId", filters.committeeId);
  if (filters.deliverableType) params.append("deliverableType", filters.deliverableType);
  if (filters.deadlineStatus) params.append("deadlineStatus", filters.deadlineStatus);
  params.append("page", String(filters.page ?? 0));
  params.append("size", String(filters.size ?? 20));

  const res = await fetch(`${API_BASE}/submissions?${params.toString()}`, {
    headers: buildHeaders(),
  });

  if (!res.ok) {
    const msg = await parseError(res);
    throw new Error(msg);
  }

  return res.json();
}

export async function fetchSubmissionReviews(submissionId: string): Promise<ReviewListResponse> {
  const controller = new AbortController();
  const timeoutId = window.setTimeout(() => controller.abort(), 4000);

  try {
    const res = await fetch(`${API_BASE}/submissions/${submissionId}/reviews`, {
      headers: buildHeaders(),
      signal: controller.signal,
    });

    if (!res.ok) {
      const msg = await parseError(res);
      throw new Error(msg);
    }

    return res.json();
  } finally {
    window.clearTimeout(timeoutId);
  }
}

export async function createRevisionSubmission(
  parentSubmissionId: string,
  payload: { file: File; description?: string },
): Promise<RevisionCreateResponse> {
  const token = getToken();
  const formData = new FormData();
  formData.append("file", payload.file);
  if (payload.description?.trim()) {
    formData.append("description", payload.description.trim());
  }

  const headers: Record<string, string> = {};
  if (token) headers.Authorization = `Bearer ${token}`;

  // TODO(#156): keep this endpoint aligned with the backend once Process 3
  // revision upload support is implemented end-to-end.
  const res = await fetch(`${API_BASE}/submissions/${parentSubmissionId}/revisions`, {
    method: "POST",
    headers,
    body: formData,
  });

  if (!res.ok) {
    const msg = await parseError(res);
    throw new Error(msg);
  }

  return res.json();
}
