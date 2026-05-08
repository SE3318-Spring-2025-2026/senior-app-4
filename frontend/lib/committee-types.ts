export type CommitteeStatus = "FORMING" | "ACTIVE" | "INACTIVE" | "COMPLETED";

export type Committee = {
    committeeId: number;
    committeeName: string;
    description?: string | null;
    status: CommitteeStatus;
    createdBy: number;
    createdByName?: string;
    advisorCount?: number;
    juryCount?: number;
    groupCount?: number;
    createdAt?: string;
    updatedAt?: string;
};

/** Extended list item that may include member arrays from the API response */
export type CommitteeListItem = Committee & {
    advisors?: { userId: number; name: string; role: string }[];
    jury?: { userId: number; name: string; role: string }[];
};

export type CommitteeMemberAssignment = {
    id: number;       // committeeAdvisorId / committeeJuryId (row ID)
    userId: number;   // the professor's actual userId
    name: string;
    email: string;
    role: "ADVISOR" | "JURY";
    assignedAt: string;
};

export type CommitteeAssignedGroup = {
    groupId: number;
    groupName: string;
    membersCount: number;
    status: string;
    examDate?: string | null;
};

export type CommitteeAuditLog = {
    id: number;
    timestamp: string;
    userName: string;
    action: string;
    entityType: string;
    description: string;
};

export type CommitteeDetail = Committee & {
    advisors: CommitteeMemberAssignment[];
    jury: CommitteeMemberAssignment[];
    groups: CommitteeAssignedGroup[];
    recentAuditLogs: CommitteeAuditLog[];
};

export type CommitteeFormValues = {
    committeeName: string;
    description?: string;
    status: CommitteeStatus;
};

export type CommitteePageResponse = {
    content: CommitteeListItem[];
    totalPages: number;
    totalElements: number;
};

export type CommitteeAuditLogPageResponse = {
    content: CommitteeAuditLog[];
    totalPages: number;
    totalElements: number;
};
