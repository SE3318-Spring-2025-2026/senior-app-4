import { getToken } from "@/lib/auth";
import {
    Committee,
    CommitteeFormValues,
    CommitteePageResponse,
    CommitteeStatus,
} from "@/lib/committee-types";

const API_BASE =
    process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api/v1";

const USE_MOCK = true;

let mockCommittees: Committee[] = [
    {
        committeeId: 1,
        committeeName: "Senior Project Committee A",
        description: "Main evaluation committee.",
        status: "ACTIVE",
        createdBy: 1,
        advisorCount: 2,
        juryCount: 3,
        groupCount: 5,
        createdAt: "2026-04-20T10:00:00Z",
        updatedAt: "2026-04-20T10:00:00Z",
    },
    {
        committeeId: 2,
        committeeName: "Proposal Review Committee",
        description: "Reviews proposal submissions.",
        status: "FORMING",
        createdBy: 1,
        advisorCount: 1,
        juryCount: 2,
        groupCount: 3,
        createdAt: "2026-04-22T10:00:00Z",
        updatedAt: "2026-04-22T10:00:00Z",
    },
];

async function parseError(res: Response) {
    try {
        const data = await res.json();
        return data?.message || data?.error || "Request failed.";
    } catch {
        return "Request failed.";
    }
}

function filterSortPaginate(
    committees: Committee[],
    page: number,
    size: number,
    status: string,
    search: string,
    sort: string
): CommitteePageResponse {
    let result = [...committees];

    if (status && status !== "ALL") {
        result = result.filter((committee) => committee.status === status);
    }

    if (search.trim()) {
        const query = search.trim().toLowerCase();
        result = result.filter((committee) =>
            committee.committeeName.toLowerCase().includes(query)
        );
    }

    result.sort((a, b) => {
        if (sort === "name_asc") return a.committeeName.localeCompare(b.committeeName);
        if (sort === "name_desc") return b.committeeName.localeCompare(a.committeeName);

        const aDate = new Date(a.createdAt || "").getTime();
        const bDate = new Date(b.createdAt || "").getTime();

        if (sort === "created_asc") return aDate - bDate;
        return bDate - aDate;
    });

    const totalElements = result.length;
    const totalPages = Math.max(Math.ceil(totalElements / size), 1);
    const start = page * size;

    return {
        content: result.slice(start, start + size),
        totalPages,
        totalElements,
    };
}

export async function fetchCommittees(
    page = 0,
    size = 10,
    status = "ALL",
    search = "",
    sort = "created_desc"
): Promise<CommitteePageResponse> {
    if (USE_MOCK) {
        return filterSortPaginate(mockCommittees, page, size, status, search, sort);
    }

    const token = getToken();
    const params = new URLSearchParams({
        page: String(page),
        size: String(size),
        sort,
    });

    if (status !== "ALL") params.set("status", status);
    if (search) params.set("search", search);

    const res = await fetch(`${API_BASE}/committees?${params.toString()}`, {
        headers: { Authorization: `Bearer ${token ?? ""}` },
        cache: "no-store",
    });

    if (!res.ok) throw new Error(await parseError(res));
    return res.json();
}

export async function fetchCoordinatorCommittees(
    coordinatorId: number,
    page = 0,
    size = 10,
    status = "ALL",
    search = "",
    sort = "created_desc"
): Promise<CommitteePageResponse> {
    if (USE_MOCK) {
        return filterSortPaginate(mockCommittees, page, size, status, search, sort);
    }

    const token = getToken();
    const params = new URLSearchParams({
        page: String(page),
        size: String(size),
        sort,
    });

    if (status !== "ALL") params.set("status", status);
    if (search) params.set("search", search);

    const res = await fetch(
        `${API_BASE}/committees/coordinator/${coordinatorId}?${params.toString()}`,
        {
            headers: { Authorization: `Bearer ${token ?? ""}` },
            cache: "no-store",
        }
    );

    if (!res.ok) throw new Error(await parseError(res));
    return res.json();
}

export async function createCommittee(values: CommitteeFormValues) {
    if (USE_MOCK) {
        const nextCommittee: Committee = {
            committeeId: Date.now(),
            committeeName: values.committeeName,
            description: values.description || null,
            status: values.status,
            createdBy: 1,
            advisorCount: 0,
            juryCount: 0,
            groupCount: 0,
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
        };

        mockCommittees = [nextCommittee, ...mockCommittees];
        return nextCommittee;
    }

    const token = getToken();
    const res = await fetch(`${API_BASE}/committees`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token ?? ""}`,
        },
        body: JSON.stringify(values),
    });

    if (!res.ok) throw new Error(await parseError(res));
    return res.json();
}

export async function updateCommittee(
    committeeId: number,
    values: CommitteeFormValues
) {
    if (USE_MOCK) {
        mockCommittees = mockCommittees.map((committee) =>
            committee.committeeId === committeeId
                ? {
                    ...committee,
                    ...values,
                    updatedAt: new Date().toISOString(),
                }
                : committee
        );

        return mockCommittees.find((committee) => committee.committeeId === committeeId);
    }

    const token = getToken();
    const res = await fetch(`${API_BASE}/committees/${committeeId}`, {
        method: "PUT",
        headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token ?? ""}`,
        },
        body: JSON.stringify(values),
    });

    if (!res.ok) throw new Error(await parseError(res));
    return res.json();
}

export async function deleteCommittee(committeeId: number) {
    if (USE_MOCK) {
        mockCommittees = mockCommittees.filter(
            (committee) => committee.committeeId !== committeeId
        );
        return;
    }

    const token = getToken();
    const res = await fetch(`${API_BASE}/committees/${committeeId}`, {
        method: "DELETE",
        headers: { Authorization: `Bearer ${token ?? ""}` },
    });

    if (!res.ok) throw new Error(await parseError(res));
}
export async function fetchCommitteeById(
    committeeId: number
): Promise<Committee> {
    if (USE_MOCK) {
        const committee = mockCommittees.find(
            (item) => item.committeeId === committeeId
        );

        if (!committee) {
            throw new Error("Committee not found.");
        }

        return committee;
    }

    const token = getToken();

    const res = await fetch(`${API_BASE}/committees/${committeeId}`, {
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

export async function assignAdvisor(
    committeeId: number,
    advisorId: number,
    role: string
): Promise<void> {
    const token = getToken();

    const res = await fetch(`${API_BASE}/committees/${committeeId}/advisors`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token ?? ""}`,
        },
        body: JSON.stringify({
            advisorId,
            role,
        }),
    });

    if (!res.ok) throw new Error(await parseError(res));
}

export async function removeAdvisor(
    committeeId: number,
    advisorId: number
): Promise<void> {
    const token = getToken();

    const res = await fetch(
        `${API_BASE}/committees/${committeeId}/advisors/${advisorId}`,
        {
            method: "DELETE",
            headers: {
                Authorization: `Bearer ${token ?? ""}`,
            },
        }
    );

    if (!res.ok) throw new Error(await parseError(res));
}
