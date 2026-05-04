import apiClient from "@/lib/client";

export interface StudentPerformanceDto {
    githubUsername: string;
    totalCompletedStoryPoints: number;
    totalAssignedStoryPoints: number;
    completionPercentage: number;
    mergedPrCount: number;
    groupName: string;
}

export interface LeaderboardResponse {
    activeSprint: {
        sprintId: number;
        sprintName: string;
        startDate: string;
        endDate: string;
        daysRemaining: number;
    };
    leaderboard: StudentPerformanceDto[];
}

/**
 * Fetch leaderboard data showing top performers across all groups
 */
export async function fetchLeaderboard(token: string): Promise<LeaderboardResponse> {
    try {
        const response = await apiClient.get("/api/v1/advisor/leaderboard", {
            headers: {
                Authorization: `Bearer ${token}`,
            },
        });
        return response.data;
    } catch (error) {
        console.error("Failed to fetch leaderboard:", error);
        throw error;
    }
}
