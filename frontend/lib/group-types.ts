export type GroupStatus = "forming" | "formed" | "advised" | "disbanded";

export type UserRole = "student" | "leader" | "professor" | "coordinator";

export type GroupUser = {
    userId: number;
    studentId: string;
    fullName: string;
    role: UserRole;
    githubUsername?: string | null;
};

export type Group = {
    groupId: number;
    groupName: string;
    status: GroupStatus;
    leaderId: number;
    leaderName: string;
    advisorId?: number | null;
    advisorName?: string | null;
    memberCount: number;
    members: GroupUser[];
    githubBound: boolean;
    jiraBound: boolean;
    createdAt: string;
    updatedAt: string;
};