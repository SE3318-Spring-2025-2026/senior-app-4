'use client';

import { useState, useCallback, useEffect } from 'react';
import { ChevronLeft, ChevronRight, Download, Filter, RefreshCw } from 'lucide-react';
import { toast } from 'sonner';
import { format } from 'date-fns';
import Sidebar from '@/components/Sidebar';
import { getUser, getToken } from '@/lib/auth';
import { useRouter } from 'next/navigation';
import { getAuditLogs, type AuditLog, type AuditLogFilterParams, type PaginationInfo } from '@/lib/audit-logs-api';

const ACTION_OPTIONS = [
  { value: 'GROUP_CREATED', label: 'Group Created' },
  { value: 'GROUP_UPDATED', label: 'Group Updated' },
  { value: 'GROUP_DISBANDED', label: 'Group Disbanded' },
  { value: 'MEMBER_ADDED', label: 'Member Added' },
  { value: 'MEMBER_REMOVED', label: 'Member Removed' },
  { value: 'ADVISOR_ASSIGNED', label: 'Advisor Assigned' },
  { value: 'ADVISOR_REQUESTED', label: 'Advisor Requested' },
  { value: 'COMMITTEE_CREATED', label: 'Committee Created' },
  { value: 'JURY_ASSIGNED', label: 'Jury Assigned' },
];

const ACTION_COLORS: Record<string, string> = {
  GROUP_CREATED: 'bg-green-500/15 text-green-400 border-green-500/20',
  GROUP_UPDATED: 'bg-blue-500/15 text-blue-400 border-blue-500/20',
  GROUP_DISBANDED: 'bg-red-500/15 text-red-400 border-red-500/20',
  MEMBER_ADDED: 'bg-teal-500/15 text-teal-400 border-teal-500/20',
  MEMBER_REMOVED: 'bg-orange-500/15 text-orange-400 border-orange-500/20',
  ADVISOR_ASSIGNED: 'bg-purple-500/15 text-purple-400 border-purple-500/20',
  ADVISOR_REQUESTED: 'bg-indigo-500/15 text-indigo-400 border-indigo-500/20',
  COMMITTEE_CREATED: 'bg-cyan-500/15 text-cyan-400 border-cyan-500/20',
  JURY_ASSIGNED: 'bg-amber-500/15 text-amber-400 border-amber-500/20',
};

function TableRows({ loading, logs }: Readonly<{ loading: boolean; logs: AuditLog[] }>) {
  if (loading) {
    return (
      <tr>
        <td colSpan={5} className="px-5 py-12 text-center">
          <RefreshCw className="mx-auto h-6 w-6 animate-spin text-blue-500" />
          <p className="mt-2 text-sm text-gray-500">Loading logs…</p>
        </td>
      </tr>
    );
  }
  if (logs.length === 0) {
    return (
      <tr>
        <td colSpan={5} className="px-5 py-12 text-center text-sm text-gray-500">
          No audit logs found matching the filters.
        </td>
      </tr>
    );
  }
  return logs.map((log) => (
    <tr key={log.id} className="hover:bg-white/[0.02] transition-colors">
      <td className="px-5 py-3 whitespace-nowrap text-xs text-gray-400">
        {format(new Date(log.createdAt), 'MMM d, yyyy HH:mm:ss')}
      </td>
      <td className="px-5 py-3 whitespace-nowrap text-sm font-medium text-white">
        {log.userId}
      </td>
      <td className="px-5 py-3 whitespace-nowrap">
        <span className={`inline-flex rounded-full border px-2.5 py-0.5 text-xs font-medium ${ACTION_COLORS[log.actionType] ?? 'bg-gray-500/15 text-gray-400 border-gray-500/20'}`}>
          {log.actionType.replaceAll('_', ' ')}
        </span>
      </td>
      <td className="px-5 py-3 whitespace-nowrap text-sm">
        <EntityCell log={log} />
      </td>
      <td className="px-5 py-3 max-w-xs text-sm text-gray-400 truncate" title={log.eventDetails}>
        {log.eventDetails || '—'}
      </td>
    </tr>
  ));
}

function EntityCell({ log }: Readonly<{ log: AuditLog }>) {
  if (log.groupId) return <span className="text-purple-400">Group {log.groupId}</span>;
  if (log.committeeId) return <span className="text-amber-400">Committee {log.committeeId}</span>;
  return <span className="text-gray-600">—</span>;
}

