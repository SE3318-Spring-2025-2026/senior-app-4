import { getToken } from "@/lib/auth";

export type ProfessorDirectoryItem = {
    userId: number;
    fullName: string;
    email: string;
    githubUsername: string | null;
    role: string;
    createdAt: string | null;
};

export type AdvisorRequestStatus = {
    requestId: number;
    professorName: string;
    status: string;
};

type UserListResponse = {
    message: string;
    count: number;
    data: ProfessorDirectoryItem[];
};

type ErrorPayload = {
    error?: string;
    message?: string;
};

const API_BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api/v1";

async function parseError(res: Response) {
    try {
        const data = (await res.json()) as ErrorPayload;
        return data.message || data.error || "Request failed.";
    } catch {
        return "Request failed.";
    }
}

function getAuthHeaders() {
    const token = getToken();

    return {
        Authorization: `Bearer ${token ?? ""}`,
        "Content-Type": "application/json",
    };
}

export async function fetchProfessors(): Promise<ProfessorDirectoryItem[]> {
    // TODO(#80): replace the generic professor directory with a dedicated
    // "available professors" endpoint once backend filtering/availability data lands.
    const res = await fetch(`${API_BASE}/users`, {
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

    return (payload.data ?? []).filter(
        (user) => user.role?.toLowerCase() === "professor"
    );
}

export async function fetchAdvisorRequestStatus(groupId: number): Promise<AdvisorRequestStatus | null> {
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

export async function createAdvisorRequest(groupId: number, professorId: number): Promise<void> {
    const res = await fetch(`${API_BASE}/advisor-requests`, {
        method: "POST",
        headers: getAuthHeaders(),
        body: JSON.stringify({ teamId: String(groupId), professorId }),
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
