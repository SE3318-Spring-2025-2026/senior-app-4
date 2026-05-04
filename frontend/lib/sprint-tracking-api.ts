import apiClient from "@/lib/client";

export interface ActiveSprintInfo {
  sprintId: number;
  sprintName: string;
  startDate: string;
  endDate: string;
  daysRemaining: number;
}

export interface PerStudentSummary {
  githubUsername: string;
  completedStoryPoints: number;
  totalAssignedStoryPoints: number;
}

export interface GroupSummary {
  groupId: number;
  groupName: string;
  totalIssues: number;
  mergedPRCount: number;
  syncedAt: string;
  perStudentSummary: PerStudentSummary[];
}

export interface SprintSummaryResponse {
  activeSprint: ActiveSprintInfo;
  groups: GroupSummary[];
}

export interface IssueTracking {
  issueKey: string;
  storyPoints: number | null;
  assigneeGithubUsername: string | null;
  prNumber: number | null;
  prMerged: boolean | null;
}

export interface GroupTrackingDetail {
  groupId: number;
  groupName: string;
  sprintId: number;
  syncedAt: string;
  issues: IssueTracking[];
}

/**
 * Fetch advisor's active sprint summary with all groups
 */
export async function fetchAdvisorSprintSummary(token: string): Promise<SprintSummaryResponse> {
  try {
    const response = await apiClient.get("/api/v1/advisor/sprint-summary", {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });
    return response.data;
  } catch (error) {
    console.error("Failed to fetch sprint summary:", error);
    throw error;
  }
}

/**
 * Fetch detailed tracking for a specific group
 */
export async function fetchGroupTrackingDetails(
  groupId: number,
  token: string
): Promise<GroupTrackingDetail> {
  try {
    const response = await apiClient.get(
      `/api/v1/advisor/groups/${groupId}/sprint-tracking`,
      {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      }
    );
    return response.data;
  } catch (error) {
    console.error(`Failed to fetch tracking details for group ${groupId}:`, error);
    throw error;
  }
}
