export type CommitteeStatus = "ACTIVE" | "INACTIVE" | "COMPLETED";

export type Committee = {
    committeeId: number;
    committeeName: string;
    description?: string | null;
    status: CommitteeStatus;
    createdBy: number;
    advisorCount?: number;
    juryCount?: number;
    groupCount?: number;
    createdAt?: string;
    updatedAt?: string;
};

export type CommitteeMemberAssignment = {
    id: number;
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
    content: Committee[];
    totalPages: number;
    totalElements: number;
};

export type CommitteeAuditLogPageResponse = {
    content: CommitteeAuditLog[];
    totalPages: number;
    totalElements: number;
};

export type CommitteeValidationRules = {
    sizeRequirements: string[];
    advisorQualifications: string[];
    scheduleRules: string[];
    groupAssignmentRules: string[];
};

export type AdvisorRole = "PRESIDENT" | "VICE_PRESIDENT" | "MEMBER";
export type AdvisorAssignmentStatus = "PENDING" | "ACCEPTED" | "REJECTED";

export type AdvisorAssignment = {
    assignmentId: number;
    committeeId: number;
    advisorId: number;
    fullName: string;
    email: string;
    role: AdvisorRole;
    assignedAt: string;
    status: AdvisorAssignmentStatus;
};

export type AdvisorAssignmentFormValues = {
    advisorId: number;
    role: AdvisorRole;
};

export type JuryType = "CORE" | "SUBSTITUTE";

export type JuryAssignment = {
    assignmentId: number;
    committeeId: number;
    juryId: number;
    fullName: string;
    email: string;
    juryType: JuryType;
    assignedAt: string;
};

export type JuryAssignmentFormValues = {
    juryId: number;
    juryType: JuryType;
};

export type CommitteeAssignmentDetail = {
    advisors: AdvisorAssignment[];
    juryMembers: JuryAssignment[];
};