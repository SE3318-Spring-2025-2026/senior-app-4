import { getToken } from "@/lib/auth";
import {
    Committee,
    CommitteeAuditLog,
    CommitteeAuditLogPageResponse,
    CommitteeDetail,
    CommitteeFormValues,
    CommitteePageResponse,
} from "@/lib/committee-types";

const API_BASE =
    process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api/v1";

const USE_MOCK = false;

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
        status: "ACTIVE",
        createdBy: 1,
        advisorCount: 1,
        juryCount: 2,
        groupCount: 3,
        createdAt: "2026-04-22T10:00:00Z",
        updatedAt: "2026-04-22T10:00:00Z",
    },
];

const mockCommitteeDetails: Record<number, Omit<CommitteeDetail, keyof Committee>> = {
    1: {
        advisors: [
            {
                id: 1,
                name: "Prof. Ali Mert",
                email: "alimert@example.com",
                role: "ADVISOR",
                assignedAt: "2026-04-21T09:30:00Z",
            },
            {
                id: 2,
                name: "Prof. Advisor",
                email: "prof@spms-test.com",
                role: "ADVISOR",
                assignedAt: "2026-04-22T11:00:00Z",
            },
        ],
        jury: [
            {
                id: 3,
                name: "Professor One",
                email: "prof.one@spms-test.com",
                role: "JURY",
                assignedAt: "2026-04-23T12:00:00Z",
            },
            {
                id: 4,
                name: "Professor Two",
                email: "prof.two@spms-test.com",
                role: "JURY",
                assignedAt: "2026-04-23T12:15:00Z",
            },
            {
                id: 5,
                name: "Professor Three",
                email: "prof.three@spms-test.com",
                role: "JURY",
                assignedAt: "2026-04-23T12:30:00Z",
            },
        ],
        groups: [
            {
                groupId: 101,
                groupName: "Group 1",
                membersCount: 4,
                status: "ADVISED",
                examDate: "2026-05-10T10:00:00Z",
            },
            {
                groupId: 102,
                groupName: "Group 2",
                membersCount: 5,
                status: "ADVISED",
                examDate: "2026-05-10T11:00:00Z",
            },
        ],
        recentAuditLogs: [
            {
                id: 1,
                timestamp: "2026-04-24T14:00:00Z",
                userName: "Coordinator",
                action: "COMMITTEE_UPDATED",
                entityType: "COMMITTEE",
                description: "Updated committee status to ACTIVE.",
            },
            {
                id: 2,
                timestamp: "2026-04-23T12:30:00Z",
                userName: "Coordinator",
                action: "JURY_ASSIGNED",
                entityType: "COMMITTEE_ASSIGNMENT",
                description: "Assigned jury members to committee.",
            },
        ],
    },
    2: {
        advisors: [
            {
                id: 6,
                name: "Prof. Proposal Advisor",
                email: "proposal.advisor@spms-test.com",
                role: "ADVISOR",
                assignedAt: "2026-04-24T09:00:00Z",
            },
        ],
        jury: [
            {
                id: 7,
                name: "Proposal Jury One",
                email: "jury.one@spms-test.com",
                role: "JURY",
                assignedAt: "2026-04-24T09:15:00Z",
            },
            {
                id: 8,
                name: "Proposal Jury Two",
                email: "jury.two@spms-test.com",
                role: "JURY",
                assignedAt: "2026-04-24T09:30:00Z",
            },
        ],
        groups: [
            {
                groupId: 201,
                groupName: "Proposal Group A",
                membersCount: 3,
                status: "FORMING",
                examDate: null,
            },
            {
                groupId: 202,
                groupName: "Proposal Group B",
                membersCount: 4,
                status: "FORMING",
                examDate: null,
            },
            {
                groupId: 203,
                groupName: "Proposal Group C",
                membersCount: 4,
                status: "FORMING",
                examDate: null,
            },
        ],
        recentAuditLogs: [
            {
                id: 3,
                timestamp: "2026-04-24T09:30:00Z",
                userName: "Coordinator",
                action: "COMMITTEE_CREATED",
                entityType: "COMMITTEE",
                description: "Created proposal review committee.",
            },
        ],
    },
};
const mockAuditLogs: CommitteeAuditLog[] = [
    {
        id: 1,
        timestamp: "2026-04-24T14:00:00Z",
        userName: "Coordinator",
        action: "COMMITTEE_UPDATED",
        entityType: "COMMITTEE",
        description: "Updated committee status to ACTIVE.",
    },
    {
        id: 2,
        timestamp: "2026-04-23T12:30:00Z",
        userName: "Coordinator",
        action: "JURY_ASSIGNED",
        entityType: "COMMITTEE_ASSIGNMENT",
        description: "Assigned jury members to committee.",
    },
    {
        id: 3,
        timestamp: "2026-04-22T10:15:00Z",
        userName: "Coordinator",
        action: "COMMITTEE_CREATED",
        entityType: "COMMITTEE",
        description: "Created Senior Project Committee A.",
    },
    {
        id: 4,
        timestamp: "2026-04-21T09:45:00Z",
        userName: "Professor",
        action: "GRADE_SUBMITTED",
        entityType: "SUBMISSION",
        description: "Submitted committee grade for Group 1.",
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

function normalizeCommitteeDetail(data: any): CommitteeDetail {
    const advisors = (data.advisors ?? []).map((advisor: any) => ({
        id: advisor.id ?? advisor.committeeAdvisorId ?? advisor.userId,
        name:
            advisor.name ??
            advisor.fullName ??
            advisor.advisorName ??
            `User #${advisor.userId ?? "-"}`,
        email: advisor.email ?? advisor.advisorEmail ?? "",
        role: advisor.role ?? "ADVISOR",
        assignedAt: advisor.assignedAt ?? null,
    }));

    const jury = (data.jury ?? data.juryMembers ?? []).map((member: any) => ({
        id: member.id ?? member.juryMemberId,
        name: member.name ?? member.fullName ?? member.juryMemberName,
        email: member.email ?? member.juryMemberEmail ?? "",
        role: member.role ?? member.juryType ?? "JURY",
        assignedAt: member.assignedAt,
    }));

    const groups = (data.groups ?? data.groupAssignments ?? []).map((group: any) => ({
        groupId: group.groupId,
        groupName: group.groupName ?? `Group #${group.groupId}`,
        membersCount: group.membersCount ?? 0,
        status: group.status,
        examDate: group.examDate,
    }));

    return {
        ...data,
        advisors,
        jury,
        groups,

        advisorCount: advisors.length,
        juryCount: jury.length,
        groupCount: groups.length,

        recentAuditLogs: data.recentAuditLogs ?? [],
    };
}

function getAuthHeaders() {
    const token = getToken();

    return {
        Authorization: `Bearer ${token ?? ""}`,
        "Content-Type": "application/json",
    };
}

function normalizeCommitteePage(data: any): CommitteePageResponse {
    const rawCommittees = data?.content ?? data?.data ?? [];
    const committees = rawCommittees.map((c: any) => normalizeCommitteeDetail(c));

    return {
        content: committees,
        totalPages: data?.totalPages ?? data?.pagination?.totalPages ?? 1,
        totalElements:
            data?.totalElements ??
            data?.pagination?.totalElements ??
            data?.count ??
            committees.length,
    };
}

function normalizeAuditLogPage(data: any): CommitteeAuditLogPageResponse {
    const logs = data?.content ?? data?.data ?? [];

    return {
        content: logs.map((log: any) => ({
            id: log.id ?? log.logId,
            timestamp: log.timestamp ?? log.createdAt,
            userName: log.userName ?? `User #${log.userId ?? "-"}`,
            action: log.action ?? log.actionType,
            entityType: log.entityType,
            description: log.description,
        })),
        totalPages: data?.totalPages ?? data?.pagination?.totalPages ?? 1,
        totalElements:
            data?.totalElements ??
            data?.pagination?.totalElements ??
            data?.count ??
            logs.length,
    };
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
        sort: sort,
    });

    if (status !== "ALL") params.set("status", status);
    if (search) params.set("search", search);

    const res = await fetch(`${API_BASE}/committees?${params.toString()}`, {
        headers: { Authorization: `Bearer ${token ?? ""}` },
        cache: "no-store",
    });

    if (!res.ok) throw new Error(await parseError(res));
    return normalizeCommitteePage(await res.json());
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
        sort: sort
    });

    if (status !== "ALL") params.set("status", status);
    if (search) params.set("search", search);

    const res = await fetch(`${API_BASE}/committees?${params.toString()}`, {
        headers: { Authorization: `Bearer ${token ?? ""}` },
        cache: "no-store",
    });

    if (!res.ok) throw new Error(await parseError(res));
    return normalizeCommitteePage(await res.json());
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
): Promise<CommitteeDetail> {
    if (USE_MOCK) {
        const committee = mockCommittees.find(
            (item) => item.committeeId === committeeId
        );

        if (!committee) {
            throw new Error("Committee not found.");
        }

        const nested = mockCommitteeDetails[committeeId] ?? {
            advisors: [],
            jury: [],
            groups: [],
            recentAuditLogs: [],
        };

        return {
            ...committee,
            ...nested,
        };
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

    return normalizeCommitteeDetail(await res.json());
}

export async function fetchCommitteeAuditLogs(
    page = 0,
    size = 20,
    action = "ALL",
    entityType = "ALL",
    fromDate = "",
    toDate = ""
): Promise<CommitteeAuditLogPageResponse> {
    if (USE_MOCK) {
        let result = [...mockAuditLogs];

        if (action !== "ALL") {
            result = result.filter((log) => log.action === action);
        }

        if (entityType !== "ALL") {
            result = result.filter((log) => log.entityType === entityType);
        }

        if (fromDate) {
            result = result.filter(
                (log) => new Date(log.timestamp) >= new Date(fromDate)
            );
        }

        if (toDate) {
            result = result.filter(
                (log) => new Date(log.timestamp) <= new Date(toDate)
            );
        }

        result.sort(
            (a, b) =>
                new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
        );

        const totalElements = result.length;
        const totalPages = Math.max(Math.ceil(totalElements / size), 1);
        const start = page * size;

        return {
            content: result.slice(start, start + size),
            totalPages,
            totalElements,
        };
    }

    const token = getToken();
    const params = new URLSearchParams({
        page: String(page),
        size: String(size),
    });

    if (action !== "ALL") params.set("actionType", action);
    if (entityType !== "ALL") params.set("entityType", entityType);
    if (fromDate) params.set("startDate", `${fromDate}T00:00:00Z`);
    if (toDate) params.set("endDate", `${toDate}T23:59:59Z`);

    const res = await fetch(
        `${API_BASE}/audit-logs?${params.toString()}`,
        {
            headers: {
                Authorization: `Bearer ${token ?? ""}`,
            },
            cache: "no-store",
        }
    );

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
        body: JSON.stringify({ advisorId, role }),
    });

    if (!res.ok) throw new Error(await parseError(res));
}

export async function removeAdvisor(
    committeeId: number,
    advisorId: number
): Promise<void> {
    const token = getToken();

    const res = await fetch(`${API_BASE}/committees/${committeeId}/advisors/${advisorId}`, {
        method: "DELETE",
        headers: {
            Authorization: `Bearer ${token ?? ""}`,
        },
    });

    if (!res.ok) throw new Error(await parseError(res));
}
