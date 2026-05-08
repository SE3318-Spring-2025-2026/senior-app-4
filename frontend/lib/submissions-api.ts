import { getToken } from "@/lib/auth";
import { API_BASE, buildHeaders, parseError } from "@/lib/api-utils";

export type DeliverableType = "PROPOSAL" | "REVISED_PROPOSAL" | "STATEMENT_OF_WORK" | "DEMONSTRATION";
export type SubmissionStatus =
  | "SUBMITTED"
  | "PENDING_REVIEW"
  | "UNDER_REVIEW"
  | "REVISION_REQUESTED"
  | "APPROVED"
  | "GRADED"
  | "SUPERSEDED"
  | "REJECTED";
export type DeadlineStatus = "APPROACHING" | "OVERDUE";
export type ReviewDecision = "APPROVED" | "REVISION_REQUESTED";
export type SubmissionId = string | number;

export interface SubmissionSummary {
  id: number;
  teamId: number;
  teamName?: string;
  deliverableType: DeliverableType;
  status: SubmissionStatus;
  assignedCommitteeId: number | null;
  revisionNumber?: number;
  submittedAt: string;
  deadline?: string | null;
  isOverdue?: boolean;
}

export interface SubmissionReviewSummary {
  totalReviews: number;
  latestStatus?: ReviewDecision | null;
}

export interface GradeCriterionScore {
  criteriaId: SubmissionId;
  criteriaName: string;
  score: number;
}

export interface GradeCriterionInput {
  criterionId: SubmissionId;
  score?: number;
  grade?: string;
}

export interface GradingCriterion {
  id: number;
  deliverableType: DeliverableType;
  gradingType: "SOFT" | "BINARY" | null;
  name: string;
  description?: string | null;
  weight: number;
}

export interface GradeItem {
  id: SubmissionId;
  professorId: SubmissionId;
  professorName: string;
  grade: number;
  feedback?: string | null;
  criteriaScores?: GradeCriterionScore[] | null;
  gradedAt: string;
}

export interface SubmissionGradeSummary {
  averageGrade?: number | null;
  gradeCount: number;
  totalCommitteeMembers: number;
  isGradingComplete: boolean;
  grades?: GradeItem[];
}

export interface SubmissionRevision {
  id: SubmissionId;
  parentSubmissionId?: SubmissionId | null;
  revisionNumber: number;
  status: SubmissionStatus;
  submittedAt: string;
  description?: string | null;
  deliverableType?: DeliverableType;
  fileUrl?: string | null;
}

export interface SubmissionRevisionNode extends SubmissionRevision {
  children?: SubmissionRevisionNode[];
}

export interface SubmissionDetail {
  id: SubmissionId;
  teamId: SubmissionId;
  teamName?: string;
  fileUrl?: string | null;
  fileName?: string | null;
  content?: string | null;
  deliverableType: DeliverableType;
  status: SubmissionStatus;
  assignedCommitteeId?: SubmissionId | null;
  assignedCommitteeName?: string | null;
  revisionNumber?: number;
  parentSubmissionId?: SubmissionId | null;
  submittedAt: string;
  deadline?: string | null;
  reviewSummary?: SubmissionReviewSummary | null;
  gradeSummary?: SubmissionGradeSummary | null;
  revisionHistory?: SubmissionRevision[] | SubmissionRevisionNode[];
}

