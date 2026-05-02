"use client";

import React, { useCallback, useEffect, useMemo, useState } from "react";
import { ChevronLeft, ChevronRight, RefreshCw, AlertTriangle } from "lucide-react";
import { format } from "date-fns";
import {
  AuditLog,
  AuditLogFilterParams,
  PaginationInfo,
  getAuditLogs,
} from "@/lib/audit-logs-api";

type Severity = "success" | "warning" | "error";

const ERROR_ACTIONS = new Set<string>([
  "GROUP_DISBANDED",
  "MEMBER_REMOVED",
  "ADVISOR_REJECTED",
  "COMMITTEE_DELETED",
  "COMMITTEE_GROUP_REMOVED",
  "SCHEDULE_CONFLICT_DETECTED",
]);

const WARNING_ACTIONS = new Set<string>([
  "ADVISOR_REQUEST_CANCELLED",
  "ADVISOR_REQUEST_WITHDRAWN",
  "ADVISOR_RELEASED",
  "ADVISOR_OVERRIDDEN",
  "INTEGRATION_REMOVED",
  "COMMITTEE_GROUP_STATUS_UPDATED",
]);

function classifySeverity(actionType: string): Severity {
  if (ERROR_ACTIONS.has(actionType)) return "error";
  if (WARNING_ACTIONS.has(actionType)) return "warning";
  return "success";
}

function severityBadgeClasses(sev: Severity): string {
  switch (sev) {
    case "error":
      return "bg-red-500/10 text-red-300 border-red-500/30";
    case "warning":
      return "bg-amber-500/10 text-amber-300 border-amber-500/30";
    case "success":
    default:
      return "bg-emerald-500/10 text-emerald-300 border-emerald-500/30";
  }
}

function buildSystemMessage(log: AuditLog): string {
  if (log.eventDetails && log.eventDetails.trim().length > 0) {
    return log.eventDetails;
  }
  const target =
    log.groupId != null
      ? `group ${log.groupId}`
      : log.committeeId != null
        ? `committee ${log.committeeId}`
        : null;
  const subject = log.actionType.toLowerCase().replaceAll("_", " ");
  return target ? `User ${log.userId} · ${subject} on ${target}` : `User ${log.userId} · ${subject}`;
}

const PAGE_SIZE = 10;

