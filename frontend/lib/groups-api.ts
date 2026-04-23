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
    totalPages: number;
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

/* ===================== GET ===================== */

export async function fetchGroups(
    page = 0,
    size = 1000
): Promise<ApiPage<ApiGroupListItem>> {
    const token = getToken();

    const params = new URLSearchParams();
    params.append("page", page.toString());
    params.append("size", size.toString());

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

/* ===================== CREATE ===================== */

export async function createGroupApi(groupName: string) {
    const token = getToken();

    const res = await fetch(`${API_BASE}/groups`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token ?? ""}`,
        },
        body: JSON.stringify({ groupName }),
    });

    if (!res.ok) {
        throw new Error(await parseError(res));
    }

    return res.json();
}

/* ===================== UPDATE ===================== */

export async function updateGroupNameApi(
    groupId: number,
    groupName: string
) {
    const token = getToken();

    const res = await fetch(`${API_BASE}/groups/${groupId}`, {
        method: "PUT",
        headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token ?? ""}`,
        },
        body: JSON.stringify({ groupName }),
    });

    if (!res.ok) {
        throw new Error(await parseError(res));
    }
}

/* ===================== MEMBERS ===================== */

export async function addMemberApi(
    groupId: number,
    studentId: number
) {
    const token = getToken();

    const res = await fetch(`${API_BASE}/groups/${groupId}/members`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token ?? ""}`,
        },
        body: JSON.stringify({ studentId }),
    });

    if (!res.ok) {
        throw new Error(await parseError(res));
    }
}

export async function removeMemberApi(
    groupId: number,
    studentId: number
) {
    const token = getToken();

    const res = await fetch(
        `${API_BASE}/groups/${groupId}/members/${studentId}`,
        {
            method: "DELETE",
            headers: {
                Authorization: `Bearer ${token ?? ""}`,
            },
        }
    );

    if (!res.ok) {
        throw new Error(await parseError(res));
    }
}

/* ===================== LEAVE ===================== */

export async function leaveGroupApi(groupId: number) {
    const token = getToken();

    const res = await fetch(`${API_BASE}/groups/${groupId}/leave`, {
        method: "POST",
        headers: {
            Authorization: `Bearer ${token ?? ""}`,
        },
    });

    if (!res.ok) {
        throw new Error(await parseError(res));
    }
}