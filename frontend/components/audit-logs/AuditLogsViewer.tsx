'use client';

import React, { useState, useEffect, useCallback } from 'react';
import { Download, Filter, Search, ChevronLeft, ChevronRight, RefreshCw } from 'lucide-react';
import { getAuditLogs, AuditLog, AuditLogFilterParams, PaginationInfo } from '@/lib/audit-logs-api';
import { format } from 'date-fns';

export default function AuditLogsViewer() {
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [pagination, setPagination] = useState<PaginationInfo | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [exporting, setExporting] = useState<boolean>(false);

  // Filter states
  const [actionType, setActionType] = useState<string>('');
  const [entityType, setEntityType] = useState<string>('');
  const [startDate, setStartDate] = useState<string>('');
  const [endDate, setEndDate] = useState<string>('');

  // Pagination states
  const [page, setPage] = useState<number>(0);
  const [size, setSize] = useState<number>(20);

  const fetchLogs = useCallback(async (isExport = false) => {
    try {
      if (!isExport) setLoading(true);
      
      const params: AuditLogFilterParams = {
        page: isExport ? 0 : page,
        size: isExport ? 10000 : size,
        sort: 'createdAt,desc',
      };

      if (actionType) params.actionType = actionType;
      if (entityType) params.entityType = entityType;
      if (startDate) params.startDate = new Date(startDate).toISOString();
      if (endDate) {
        // Set end date to end of day
        const end = new Date(endDate);
        end.setHours(23, 59, 59, 999);
        params.endDate = end.toISOString();
      }

      const response = await getAuditLogs(params);
      
      if (isExport) {
        return response.data;
      } else {
        setLogs(response.data);
        setPagination(response.pagination);
      }
    } catch (err) {
      console.error('Failed to fetch logs:', err);
    } finally {
      if (!isExport) setLoading(false);
    }
  }, [page, size, actionType, entityType, startDate, endDate]);

  useEffect(() => {
    fetchLogs();
  }, [fetchLogs]);

  const handleExport = async () => {
    try {
      setExporting(true);
      const exportData = await fetchLogs(true);
      
      if (!exportData || exportData.length === 0) {
        alert('No data to export.');
        return;
      }

      const headers = ['Timestamp', 'User ID', 'Action Type', 'Entity', 'Details', 'IP Address'];
      const csvContent = [
        headers.join(','),
        ...exportData.map(log => {
          let entity = 'None';
          if (log.groupId) entity = `Group ${log.groupId}`;
          if (log.committeeId) entity = `Committee ${log.committeeId}`;

          return [
            `"${format(new Date(log.createdAt), 'yyyy-MM-dd HH:mm:ss')}"`,
            `"${log.userId}"`,
            `"${log.actionType}"`,
            `"${entity}"`,
            `"${(log.eventDetails || '').replace(/"/g, '""')}"`,
            `"${log.ipAddress || ''}"`
          ].join(',');
        })
      ].join('\n');

      const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.setAttribute('href', url);
      link.setAttribute('download', `audit-logs-${format(new Date(), 'yyyy-MM-dd')}.csv`);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    } catch (err) {
      console.error('Export failed:', err);
    } finally {
      setExporting(false);
    }
  };

  const resetFilters = () => {
    setActionType('');
    setEntityType('');
    setStartDate('');
    setEndDate('');
    setPage(0);
  };

  return (
    <div className="flex flex-col space-y-4 w-full">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center space-y-4 md:space-y-0">
        <h1 className="text-2xl font-bold text-gray-900">System Audit Logs</h1>
        <button
          onClick={handleExport}
          disabled={exporting || loading || logs.length === 0}
          className="flex items-center px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700 disabled:opacity-50 transition-colors"
        >
          {exporting ? <RefreshCw className="w-4 h-4 mr-2 animate-spin" /> : <Download className="w-4 h-4 mr-2" />}
          Export CSV
        </button>
      </div>

      <div className="bg-white p-4 rounded-lg shadow border border-gray-200">
        <div className="flex items-center mb-4 text-gray-700 font-medium">
          <Filter className="w-5 h-5 mr-2 text-gray-500" />
          Filters
        </div>
        
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Action Type</label>
            <select
              value={actionType}
              onChange={(e) => { setActionType(e.target.value); setPage(0); }}
              className="w-full border border-gray-300 rounded-md p-2 focus:ring-indigo-500 focus:border-indigo-500"
            >
              <option value="">All Actions</option>
              <option value="GROUP_CREATED">Group Created</option>
              <option value="GROUP_UPDATED">Group Updated</option>
              <option value="GROUP_DISBANDED">Group Disbanded</option>
              <option value="MEMBER_ADDED">Member Added</option>
              <option value="MEMBER_REMOVED">Member Removed</option>
              <option value="ADVISOR_ASSIGNED">Advisor Assigned</option>
              <option value="ADVISOR_REQUESTED">Advisor Requested</option>
              <option value="COMMITTEE_CREATED">Committee Created</option>
              <option value="JURY_ASSIGNED">Jury Assigned</option>
              {/* Add other ActionType enum values as needed */}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Entity Type</label>
            <select
              value={entityType}
              onChange={(e) => { setEntityType(e.target.value); setPage(0); }}
              className="w-full border border-gray-300 rounded-md p-2 focus:ring-indigo-500 focus:border-indigo-500"
            >
              <option value="">All Entities</option>
              <option value="GROUP">Group</option>
              <option value="COMMITTEE">Committee</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Start Date</label>
            <input
              type="date"
              value={startDate}
              onChange={(e) => { setStartDate(e.target.value); setPage(0); }}
              className="w-full border border-gray-300 rounded-md p-2 focus:ring-indigo-500 focus:border-indigo-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">End Date</label>
            <input
              type="date"
              value={endDate}
              onChange={(e) => { setEndDate(e.target.value); setPage(0); }}
              className="w-full border border-gray-300 rounded-md p-2 focus:ring-indigo-500 focus:border-indigo-500"
            />
          </div>
        </div>

        <div className="mt-4 flex justify-end">
          <button
            onClick={resetFilters}
            className="text-sm text-indigo-600 hover:text-indigo-800 font-medium"
          >
            Clear Filters
          </button>
        </div>
      </div>

      <div className="bg-white shadow rounded-lg overflow-hidden border border-gray-200">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Timestamp</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">User ID</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Action</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Entity</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Details</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {loading ? (
                <tr>
                  <td colSpan={5} className="px-6 py-10 text-center text-gray-500">
                    <RefreshCw className="w-6 h-6 animate-spin mx-auto text-indigo-500" />
                    <p className="mt-2">Loading logs...</p>
                  </td>
                </tr>
              ) : logs.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-6 py-10 text-center text-gray-500">
                    No audit logs found matching the filters.
                  </td>
                </tr>
              ) : (
                logs.map((log) => (
                  <tr key={log.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {format(new Date(log.createdAt), 'MMM d, yyyy HH:mm:ss')}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900 font-medium">
                      {log.userId}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-blue-100 text-blue-800">
                        {log.actionType}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {log.groupId ? (
                        <span className="text-purple-600">Group {log.groupId}</span>
                      ) : log.committeeId ? (
                        <span className="text-orange-600">Committee {log.committeeId}</span>
                      ) : (
                        <span className="text-gray-400">-</span>
                      )}
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-500 max-w-xs truncate" title={log.eventDetails}>
                      {log.eventDetails}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Controls */}
        {!loading && pagination && pagination.totalPages > 0 && (
          <div className="bg-white px-4 py-3 flex items-center justify-between border-t border-gray-200 sm:px-6">
            <div className="hidden sm:flex-1 sm:flex sm:items-center sm:justify-between">
              <div>
                <p className="text-sm text-gray-700">
                  Showing <span className="font-medium">{page * size + 1}</span> to{' '}
                  <span className="font-medium">
                    {Math.min((page + 1) * size, pagination.totalElements)}
                  </span>{' '}
                  of <span className="font-medium">{pagination.totalElements}</span> results
                </p>
              </div>
              <div>
                <nav className="relative z-0 inline-flex rounded-md shadow-sm -space-x-px" aria-label="Pagination">
                  <button
                    onClick={() => setPage(p => Math.max(0, p - 1))}
                    disabled={page === 0}
                    className="relative inline-flex items-center px-2 py-2 rounded-l-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:bg-gray-100 disabled:text-gray-400"
                  >
                    <span className="sr-only">Previous</span>
                    <ChevronLeft className="h-5 w-5" aria-hidden="true" />
                  </button>
                  <span className="relative inline-flex items-center px-4 py-2 border border-gray-300 bg-white text-sm font-medium text-gray-700">
                    Page {page + 1} of {pagination.totalPages}
                  </span>
                  <button
                    onClick={() => setPage(p => Math.min(pagination.totalPages - 1, p + 1))}
                    disabled={page >= pagination.totalPages - 1}
                    className="relative inline-flex items-center px-2 py-2 rounded-r-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:bg-gray-100 disabled:text-gray-400"
                  >
                    <span className="sr-only">Next</span>
                    <ChevronRight className="h-5 w-5" aria-hidden="true" />
                  </button>
                </nav>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
