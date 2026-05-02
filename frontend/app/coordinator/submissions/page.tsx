"use client";

import { useState, useEffect, useCallback, startTransition } from "react";
import Link from "next/link";
import { toast } from "sonner";
import { useAuthGuard } from "@/hooks/useAuthGuard";
import Sidebar from "@/components/Sidebar";
import {
  fetchSubmissions,
  type SubmissionSummary,
  type SubmissionFilters,
  type SubmissionStatus,
  type DeliverableType,
  type DeadlineStatus,
  type PaginationMeta,
} from "@/lib/submissions-api";

// ─── Auth shell ──────────────────────────────────────────────────────────────

export default function CoordinatorSubmissionsPage() {
  const authStatus = useAuthGuard("coordinator");
  if (authStatus === "loading") return <Spinner />;
  if (authStatus === "denied") return <AccessDenied />;
  return <SubmissionsDashboard />;
}

// ─── Main dashboard ───────────────────────────────────────────────────────────

function SubmissionsDashboard() {
  const [submissions, setSubmissions] = useState<SubmissionSummary[]>([]);
  const [pagination, setPagination] = useState<PaginationMeta | null>(null);
  const [loading, setLoading] = useState(true);

  const [filters, setFilters] = useState<SubmissionFilters>({
    status: "",
    deliverableType: "",
    deadlineStatus: "",
    page: 0,
    size: 20,
  });

  const load = useCallback(async (f: SubmissionFilters) => {
    setLoading(true);
    try {
      const res = await fetchSubmissions(f);
      setSubmissions(res.data ?? []);
      setPagination(res.pagination ?? null);
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Failed to load submissions.");
      setSubmissions([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(filters); }, [load, filters]);

  function setFilter<K extends keyof SubmissionFilters>(key: K, value: SubmissionFilters[K]) {
    setFilters((prev) => ({ ...prev, [key]: value, page: 0 }));
  }

  function clearFilters() {
    setFilters({ status: "", deliverableType: "", deadlineStatus: "", page: 0, size: 20 });
  }

  const overdueCount = submissions.filter((s) => s.isOverdue === true).length;
  const pendingCount = submissions.filter((s) => s.status === "PENDING_REVIEW").length;
  const gradedCount = submissions.filter((s) => s.status === "GRADED").length;

  return (
    <div className="min-h-screen bg-gray-950 flex">
      <Sidebar activePage="submissions" />
      <main className="flex-1 flex flex-col min-w-0">
        {/* Top bar */}
        <div className="border-b border-white/5 px-8 py-4 flex items-center justify-between shrink-0">
          <div>
            <h1 className="text-base font-semibold text-white">Submissions</h1>
            <p className="text-xs text-gray-500 mt-0.5">
              Monitor all group deliverables and submission statuses
            </p>
          </div>
          <button
            onClick={() => load(filters)}
            disabled={loading}
            className="flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs text-gray-400
                       hover:text-white hover:bg-white/5 border border-white/10 transition-all"
          >
            <svg
              className={`w-3.5 h-3.5 ${loading ? "animate-spin" : ""}`}
              fill="none" viewBox="0 0 24 24" stroke="currentColor"
            >
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            Refresh
          </button>
        </div>

        <div className="flex-1 p-8 space-y-6 overflow-y-auto">
          {/* Stat cards */}
          <div className="grid grid-cols-3 gap-4">
            <StatCard label="Overdue" value={overdueCount} color="red" />
            <StatCard label="Pending Review" value={pendingCount} color="amber" />
            <StatCard label="Graded" value={gradedCount} color="green" />
          </div>

          {/* Filters */}
          <div className="bg-gray-900 border border-white/8 rounded-2xl px-6 py-5">
            <div className="flex items-center justify-between mb-4">
              <p className="text-sm font-medium text-white">Filters</p>
              <button
                onClick={clearFilters}
                className="text-xs text-gray-500 hover:text-gray-300 transition-colors"
              >
                Clear all
              </button>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <FilterSelect
                label="Status"
                value={filters.status ?? ""}
                onChange={(v) => setFilter("status", v as SubmissionStatus | "")}
                options={[
                  { value: "", label: "All statuses" },
                  { value: "PENDING_REVIEW", label: "Pending Review" },
                  { value: "UNDER_REVIEW", label: "Under Review" },
                  { value: "REVISION_REQUESTED", label: "Revision Requested" },
                  { value: "APPROVED", label: "Approved" },
                  { value: "GRADED", label: "Graded" },
                ]}
              />
              <FilterSelect
                label="Deliverable Type"
                value={filters.deliverableType ?? ""}
                onChange={(v) => setFilter("deliverableType", v as DeliverableType | "")}
                options={[
                  { value: "", label: "All types" },
                  { value: "PROPOSAL", label: "Proposal" },
                  { value: "REVISED_PROPOSAL", label: "Revised Proposal" },
                  { value: "STATEMENT_OF_WORK", label: "Statement of Work" },
                ]}
              />
              <FilterSelect
                label="Deadline"
                value={filters.deadlineStatus ?? ""}
                onChange={(v) => setFilter("deadlineStatus", v as DeadlineStatus | "")}
                options={[
                  { value: "", label: "All deadlines" },
                  { value: "APPROACHING", label: "Approaching" },
                  { value: "OVERDUE", label: "Overdue" },
                ]}
              />
            </div>
          </div>

          {/* Table */}
          {loading ? (
            <div className="flex items-center justify-center h-48">
              <svg className="w-6 h-6 animate-spin text-blue-500" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
              </svg>
            </div>
          ) : submissions.length === 0 ? (
            <EmptyState />
          ) : (
            <div className="bg-gray-900 border border-white/8 rounded-2xl overflow-hidden">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-white/5 text-left">
                    <th className="px-5 py-3.5 text-xs font-medium text-gray-500 uppercase tracking-wider">Team</th>
                    <th className="px-5 py-3.5 text-xs font-medium text-gray-500 uppercase tracking-wider">Type</th>
                    <th className="px-5 py-3.5 text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                    <th className="px-5 py-3.5 text-xs font-medium text-gray-500 uppercase tracking-wider">Rev.</th>
                    <th className="px-5 py-3.5 text-xs font-medium text-gray-500 uppercase tracking-wider">Submitted</th>
                    <th className="px-5 py-3.5 text-xs font-medium text-gray-500 uppercase tracking-wider">Deadline</th>
                    <th className="px-5 py-3.5 text-xs font-medium text-gray-500 uppercase tracking-wider text-right">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/5">
                  {submissions.map((s) => (
                    <SubmissionRow key={s.id} submission={s} />
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {/* Pagination */}
          {pagination && pagination.totalPages > 1 && (
            <div className="flex items-center justify-between">
              <p className="text-xs text-gray-500">
                {pagination.totalElements} submission{pagination.totalElements !== 1 ? "s" : ""} total
              </p>
              <div className="flex items-center gap-2">
                <button
                  disabled={pagination.currentPage === 0}
                  onClick={() => setFilters((p) => ({ ...p, page: (p.page ?? 1) - 1 }))}
                  className="px-3 py-1.5 rounded-lg text-xs border border-white/10 text-gray-400
                             hover:text-white hover:bg-white/5 disabled:opacity-40 disabled:cursor-not-allowed transition-all"
                >
                  Previous
                </button>
                <span className="text-xs text-gray-500">
                  Page {pagination.currentPage + 1} of {pagination.totalPages}
                </span>
                <button
                  disabled={pagination.currentPage >= pagination.totalPages - 1}
                  onClick={() => setFilters((p) => ({ ...p, page: (p.page ?? 0) + 1 }))}
                  className="px-3 py-1.5 rounded-lg text-xs border border-white/10 text-gray-400
                             hover:text-white hover:bg-white/5 disabled:opacity-40 disabled:cursor-not-allowed transition-all"
                >
                  Next
                </button>
              </div>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}

// ─── Submission row ───────────────────────────────────────────────────────────

function SubmissionRow({ submission: s }: { submission: SubmissionSummary }) {
  const teamLabel = s.teamName ?? `Team #${s.teamId}`;
  return (
    <tr className={`transition-colors hover:bg-white/[0.02] ${s.isOverdue ? "bg-red-500/5" : ""}`}>
      <td className="px-5 py-4">
        <div className="flex items-center gap-2.5">
          {s.isOverdue && (
            <span title="Overdue">
              <svg className="w-3.5 h-3.5 text-red-400 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                  d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
              </svg>
            </span>
          )}
          <span className="font-medium text-white truncate max-w-[160px]">{teamLabel}</span>
        </div>
      </td>
      <td className="px-5 py-4">
        <DeliverableTypeBadge type={s.deliverableType} />
      </td>
      <td className="px-5 py-4">
        <SubmissionStatusBadge status={s.status} />
      </td>
      <td className="px-5 py-4 text-gray-400 text-center">
        {s.revisionNumber ?? "—"}
      </td>
      <td className="px-5 py-4 text-gray-400 whitespace-nowrap">
        {formatDate(s.submittedAt)}
      </td>
      <td className="px-5 py-4 whitespace-nowrap">
        {s.deadline ? (
          <span className={s.isOverdue ? "text-red-400 font-medium" : "text-gray-400"}>
            {formatDate(s.deadline)}
          </span>
        ) : (
          <span className="text-gray-600">—</span>
        )}
      </td>
      <td className="px-5 py-4 text-right">
        <Link
          href={`/coordinator/submissions/${s.id}`}
          className="inline-flex items-center rounded-lg border border-white/10 px-3 py-1.5 text-xs font-medium text-gray-300 transition hover:bg-white/5 hover:text-white"
        >
          View detail
        </Link>
      </td>
    </tr>
  );
}

// ─── Sub-components ───────────────────────────────────────────────────────────

function SubmissionStatusBadge({ status }: { status: SubmissionStatus }) {
  const styles: Record<string, string> = {
    PENDING_REVIEW:     "bg-yellow-500/15 text-yellow-400 border-yellow-500/20",
    UNDER_REVIEW:       "bg-blue-500/15 text-blue-400 border-blue-500/20",
    REVISION_REQUESTED: "bg-orange-500/15 text-orange-400 border-orange-500/20",
    APPROVED:           "bg-green-500/15 text-green-400 border-green-500/20",
    GRADED:             "bg-purple-500/15 text-purple-400 border-purple-500/20",
  };
  const labels: Record<string, string> = {
    PENDING_REVIEW:     "Pending Review",
    UNDER_REVIEW:       "Under Review",
    REVISION_REQUESTED: "Revision Requested",
    APPROVED:           "Approved",
    GRADED:             "Graded",
  };
  return (
    <span className={`inline-flex px-2.5 py-1 rounded-full text-xs font-medium border ${styles[status]}`}>
      {labels[status]}
    </span>
  );
}

function DeliverableTypeBadge({ type }: { type: DeliverableType }) {
  const labels: Record<string, string> = {
    PROPOSAL:           "Proposal",
    REVISED_PROPOSAL:   "Revised Proposal",
    STATEMENT_OF_WORK:  "SoW",
  };
  return (
    <span className="inline-flex px-2.5 py-1 rounded-full text-xs font-medium bg-white/8 text-gray-300 border border-white/10">
      {labels[type]}
    </span>
  );
}

function FilterSelect({
  label, value, onChange, options,
}: {
  label: string;
  value: string;
  onChange: (v: string) => void;
  options: { value: string; label: string }[];
}) {
  return (
    <div className="flex flex-col gap-1.5">
      <label className="text-xs font-medium text-gray-500">{label}</label>
      <select
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="rounded-xl border border-white/10 bg-gray-800 px-3 py-2.5 text-sm text-white
                   focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 transition"
      >
        {options.map((o) => (
          <option key={o.value} value={o.value}>{o.label}</option>
        ))}
      </select>
    </div>
  );
}

function StatCard({ label, value, color }: { label: string; value: number; color: "red" | "amber" | "green" }) {
  const colors = {
    red:   { text: "text-red-400",   bg: "bg-red-500/10",   border: "border-red-500/20" },
    amber: { text: "text-amber-400", bg: "bg-amber-500/10", border: "border-amber-500/20" },
    green: { text: "text-green-400", bg: "bg-green-500/10", border: "border-green-500/20" },
  };
  const c = colors[color];
  return (
    <div className={`${c.bg} border ${c.border} rounded-2xl px-5 py-4 text-center`}>
      <p className={`text-3xl font-bold ${c.text}`}>{value}</p>
      <p className="text-xs text-gray-500 mt-1">{label}</p>
    </div>
  );
}

function EmptyState() {
  return (
    <div className="flex flex-col items-center justify-center h-48 space-y-4">
      <div className="w-14 h-14 rounded-2xl bg-gray-800 border border-white/8 flex items-center justify-center">
        <svg className="w-6 h-6 text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
            d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" />
        </svg>
      </div>
      <div className="text-center">
        <p className="text-sm font-semibold text-white">No submissions found</p>
        <p className="text-xs text-gray-500 mt-1">Try adjusting your filters.</p>
      </div>
    </div>
  );
}

function Spinner() {
  return (
    <div className="min-h-screen bg-gray-950 flex items-center justify-center">
      <svg className="w-6 h-6 animate-spin text-blue-500" fill="none" viewBox="0 0 24 24">
        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
      </svg>
    </div>
  );
}

function AccessDenied() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-950">
      <div className="text-center space-y-4">
        <div className="w-16 h-16 rounded-2xl bg-red-500/10 border border-red-500/20 flex items-center justify-center mx-auto">
          <svg className="w-7 h-7 text-red-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
              d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
          </svg>
        </div>
        <h1 className="text-lg font-semibold text-white">Access Restricted</h1>
        <p className="text-sm text-gray-500">Only Coordinators can access this page.</p>
      </div>
    </div>
  );
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

function formatDate(iso: string) {
  return new Date(iso).toLocaleString("en-US", {
    day: "numeric",
    month: "short",
    year: "numeric",
  });
}
