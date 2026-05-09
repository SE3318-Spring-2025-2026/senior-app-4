import { API_BASE, buildHeaders, parseError } from "@/lib/api-utils";

export type SoftGrade = "A" | "B" | "C" | "D" | "F";
export type BinaryGrade = "S" | "F";
export type FinalDeliverableType = "PROPOSAL" | "STATEMENT_OF_WORK" | "DEMONSTRATION";

export interface SprintConfig {
  id: number;
  sprintName: string;
  startDate: string;
  endDate: string;
  status: string;
  requiredStoryPoints: number | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface SprintConfigPayload {
  sprintName: string;
  startDate: string;
  endDate: string;
  status: string;
  requiredStoryPoints: number;
}

export interface SprintDeliverableWeightItem {
  id?: number | null;
  sprintId: number;
  sprintName?: string;
  deliverableType: FinalDeliverableType;
  weight: number;
}

export interface SprintAdvisorGrade {
  id: number;
  groupId: number;
  sprintId: number;
  advisorId: number;
  scrumGrade: SoftGrade;
  codeReviewGrade: SoftGrade;
  updatedAt: string;
}

export interface GroupGradeSummary {
  groupId: number;
  groupName: string;
  teamGrade: number | null;
  published: boolean;
  publishedAt: string | null;
  studentCount: number;
}

export interface FinalGradeResponse {
  status: string;
  data: {
    groupId: number;
    groupName: string;
    teamGrade: number;
    published: boolean;
    computedAt: string | null;
    publishedAt: string | null;
    deliverables: Array<{
      deliverableType: FinalDeliverableType | string;
      rawGrade: number;
      scrumAverage: number;
      codeReviewAverage: number;
      scalar: number;
      scaledGrade: number;
      finalWeight: number;
      contribution: number;
    }>;
    students: Array<{
      userId: number;
      fullName: string;
      githubUsername: string | null;
      finalGrade: number;
      spRatio: number;
      published: boolean;
    }>;
  };
}

export interface AiCodeReviewSuggestion {
  status: string;
  data: {
    groupId: number;
    sprintId: number;
    aiScore: number;
    suggestedGrade: SoftGrade;
  };
}

async function jsonOrThrow<T>(res: Response): Promise<T> {
  if (!res.ok) {
    throw new Error(await parseError(res));
  }
  return res.json();
}

export async function fetchFinalGradingSprints(): Promise<SprintConfig[]> {
  const res = await fetch(`${API_BASE}/final-grading/sprints`, {
    headers: buildHeaders(),
    cache: "no-store",
  });
  const body = await jsonOrThrow<{ data: SprintConfig[] }>(res);
  return body.data ?? [];
}

export async function createFinalGradingSprint(payload: SprintConfigPayload): Promise<SprintConfig> {
  const res = await fetch(`${API_BASE}/final-grading/sprints`, {
    method: "POST",
    headers: buildHeaders(),
    body: JSON.stringify(payload),
  });
  const body = await jsonOrThrow<{ data: SprintConfig }>(res);
  return body.data;
}

export async function updateFinalGradingSprint(
  sprintId: number,
  payload: SprintConfigPayload,
): Promise<SprintConfig> {
  const res = await fetch(`${API_BASE}/final-grading/sprints/${sprintId}`, {
    method: "PUT",
    headers: buildHeaders(),
    body: JSON.stringify(payload),
  });
  const body = await jsonOrThrow<{ data: SprintConfig }>(res);
  return body.data;
}

export async function fetchSprintDeliverableWeights(): Promise<SprintDeliverableWeightItem[]> {
  const res = await fetch(`${API_BASE}/final-grading/sprint-deliverable-weights`, {
    headers: buildHeaders(),
    cache: "no-store",
  });
  const body = await jsonOrThrow<{ data: SprintDeliverableWeightItem[] }>(res);
  return body.data ?? [];
}

export async function replaceSprintDeliverableWeights(items: SprintDeliverableWeightItem[]): Promise<SprintDeliverableWeightItem[]> {
  const res = await fetch(`${API_BASE}/final-grading/sprint-deliverable-weights`, {
    method: "PUT",
    headers: buildHeaders(),
    body: JSON.stringify({ items }),
  });
  const body = await jsonOrThrow<{ data: SprintDeliverableWeightItem[] }>(res);
  return body.data ?? [];
}

export async function fetchSprintAdvisorGrades(groupId: number): Promise<SprintAdvisorGrade[]> {
  const res = await fetch(`${API_BASE}/final-grading/groups/${groupId}/sprint-advisor-grades`, {
    headers: buildHeaders(),
    cache: "no-store",
  });
  const body = await jsonOrThrow<{ data: SprintAdvisorGrade[] }>(res);
  return body.data ?? [];
}

export async function saveSprintAdvisorGrade(payload: {
  groupId: number;
  sprintId: number;
  scrumGrade: SoftGrade;
  codeReviewGrade: SoftGrade;
}): Promise<SprintAdvisorGrade> {
  const res = await fetch(`${API_BASE}/final-grading/sprint-advisor-grades`, {
    method: "POST",
    headers: buildHeaders(),
    body: JSON.stringify(payload),
  });
  const body = await jsonOrThrow<{ data: SprintAdvisorGrade }>(res);
  return body.data;
}

export async function fetchDemonstrationGrade(groupId: number): Promise<number | null> {
  const res = await fetch(`${API_BASE}/final-grading/groups/${groupId}/demonstration-grade`, {
    headers: buildHeaders(),
    cache: "no-store",
  });
  if (!res.ok) return null;
  const body = await res.json() as { data?: { grade?: number | null } };
  return body.data?.grade ?? null;
}

export async function saveDemonstrationGrade(groupId: number, grade: number): Promise<void> {
  const res = await fetch(`${API_BASE}/final-grading/groups/${groupId}/demonstration-grade`, {
    method: "POST",
    headers: buildHeaders(),
    body: JSON.stringify({ grade }),
  });
  if (!res.ok) {
    const msg = await parseError(res);
    throw new Error(msg);
  }
}

export async function initializeDemonstration(groupId: number): Promise<number> {
  const res = await fetch(`${API_BASE}/final-grading/groups/${groupId}/initialize-demonstration`, {
    method: "POST",
    headers: buildHeaders(),
    body: JSON.stringify({}),
  });
  if (!res.ok) {
    const msg = await parseError(res);
    throw new Error(msg);
  }
  const body = await res.json() as { data: { id: number } };
  return body.data.id;
}

export async function fetchAiCodeReviewSuggestion(groupId: number, sprintId: number): Promise<AiCodeReviewSuggestion> {
  const res = await fetch(`${API_BASE}/final-grading/groups/${groupId}/sprints/${sprintId}/ai-code-review-suggestion`, {
    headers: buildHeaders(),
    cache: "no-store",
  });
  return jsonOrThrow<AiCodeReviewSuggestion>(res);
}

export async function recalculateFinalGrades(groupId: number): Promise<FinalGradeResponse> {
  const res = await fetch(`${API_BASE}/final-grading/groups/${groupId}/recalculate`, {
    method: "POST",
    headers: buildHeaders(),
  });
  return jsonOrThrow<FinalGradeResponse>(res);
}

export async function publishFinalGrades(groupId: number): Promise<FinalGradeResponse> {
  const res = await fetch(`${API_BASE}/final-grading/groups/${groupId}/publish`, {
    method: "POST",
    headers: buildHeaders(),
  });
  return jsonOrThrow<FinalGradeResponse>(res);
}

export async function fetchFinalGrades(groupId: number): Promise<FinalGradeResponse> {
  const res = await fetch(`${API_BASE}/final-grading/groups/${groupId}`, {
    headers: buildHeaders(),
    cache: "no-store",
  });
  return jsonOrThrow<FinalGradeResponse>(res);
}

export async function fetchAllGroupGrades(): Promise<GroupGradeSummary[]> {
  const res = await fetch(`${API_BASE}/final-grading/groups`, {
    headers: buildHeaders(),
    cache: "no-store",
  });
  const body = await jsonOrThrow<{ data: GroupGradeSummary[] }>(res);
  return body.data ?? [];
}