export interface SubmissionDetailResponse {
  status: string;
  data: SubmissionDetail;
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

export interface RevisionHistoryResponse {
  status: string;
  data: SubmissionRevision[] | SubmissionRevisionNode[];
}

export interface GradeListResponse {
  status: string;
  data: SubmissionGradeSummary;
}

export interface PaginationMeta {
  currentPage: number;
  pageSize: number;
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

export async function fetchSubmissionDetail(submissionId: SubmissionId): Promise<SubmissionDetailResponse> {
  const res = await fetch(`${API_BASE}/submissions/${submissionId}`, {
    headers: buildHeaders(),
  });

  if (!res.ok) {
    const msg = await parseError(res);
    throw new Error(msg);
  }

  const raw = await res.json();

  // Backend returns the object directly (no { status, data } wrapper).
  // Map backend field names to the frontend SubmissionDetail shape.
  if (raw && typeof raw === "object" && !("data" in raw)) {
    const mapped: SubmissionDetail = {
      id: raw.id,
      teamId: raw.groupId ?? raw.teamId,
      teamName: raw.teamName,
      deliverableType: raw.deliverableType,
      status: raw.status,
      content: raw.content ?? null,
      fileUrl: raw.fileUrl ?? null,
      fileName: raw.fileName ?? null,
      assignedCommitteeId: raw.committeeId ?? null,
      revisionNumber: raw.version ?? raw.revisionNumber ?? 1,
      parentSubmissionId: raw.parentSubmissionId ?? null,
      submittedAt: raw.createdAt ?? raw.submittedAt,
      deadline: raw.deadline ?? null,
      revisionHistory: [],
    };
    return { status: "success", data: mapped };
  }

  return raw as SubmissionDetailResponse;
}

export async function fetchSubmissionRevisionHistory(submissionId: SubmissionId): Promise<RevisionHistoryResponse> {
  // TODO(#157): keep this aligned with the backend when the documented
  // GET /submissions/{submissionId}/revisions endpoint is implemented.
  const res = await fetch(`${API_BASE}/submissions/${submissionId}/revisions`, {
    headers: buildHeaders(),
  });

  if (!res.ok) {
    const msg = await parseError(res);
    throw new Error(msg);
  }

  return res.json();
}

export async function fetchSubmissionGrades(submissionId: SubmissionId): Promise<GradeListResponse> {
  const res = await fetch(`${API_BASE}/submissions/${submissionId}/grades`, {
    headers: buildHeaders(),
  });

  if (!res.ok) {
    const msg = await parseError(res);
    throw new Error(msg);
  }

  return res.json();
}

export async function fetchGradingCriteria(deliverableType: DeliverableType): Promise<GradingCriterion[]> {
  const res = await fetch(`${API_BASE}/grading-criteria?deliverableType=${deliverableType}`, {
    headers: buildHeaders(),
    cache: "no-store",
  });

  if (!res.ok) {
    const msg = await parseError(res);
    throw new Error(msg);
  }

  const body = await res.json();
  return body.data ?? [];
}

export async function fetchSubmissionReviews(submissionId: SubmissionId): Promise<ReviewListResponse> {
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
  payload: { file?: File; description?: string; content?: string },
): Promise<RevisionCreateResponse> {
  const token = getToken();
  const formData = new FormData();
  if (payload.file) {
    formData.append("file", payload.file);
  }
  if (payload.description?.trim()) {
    formData.append("description", payload.description.trim());
  }
  if (payload.content?.trim()) {
    formData.append("content", payload.content.trim());
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

export async function createReview(
  submissionId: SubmissionId,
  payload: { decision: "APPROVED" | "REVISION_REQUESTED"; comments: string },
): Promise<void> {
  const res = await fetch(`${API_BASE}/submissions/${submissionId}/reviews`, {
    method: "POST",
    headers: { ...buildHeaders(), "Content-Type": "application/json" },
    body: JSON.stringify({ status: payload.decision, comment: payload.comments }),
  });

  if (!res.ok) {
    const msg = await parseError(res);
    throw new Error(msg);
  }
}

export interface CriterionScorePayload {
  criterionId: number;
  score: number;
}

export interface GradePayload {
  grade?: number;
  feedback?: string;
  criterionScores?: CriterionScorePayload[];
}

export interface GradingCriteriaItem {
  id: number;
  name: string;
  description: string | null;
  gradingType: "SOFT" | "BINARY" | null;
  weight: number;
}

export async function fetchCriteriaForDeliverableType(
  deliverableType: string,
): Promise<GradingCriteriaItem[]> {
  const res = await fetch(
    `${API_BASE}/grading-criteria?deliverableType=${encodeURIComponent(deliverableType)}`,
    { headers: buildHeaders(), cache: "no-store" },
  );
  if (!res.ok) return [];
  const body = await res.json();
  return (body.data ?? []) as GradingCriteriaItem[];
}

export async function submitGrade(
  submissionId: SubmissionId,
  payload: GradePayload,
): Promise<void> {
  const res = await fetch(`${API_BASE}/submissions/${submissionId}/grades`, {
    method: "POST",
    headers: { ...buildHeaders(), "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  if (!res.ok) {
    const msg = await parseError(res);
    throw new Error(msg);
  }
}

export async function updateGrade(
  submissionId: SubmissionId,
  gradeId: number,
  payload: GradePayload,
): Promise<void> {
  const res = await fetch(`${API_BASE}/submissions/${submissionId}/grades/${gradeId}`, {
    method: "PUT",
    headers: { ...buildHeaders(), "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  if (!res.ok) {
    const msg = await parseError(res);
    throw new Error(msg);
  }
}

// ── Annotation API ────────────────────────────────────────────────────────────

export interface Annotation {
  id: number;
  submissionId: number;
  advisorId: number;
  criterionId: number | null;
  selectedText: string;
  startOffset: number;
  endOffset: number;
  comment: string | null;
  grade: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateAnnotationPayload {
  criterionId?: number | null;
  selectedText: string;
  startOffset: number;
  endOffset: number;
  comment?: string;
  grade?: string;
}

export interface UpdateAnnotationPayload {
  criterionId?: number | null;
  comment?: string;
  grade?: string;
}

function annotationAuthHeaders(): Record<string, string> {
  const token = getToken();
  return token
    ? { Authorization: `Bearer ${token}`, "Content-Type": "application/json" }
    : { "Content-Type": "application/json" };
}

export async function fetchAnnotations(submissionId: SubmissionId): Promise<Annotation[]> {
  const res = await fetch(`${API_BASE}/submissions/${submissionId}/annotations`, {
    headers: annotationAuthHeaders(),
  });
  if (!res.ok) throw new Error(await parseError(res));
  return res.json();
}

export async function createAnnotation(
  submissionId: SubmissionId,
  payload: CreateAnnotationPayload,
): Promise<Annotation> {
  const res = await fetch(`${API_BASE}/submissions/${submissionId}/annotations`, {
    method: "POST",
    headers: annotationAuthHeaders(),
    body: JSON.stringify(payload),
  });
  if (!res.ok) throw new Error(await parseError(res));
  return res.json();
}

export async function updateAnnotation(
  submissionId: SubmissionId,
  annotationId: number,
  payload: UpdateAnnotationPayload,
): Promise<Annotation> {
  const res = await fetch(`${API_BASE}/submissions/${submissionId}/annotations/${annotationId}`, {
    method: "PUT",
    headers: annotationAuthHeaders(),
    body: JSON.stringify(payload),
  });
  if (!res.ok) throw new Error(await parseError(res));
  return res.json();
}

export async function deleteAnnotation(
  submissionId: SubmissionId,
  annotationId: number,
): Promise<void> {
  const res = await fetch(`${API_BASE}/submissions/${submissionId}/annotations/${annotationId}`, {
    method: "DELETE",
    headers: annotationAuthHeaders(),
  });
  if (!res.ok) throw new Error(await parseError(res));
}
