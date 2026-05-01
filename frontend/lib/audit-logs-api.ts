import apiClient from './client';
import { showToast } from '@/components/toast/ToastContext';

export interface AuditLog {
  id: number;
  userId: number;
  actionType: string;
  eventDetails: string;
  groupId: number | null;
  committeeId: number | null;
  ipAddress: string | null;
  createdAt: string;
}

export interface PaginationInfo {
  pageNumber: number;
  totalPages: number;
  totalElements: number;
  pageSize: number;
}

export interface AuditLogListResponse {
  status: string;
  data: AuditLog[];
  pagination: PaginationInfo;
}

export interface AuditLogFilterParams {
  page?: number;
  size?: number;
  sort?: string;
  actionType?: string;
  entityType?: string;
  startDate?: string;
  endDate?: string;
}

export const getAuditLogs = async (params: AuditLogFilterParams = {}): Promise<AuditLogListResponse> => {
  try {
    const response = await apiClient.get('/audit-logs', { params });
    return response.data;
  } catch (error: any) {
    console.error('Failed to fetch audit logs:', error);
    showToast('Failed to fetch audit logs', 'error');
    throw error;
  }
};
