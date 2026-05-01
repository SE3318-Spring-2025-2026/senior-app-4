import apiClient from './client';

export interface CommitteeAdvisor {
    committeeAdvisorId: number;
    role: string;
    advisor: {
        userId: number;
        fullName: string;
        email: string;
    };
}

export interface Committee {
    committeeId: number;
    committeeName: string;
    description: string;
    status: string;
    advisors: CommitteeAdvisor[];
}

export const fetchCommittees = async (): Promise<Committee[]> => {
    const response = await apiClient.get('/committees');
    const data = response.data;
    if (Array.isArray(data)) return data;
    if (data && Array.isArray(data.data)) return data.data;
    if (data && Array.isArray(data.content)) return data.content;
    return [];
};

export const fetchCommitteeById = async (id: number): Promise<Committee> => {
    const response = await apiClient.get(`/committees/${id}`);
    const data = response.data;
    return data?.data || data;
};

export const createCommittee = async (data: { committeeName: string; description: string }): Promise<Committee> => {
    const response = await apiClient.post('/committees', data);
    return response.data;
};

export const deleteCommittee = async (id: number): Promise<void> => {
    await apiClient.delete(`/committees/${id}`);
};

export const assignAdvisor = async (committeeId: number, advisorId: number, role: string): Promise<void> => {
    await apiClient.post(`/committees/${committeeId}/advisors`, {
        advisorId,
        role
    });
};

export const removeAdvisor = async (committeeId: number, advisorId: number): Promise<void> => {
    await apiClient.delete(`/committees/${committeeId}/advisors/${advisorId}`);
};