export default function SystemLogsTable() {
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [pagination, setPagination] = useState<PaginationInfo | null>(null);
  const [page, setPage] = useState<number>(0);
  const [errorsOnly, setErrorsOnly] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(true);
  const [forbidden, setForbidden] = useState<boolean>(false);
  const [fetchError, setFetchError] = useState<string | null>(null);

  const fetchLogs = useCallback(async () => {
    setLoading(true);
    setFetchError(null);
    setForbidden(false);
    try {
      const params: AuditLogFilterParams = {
        page,
        size: PAGE_SIZE,
        sort: "createdAt,desc",
      };
      const response = await getAuditLogs(params);
      setLogs(response.data);
      setPagination(response.pagination);
    } catch (err: unknown) {
      const status =
        typeof err === "object" && err !== null && "response" in err
          ? (err as { response?: { status?: number } }).response?.status
          : undefined;
      if (status === 403) {
        setForbidden(true);
      } else {
        setFetchError("Unable to load system logs. Please try again.");
      }
      setLogs([]);
      setPagination(null);
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => {
    fetchLogs();
  }, [fetchLogs]);

  const visibleLogs = useMemo(() => {
    if (!errorsOnly) return logs;
    return logs.filter((log) => classifySeverity(log.actionType) !== "success");
  }, [logs, errorsOnly]);

  const totalPages = pagination?.totalPages ?? 0;
  const canPrev = page > 0 && !loading;
  const canNext = page + 1 < totalPages && !loading;

  return (
    <div className="rounded-2xl border border-white/5 bg-white/[0.02] backdrop-blur-sm">
      <div className="flex flex-col gap-3 px-6 py-4 border-b border-white/5 md:flex-row md:items-center md:justify-between">
        <div>
          <h2 className="text-sm font-semibold text-white">System Logs</h2>
          <p className="text-xs text-gray-500 mt-0.5">
            Asynchronous audit events streamed from the backend
          </p>
        </div>
        <div className="flex items-center gap-3">
          <label className="flex items-center gap-2 cursor-pointer select-none">
            <span className="text-xs text-gray-400">Errors only</span>
            <button
              type="button"
              role="switch"
              aria-checked={errorsOnly}
              aria-label="Filter by error"
              onClick={() => setErrorsOnly((v) => !v)}
              className={`relative inline-flex h-5 w-9 items-center rounded-full border transition-colors ${
                errorsOnly
                  ? "bg-red-500/30 border-red-500/50"
                  : "bg-white/5 border-white/10"
              }`}
            >
              <span
                className={`inline-block h-3.5 w-3.5 rounded-full bg-white shadow transition-transform ${
                  errorsOnly ? "translate-x-4" : "translate-x-0.5"
                }`}
              />
            </button>
          </label>
          <button
            type="button"
            onClick={fetchLogs}
            disabled={loading}
            className="flex items-center gap-1.5 rounded-lg border border-white/10 bg-white/5 px-2.5 py-1.5 text-xs text-gray-300 hover:bg-white/10 disabled:opacity-50 transition-colors"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${loading ? "animate-spin" : ""}`} />
            Refresh
          </button>
        </div>
      </div>

      <div className="overflow-x-auto">
        <table className="min-w-full">
          <thead>
            <tr className="border-b border-white/5 bg-white/[0.02]">
              <th className="px-6 py-3 text-left text-[11px] font-medium uppercase tracking-widest text-gray-500">
                Timestamp
              </th>
              <th className="px-6 py-3 text-left text-[11px] font-medium uppercase tracking-widest text-gray-500">
                Event Type
              </th>
              <th className="px-6 py-3 text-left text-[11px] font-medium uppercase tracking-widest text-gray-500">
                System Message
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-white/5">
            {loading ? (
              <tr>
                <td colSpan={3} className="px-6 py-12 text-center">
                  <RefreshCw className="mx-auto h-5 w-5 animate-spin text-blue-400" />
                  <p className="mt-2 text-xs text-gray-500">Loading system logs…</p>
                </td>
              </tr>
            ) : forbidden ? (
              <tr>
                <td colSpan={3} className="px-6 py-12 text-center">
                  <AlertTriangle className="mx-auto h-5 w-5 text-amber-400" />
                  <p className="mt-2 text-xs text-gray-400">
                    System logs are restricted to coordinators and admins.
                  </p>
                </td>
              </tr>
            ) : fetchError ? (
              <tr>
                <td colSpan={3} className="px-6 py-12 text-center">
                  <AlertTriangle className="mx-auto h-5 w-5 text-red-400" />
                  <p className="mt-2 text-xs text-red-300">{fetchError}</p>
                </td>
              </tr>
            ) : visibleLogs.length === 0 ? (
              <tr>
                <td colSpan={3} className="px-6 py-12 text-center text-xs text-gray-500">
                  {errorsOnly
                    ? "No warning or error events on this page."
                    : "No system logs to display."}
                </td>
              </tr>
            ) : (
              visibleLogs.map((log) => {
                const severity = classifySeverity(log.actionType);
                return (
                  <tr
                    key={log.id}
                    className="hover:bg-white/[0.03] transition-colors"
                  >
                    <td className="px-6 py-3 whitespace-nowrap text-xs text-gray-400 font-mono">
                      {format(new Date(log.createdAt), "MMM d, yyyy HH:mm:ss")}
                    </td>
                    <td className="px-6 py-3 whitespace-nowrap">
                      <span
                        className={`inline-flex items-center rounded-full border px-2 py-0.5 text-[11px] font-medium ${severityBadgeClasses(severity)}`}
                      >
                        {log.actionType}
                      </span>
                    </td>
                    <td
                      className="px-6 py-3 max-w-xl truncate text-xs text-gray-300"
                      title={buildSystemMessage(log)}
                    >
                      {buildSystemMessage(log)}
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {!loading && !forbidden && !fetchError && pagination && totalPages > 0 && (
        <div className="flex items-center justify-between border-t border-white/5 px-6 py-3">
          <p className="text-xs text-gray-500">
            Page <span className="text-gray-300">{page + 1}</span> of{" "}
            <span className="text-gray-300">{totalPages}</span>
            {pagination.totalElements > 0 && (
              <>
                {" · "}
                <span className="text-gray-300">{pagination.totalElements}</span> total
              </>
            )}
          </p>
          <div className="flex items-center gap-1">
            <button
              type="button"
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={!canPrev}
              aria-label="Previous page"
              className="inline-flex items-center justify-center rounded-lg border border-white/10 bg-white/5 px-2 py-1 text-gray-300 hover:bg-white/10 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
            >
              <ChevronLeft className="h-4 w-4" />
            </button>
            <button
              type="button"
              onClick={() => setPage((p) => p + 1)}
              disabled={!canNext}
              aria-label="Next page"
              className="inline-flex items-center justify-center rounded-lg border border-white/10 bg-white/5 px-2 py-1 text-gray-300 hover:bg-white/10 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
            >
              <ChevronRight className="h-4 w-4" />
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
