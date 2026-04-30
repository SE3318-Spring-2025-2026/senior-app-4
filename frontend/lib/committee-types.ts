export type CommitteeStatus = "FORMING" | "ACTIVE" | "INACTIVE" | "COMPLETED";

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