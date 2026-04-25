import { getToken } from "@/lib/auth";

const API_BASE =
    process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api/v1";

export type AdvisorRequestStatus = "PENDING" | "APPROVED" | "REJECTED" | "WITHDRAWN";

export type AdvisorRequestSummary = {
    id: number;
    teamId: number;
    teamName: string;
    professorId: number;
    professorName: string;
    status: AdvisorRequestStatus;
    message?: string | null;
    requestedAt: string;
    decidedAt: string | null;
};

export type AdvisorRequestListResponse = {
    status: string;
    data: AdvisorRequestSummary[];
};

export type AdvisorDecisionRequest = {
    decision: "APPROVE" | "REJECT";
    reason?: string;
};

export type AdvisorDecisionResponse = {
    status: string;
    message: string;
    data: {
        requestId: number;
        decision: "APPROVE" | "REJECT";
        teamId: number;
        advisorId: number | null;
    };
};

function buildHeaders(): Record<string, string> {
    const headers: Record<string, string> = { "Content-Type": "application/json" };
    const token = getToken();
    if (token) headers["Authorization"] = `Bearer ${token}`;
    return headers;
}

async function parseError(res: Response): Promise<string> {
    try {
        const data = await res.json();
        return data?.message || data?.error || "Request failed.";
    } catch {
        return "Request failed.";
    }
}

export async function fetchPendingRequests(): Promise<AdvisorRequestSummary[]> {
    const res = await fetch(`${API_BASE}/advisor-requests?status=PENDING`, {
        method: "GET",
        headers: buildHeaders(),
        cache: "no-store",
    });

    if (!res.ok) {
        throw new Error(await parseError(res));
    }

    const body: AdvisorRequestListResponse = await res.json();
    return body.data ?? [];
}

export type AdvisorAssignment = {
    teamId: number;
    teamName: string;
    leaderName: string;
    advisorId: number | null;
    advisorName: string | null;
    status: string;
    // TODO(P4-ASSIGN-2 backend): assignedAt and assignmentType are currently
    // returned as null until assignment metadata is persisted or derived.
    assignedAt: string | null;
    assignmentType: "REQUESTED" | "OVERRIDDEN" | null;
};

export type AdvisorAssignmentListResponse = {
    status: string;
    data: AdvisorAssignment[];
};

export type OverrideAssignmentRequest = {
    teamId: number;
    advisorId: number;
    reason?: string;
};

export type FetchAdvisorAssignmentsOptions = {
    advisorId?: number;
    hasAdvisor?: boolean;
};

export async function fetchAdvisorAssignments(
    options: FetchAdvisorAssignmentsOptions = {}
): Promise<AdvisorAssignment[]> {
    const params = new URLSearchParams();
    if (options.advisorId != null) params.set("advisorId", String(options.advisorId));
    if (options.hasAdvisor != null) params.set("hasAdvisor", String(options.hasAdvisor));

    const query = params.toString();
    const res = await fetch(`${API_BASE}/advisor-assignments${query ? `?${query}` : ""}`, {
        method: "GET",
        headers: buildHeaders(),
        cache: "no-store",
    });

    if (!res.ok) {
        throw new Error(await parseError(res));
    }

    const body: AdvisorAssignmentListResponse = await res.json();
    return body.data ?? [];
}

export async function overrideAdvisorAssignment(
    payload: OverrideAssignmentRequest
): Promise<void> {
    const res = await fetch(`${API_BASE}/advisor-assignments/override`, {
        method: "POST",
        headers: buildHeaders(),
        body: JSON.stringify(payload),
    });

    if (!res.ok) {
        throw new Error(await parseError(res));
    }
}

export async function releaseAdviseeGroup(groupId: number): Promise<void> {
    const res = await fetch(`${API_BASE}/advisor-assignments/${groupId}/release`, {
        method: "POST",
        headers: buildHeaders(),
    });

    if (!res.ok) {
        throw new Error(await parseError(res));
    }
}

export async function submitAdvisorDecision(
    requestId: number,
    payload: AdvisorDecisionRequest
): Promise<AdvisorDecisionResponse> {
    const res = await fetch(`${API_BASE}/advisor-requests/${requestId}/decision`, {
        method: "POST",
        headers: buildHeaders(),
        body: JSON.stringify(payload),
    });

    if (!res.ok) {
        throw new Error(await parseError(res));
    }

    return res.json();
}
