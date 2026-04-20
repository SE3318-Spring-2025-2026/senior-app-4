import { getToken } from "@/lib/auth";

export type ApiGroupListItem = {
    id: number;
    groupName: string;
    leaderId: number;
    advisorId: number | null;
    status: string;
    memberCount: number;
    createdAt: string;
};

export type ApiGroupMember = {
    userId: number;
    fullName: string;
    role: string;
    joinedAt: string;
};

export type ApiGroupDetail = {
    id: number;
    groupName: string;
    leaderId: number;
    advisorId: number | null;
    status: string;
    createdAt: string;
    updatedAt: string;
    members: ApiGroupMember[];
};

export type ApiPage<T> = {
    content: T[];
    pageable: unknown;
    totalPages: number;
    totalElements: number;
    last: boolean;
    size: number;
    number: number;
    first: boolean;
    numberOfElements: number;
    empty: boolean;
};

const API_BASE =
    process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api/v1";

async function parseError(res: Response) {
    try {
        const data = await res.json();
        return data?.message || data?.error || "Request failed.";
    } catch {
        return "Request failed.";
    }
}

export async function fetchGroups(
    page = 0,
    size = 6,
    status?: string,
    groupName?: string,
    advisorAssigned?: string
): Promise<ApiPage<ApiGroupListItem>> {
    const token = getToken();

    const params = new URLSearchParams();
    params.append("page", page.toString());
    params.append("size", size.toString());
    if (status && status !== "all") params.append("status", status.toUpperCase());
    if (groupName) params.append("groupName", groupName);
    if (advisorAssigned && advisorAssigned !== "all") {
        params.append("advisorAssigned", advisorAssigned === "has_advisor" ? "true" : "false");
    }

    const res = await fetch(`${API_BASE}/groups?${params.toString()}`, {
        method: "GET",
        headers: {
            Authorization: `Bearer ${token ?? ""}`,
        },
        cache: "no-store",
    });

    if (!res.ok) {
        throw new Error(await parseError(res));
    }

    return res.json();
}

export async function fetchGroupDetail(
    groupId: number
): Promise<ApiGroupDetail> {
    const token = getToken();

    const res = await fetch(`${API_BASE}/groups/${groupId}`, {
        method: "GET",
        headers: {
            Authorization: `Bearer ${token ?? ""}`,
        },
        cache: "no-store",
    });

    if (!res.ok) {
        throw new Error(await parseError(res));
    }

    return res.json();
}