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

/**
 * Fetches the student leaderboard data from the analytics API.
 * 
 * @param page 0-indexed page number
 * @param size number of records per page
 * @param sortField field to sort by (name, assignedSp, accomplishedSp, ratio)
 * @param sortDir direction to sort (asc, desc)
 */
export async function fetchLeaderboard(
    page: number = 0,
    size: number = 10,
    sortField: string = "ratio",
    sortDir: string = "desc"
): Promise<LeaderboardResponse> {
    // Map frontend sort fields to backend property paths
    // 'name' maps to 'user.fullName' since 'name' is in the DTO but sorting happens on the Entity
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
