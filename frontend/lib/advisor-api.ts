import { UserRole } from "@/types/enums";
import { API_BASE, buildHeaders as getAuthHeaders, parseError } from "@/lib/api-utils";

export type ProfessorDirectoryItem = {
    userId: number;
    fullName: string;
    email: string;
    githubUsername: string | null;
    role: string;
    createdAt: string | null;
};

export type AdvisorRequestInfo = {
    requestId: number;
    professorName: string;
    status: string;
};

type UserListResponse = {
    message: string;
    count: number;
    data: ProfessorDirectoryItem[];
};

export async function fetchProfessors(): Promise<ProfessorDirectoryItem[]> {
    // TODO(#80): replace the generic professor directory with a dedicated
    // "available professors" endpoint once backend filtering/availability data lands.
    const res = await fetch(`${API_BASE}/users?role=${UserRole.PROFESSOR}`, {
        method: "GET",
        headers: {
            Authorization: getAuthHeaders().Authorization,
        },
        cache: "no-store",
    });

    if (!res.ok) {
        throw new Error(await parseError(res));
    }

    const payload = (await res.json()) as UserListResponse;
    return payload.data ?? [];
}

export async function fetchAdvisorRequestInfo(groupId: number): Promise<AdvisorRequestInfo | null> {
    const res = await fetch(`${API_BASE}/groups/${groupId}/advisor-request`, {
        method: "GET",
        headers: {
            Authorization: getAuthHeaders().Authorization,
        },
        cache: "no-store",
    });

    if (!res.ok) {
        const message = await parseError(res);
        if (message === "No pending advisor request found.") {
            return null;
        }

        throw new Error(message);
    }

    return res.json();
}

export async function createAdvisorRequest(groupId: number, professorId: number, message?: string): Promise<void> {
    const res = await fetch(`${API_BASE}/advisor-requests`, {
        method: "POST",
        headers: getAuthHeaders(),
        body: JSON.stringify({ teamId: groupId, professorId, message: message ?? null }),
    });

    if (!res.ok) {
        throw new Error(await parseError(res));
    }
}

export async function withdrawAdvisorRequest(requestId: number): Promise<void> {
    const res = await fetch(`${API_BASE}/advisor-requests/${requestId}`, {
        method: "DELETE",
        headers: {
            Authorization: getAuthHeaders().Authorization,
        },
    });

    if (!res.ok) {
        throw new Error(await parseError(res));
    }
}
