import { API_BASE, buildHeaders, parseError } from "./api-utils";

export interface StudentPerformanceDto {
    studentId: number;
    name: string;
    assignedSp: number;
    accomplishedSp: number;
    ratio: number;
}

export interface LeaderboardResponse {
    content: StudentPerformanceDto[];
    totalPages: number;
    totalElements: number;
}

export async function fetchLeaderboard(
    page: number = 0,
    size: number = 10,
    sortField: string = "ratio",
    sortDir: string = "desc"
): Promise<LeaderboardResponse> {
    const backendSortField = sortField === "name" ? "user.fullName" : sortField;

    const params = new URLSearchParams({
        page: String(page),
        size: String(size),
        sort: `${backendSortField},${sortDir}`,
    });

    const res = await fetch(`${API_BASE}/analytics/leaderboard?${params.toString()}`, {
        headers: buildHeaders(),
        cache: "no-store",
    });

    if (!res.ok) {
        throw new Error(await parseError(res));
    }

    return res.json();
}