export default function AuditLogsPage() {
  const router = useRouter();

  useEffect(() => {
    const token = getToken();
    const user = getUser();
    if (!token || !user) { router.replace('/auth/login'); return; }
    if (user.requiresPasswordChange) { router.replace('/auth/change-password'); return; }
    if (user.role !== 'coordinator') { router.replace('/auth/login'); }
  }, [router]);

  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [pagination, setPagination] = useState<PaginationInfo | null>(null);
  const [loading, setLoading] = useState(true);
  const [exporting, setExporting] = useState(false);

  const [actionType, setActionType] = useState('');
  const [entityType, setEntityType] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [page, setPage] = useState(0);
  const size = 20;

  const buildParams = useCallback((overrides: Partial<AuditLogFilterParams> = {}): AuditLogFilterParams => {
    const params: AuditLogFilterParams = { page, size, sort: 'createdAt,desc', ...overrides };
    if (actionType) params.actionType = actionType;
    if (entityType) params.entityType = entityType;
    if (startDate) params.startDate = new Date(startDate).toISOString();
    if (endDate) {
      const end = new Date(endDate);
      end.setHours(23, 59, 59, 999);
      params.endDate = end.toISOString();
    }
    return params;
  }, [page, size, actionType, entityType, startDate, endDate]);

  const fetchLogs = useCallback(async () => {
    setLoading(true);
    try {
      const response = await getAuditLogs(buildParams());
      setLogs(response.data);
      setPagination(response.pagination);
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Failed to load audit logs.');
    } finally {
      setLoading(false);
    }
  }, [buildParams]);

  useEffect(() => { fetchLogs(); }, [fetchLogs]);

  const handleExport = async () => {
    setExporting(true);
    try {
      const allData: AuditLog[] = [];
      let currentPage = 0;
      let totalPages = 1;
      while (currentPage < totalPages) {
        const response = await getAuditLogs(buildParams({ page: currentPage, size: 100 }));
        allData.push(...response.data);
        totalPages = response.pagination.totalPages;
        currentPage += 1;
      }
      const data = allData;
      if (!data.length) { toast.error('No data to export.'); return; }

      const headers = ['Timestamp', 'User ID', 'Action Type', 'Entity', 'Details', 'IP Address'];
      const rows = data.map(log => {
        let entity = 'None';
        if (log.groupId) entity = `Group ${log.groupId}`;
        else if (log.committeeId) entity = `Committee ${log.committeeId}`;
        return [
          `"${format(new Date(log.createdAt), 'yyyy-MM-dd HH:mm:ss')}"`,
          `"${log.userId}"`,
          `"${log.actionType}"`,
          `"${entity}"`,
          `"${(log.eventDetails || '').replaceAll('"', '""')}"`,
          `"${log.ipAddress || ''}"`,
        ].join(',');
      });

      const blob = new Blob([[headers.join(','), ...rows].join('\n')], { type: 'text/csv;charset=utf-8;' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `audit-logs-${format(new Date(), 'yyyy-MM-dd')}.csv`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(url);
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Export failed.');
    } finally {
      setExporting(false);
    }
  };

  const resetFilters = () => {
    setActionType(''); setEntityType(''); setStartDate(''); setEndDate(''); setPage(0);
  };

  return (
    <div className="flex min-h-screen bg-gray-950 text-white">
      <Sidebar activePage="audit-logs" />
      <main className="flex min-w-0 flex-1 flex-col">
        {/* Header */}
        <div className="flex shrink-0 items-center justify-between border-b border-white/5 px-8 py-4">
          <div>
            <h1 className="text-base font-semibold text-white">Audit Logs</h1>
            <p className="mt-0.5 text-xs text-gray-500">System-wide activity records for all coordinator-level actions</p>
          </div>
          <button
            onClick={handleExport}
            disabled={exporting || loading || logs.length === 0}
            className="inline-flex items-center gap-2 rounded-lg border border-white/10 px-3 py-1.5 text-xs text-gray-400 transition hover:bg-white/5 hover:text-white disabled:cursor-not-allowed disabled:opacity-50"
          >
            {exporting ? <RefreshCw className="h-3.5 w-3.5 animate-spin" /> : <Download className="h-3.5 w-3.5" />}
            Export CSV
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-8 space-y-6">
          {/* Filters */}
          <section className="rounded-2xl border border-white/8 bg-gray-900 p-5">
            <div className="mb-4 flex items-center gap-2">
              <Filter className="h-4 w-4 text-gray-400" />
              <span className="text-sm font-medium text-white">Filters</span>
            </div>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <div>
                <label htmlFor="filter-action" className="mb-1.5 block text-xs text-gray-400">Action Type</label>
                <select
                  id="filter-action"
                  value={actionType}
                  onChange={(e) => { setActionType(e.target.value); setPage(0); }}
                  className="w-full rounded-lg border border-white/10 bg-gray-800 px-3 py-2 text-sm text-white outline-none focus:border-blue-500/50"
                >
                  <option value="">All Actions</option>
                  {ACTION_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
                </select>
              </div>
              <div>
                <label htmlFor="filter-entity" className="mb-1.5 block text-xs text-gray-400">Entity Type</label>
                <select
                  id="filter-entity"
                  value={entityType}
                  onChange={(e) => { setEntityType(e.target.value); setPage(0); }}
                  className="w-full rounded-lg border border-white/10 bg-gray-800 px-3 py-2 text-sm text-white outline-none focus:border-blue-500/50"
                >
                  <option value="">All Entities</option>
                  <option value="GROUP">Group</option>
                  <option value="COMMITTEE">Committee</option>
                </select>
              </div>
              <div>
                <label htmlFor="filter-start" className="mb-1.5 block text-xs text-gray-400">Start Date</label>
                <input
                  id="filter-start"
                  type="date"
                  value={startDate}
                  onChange={(e) => { setStartDate(e.target.value); setPage(0); }}
                  className="w-full rounded-lg border border-white/10 bg-gray-800 px-3 py-2 text-sm text-white outline-none focus:border-blue-500/50 [color-scheme:dark]"
                />
              </div>
              <div>
                <label htmlFor="filter-end" className="mb-1.5 block text-xs text-gray-400">End Date</label>
                <input
                  id="filter-end"
                  type="date"
                  value={endDate}
                  onChange={(e) => { setEndDate(e.target.value); setPage(0); }}
                  className="w-full rounded-lg border border-white/10 bg-gray-800 px-3 py-2 text-sm text-white outline-none focus:border-blue-500/50 [color-scheme:dark]"
                />
              </div>
            </div>
            <div className="mt-4 flex justify-end">
              <button onClick={resetFilters} className="text-xs text-blue-400 hover:text-blue-300 transition-colors">
                Clear filters
              </button>
            </div>
          </section>

          {/* Table */}
          <section className="rounded-2xl border border-white/8 bg-gray-900 overflow-hidden">
            <div className="overflow-x-auto">
              <table className="min-w-full">
                <thead>
                  <tr className="border-b border-white/5">
                    <th className="px-5 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">Timestamp</th>
                    <th className="px-5 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">User ID</th>
                    <th className="px-5 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">Action</th>
                    <th className="px-5 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">Entity</th>
                    <th className="px-5 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">Details</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/5">
                  <TableRows loading={loading} logs={logs} />
                </tbody>
              </table>
            </div>

            {/* Pagination */}
            {!loading && pagination && pagination.totalPages > 0 && (
              <div className="flex items-center justify-between border-t border-white/5 px-5 py-3">
                <p className="text-xs text-gray-500">
                  Showing{' '}
                  <span className="text-white">{page * size + 1}–{Math.min((page + 1) * size, pagination.totalElements)}</span>
                  {' '}of{' '}
                  <span className="text-white">{pagination.totalElements}</span> results
                </p>
                <div className="flex items-center gap-1">
                  <button
                    onClick={() => setPage(p => Math.max(0, p - 1))}
                    disabled={page === 0}
                    className="rounded-lg border border-white/10 p-1.5 text-gray-400 hover:bg-white/5 hover:text-white disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
                  >
                    <ChevronLeft className="h-4 w-4" />
                  </button>
                  <span className="px-3 text-xs text-gray-400">
                    Page {page + 1} of {pagination.totalPages}
                  </span>
                  <button
                    onClick={() => setPage(p => Math.min(pagination.totalPages - 1, p + 1))}
                    disabled={page >= pagination.totalPages - 1}
                    className="rounded-lg border border-white/10 p-1.5 text-gray-400 hover:bg-white/5 hover:text-white disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
                  >
                    <ChevronRight className="h-4 w-4" />
                  </button>
                </div>
              </div>
            )}
          </section>
        </div>
      </main>
    </div>
  );
}
