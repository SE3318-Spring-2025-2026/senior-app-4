import apiClient from "@/lib/client";
import { getToken } from "@/lib/auth";

export interface StudentPerformanceDto {
    studentId: number;
    name: string;
    assignedSp: number;
    accomplishedSp: number;
    ratio: number;
}

export interface LeaderboardPageResponse {
    content: StudentPerformanceDto[];
    totalPages: number;
    totalElements: number;
    number: number;
    size: number;
}

// Legacy type kept for any existing imports
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

const SORT_FIELD_MAP: Record<string, string> = {
    name: "name",
    assignedSp: "assignedSp",
    accomplishedSp: "accomplishedSp",
    ratio: "ratio",
};

export async function fetchLeaderboard(
    page: number = 0,
    size: number = 10,
    sortField: string = "ratio",
    sortDir: string = "desc"
): Promise<LeaderboardPageResponse> {
    const token = getToken();
    const backendSort = SORT_FIELD_MAP[sortField] ?? "ratio";

    const response = await apiClient.get("/analytics/leaderboard", {
        headers: {
            Authorization: `Bearer ${token ?? ""}`,
        },
        params: {
            page,
            size,
            sort: `${backendSort},${sortDir}`,
        },
    });
    return response.data;
}
