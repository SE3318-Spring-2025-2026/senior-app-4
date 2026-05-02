import { API_BASE, buildHeaders, parseError } from "@/lib/api-utils";

export interface LeaderboardStudent {
    studentId: number;
    name: string;
    storyPoints: number;
    ratio: number;
}

export interface PaginationInfo {
    totalPages: number;
    totalElements: number;
    size?: number;
    number?: number;
}

export interface LeaderboardResponse {
    content: LeaderboardStudent[];
    totalPages: number;
    totalElements: number;
}

export interface LeaderboardFilterParams {
    page?: number;
    size?: number;
    sort?: string;
}

export async function getLeaderboard(params: LeaderboardFilterParams = {}): Promise<LeaderboardResponse> {
    const urlParams = new URLSearchParams();
    if (params.page !== undefined) urlParams.append("page", params.page.toString());
    if (params.size !== undefined) urlParams.append("size", params.size.toString());
    if (params.sort) urlParams.append("sort", params.sort);

    const qs = urlParams.toString();
    const url = `${API_BASE}/analytics/leaderboard${qs ? `?${qs}` : ""}`;

    const res = await fetch(url, {
        method: "GET",
        headers: buildHeaders(),
    });

    if (!res.ok) {
        throw new Error(await parseError(res));
    }

    return res.json();
}
