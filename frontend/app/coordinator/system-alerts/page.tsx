"use client";

import { useState, useEffect, useCallback } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { buildHeaders } from "@/lib/api-utils";
import { useAuthGuard } from "@/hooks/useAuthGuard";
import Sidebar from "@/components/Sidebar";

const API = process.env.NEXT_PUBLIC_API_URL;

interface AffectedGroup {
  groupId: number;
  groupName: string;
  memberCount: number;
}

interface SystemAlert {
  alertId: number;
  alertType: "formation_deadline_missed" | "advisor_deadline_missed" | string;
  message: string;
  deadlineDate: string | null;
  currentDate: string;
  daysOverdue: number;
  affectedGroups: AffectedGroup[];
  suggestedAction: "extend_deadline" | "disband" | string;
  createdAt: string;
}

interface SystemAlertListResponse {
  message: string;
  total: number;
  alerts: SystemAlert[];
}

export default function SystemAlertsPage() {
  const authStatus = useAuthGuard("coordinator");
  if (authStatus === "loading") return <Spinner />;
  if (authStatus === "denied") return <AccessDenied />;
  return <SystemAlertsLayout />;
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
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
          </svg>
        </div>
        <h1 className="text-lg font-semibold text-white">Access Restricted</h1>
        <p className="text-sm text-gray-500">Only Coordinators can access this page.</p>
      </div>
    </div>
  );
}

function SystemAlertsLayout() {
  const [alerts, setAlerts] = useState<SystemAlert[]>([]);
  const [loading, setLoading] = useState(true);
  const [lastRefresh, setLastRefresh] = useState<Date | null>(null);
  const router = useRouter();

  const loadAlerts = useCallback(async () => {
    setLoading(true);
    try {
      const res = await fetch(`${API}/coordinator/system-alerts`, { headers: buildHeaders() });
      if (!res.ok) throw new Error(`Error ${res.status}`);
      const data: SystemAlertListResponse = await res.json();
      setAlerts(data.alerts || []);
      setLastRefresh(new Date());
    } catch {
      toast.error("Failed to load system alerts.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadAlerts(); }, [loadAlerts]);

  const handleAction = (alert: SystemAlert) => {
    if (alert.suggestedAction === "extend_deadline") {
      router.push("/coordinator/schedule");
    } else {
      toast.info("Disband flow will be implemented in Issue #17.");
    }
  };

  const totalAlerts = alerts.length;
  const maxOverdue = alerts.reduce((max, a) => Math.max(max, a.daysOverdue), 0);
  const totalGroups = alerts.reduce((sum, a) => sum + (a.affectedGroups?.length ?? 0), 0);

  return (
    <div className="min-h-screen bg-gray-950 flex">
      <Sidebar activePage="system-alerts" />
      <main className="flex-1 flex flex-col min-w-0">
        <div className="border-b border-white/5 px-8 py-4 flex items-center justify-between">
          <div>
            <h1 className="text-base font-semibold text-white">System Alerts</h1>
            <p className="text-xs text-gray-500 mt-0.5">
              Missed deadline notifications
              {lastRefresh && ` · Last updated: ${lastRefresh.toLocaleTimeString("tr-TR")}`}
            </p>
          </div>
          <button
            id="refreshAlertsBtn"
            onClick={loadAlerts}
            disabled={loading}
            className="flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs text-gray-400
                       hover:text-white hover:bg-white/5 border border-white/10 transition-all"
          >
            <svg className={`w-3.5 h-3.5 ${loading ? "animate-spin" : ""}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            Refresh
          </button>
        </div>

        <div className="flex-1 p-8">
          {loading ? (
            <div className="flex items-center justify-center h-64">
              <svg className="w-6 h-6 animate-spin text-blue-500" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
              </svg>
            </div>
          ) : alerts.length === 0 ? (
            <EmptyState />
          ) : (
            <div className="max-w-3xl mx-auto space-y-5">
              <div className="grid grid-cols-3 gap-4">
                <StatCard label="Active Alerts" value={totalAlerts} color="red" />
                <StatCard label="Max Days Overdue" value={maxOverdue} color="amber" />
                <StatCard label="Affected Groups" value={totalGroups} color="blue" />
              </div>
              <div className="flex items-center gap-3">
                <div className="flex-1 h-px bg-white/5" />
                <span className="text-xs text-gray-600 uppercase tracking-widest">Alert Timeline</span>
                <div className="flex-1 h-px bg-white/5" />
              </div>
              {alerts.map((alert) => (
                <AlertCard key={alert.alertId} alert={alert} onAction={() => handleAction(alert)} />
              ))}
            </div>
          )}
        </div>
      </main>
    </div>
  );
}

function AlertCard({ alert, onAction }: { alert: SystemAlert; onAction: () => void }) {
  const isFormation = alert.alertType === "formation_deadline_missed";
  const [groupsExpanded, setGroupsExpanded] = useState(false);

  return (
    <div className="bg-gray-900 border border-white/8 rounded-2xl overflow-hidden">
      <div className={`px-6 py-4 border-b border-white/5 flex items-center justify-between ${isFormation ? "bg-amber-500/5" : "bg-red-500/5"}`}>
        <div className="flex items-center gap-3">
          <div className={`w-9 h-9 rounded-xl flex items-center justify-center ${isFormation ? "bg-amber-500/15 border border-amber-500/25" : "bg-red-500/15 border border-red-500/25"}`}>
            {isFormation ? (
              <svg className="w-4 h-4 text-amber-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 6v6h4.5m4.5 0a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            ) : (
              <svg className="w-4 h-4 text-red-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z" />
              </svg>
            )}
          </div>
          <div>
            <div className="flex items-center gap-2">
              <p className="text-sm font-semibold text-white">
                {isFormation ? "Formation Deadline Missed" : "Advisor Deadline Missed"}
              </p>
              <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${isFormation ? "bg-amber-500/15 text-amber-400" : "bg-red-500/15 text-red-400"}`}>
                {isFormation ? "Incomplete Groups" : "No Advisor"}
              </span>
            </div>
            <p className="text-xs text-gray-500 mt-0.5">
              Detected: {alert.createdAt ? formatDate(alert.createdAt) : "—"}
            </p>
          </div>
        </div>
        {alert.daysOverdue > 0 && (
          <div className="text-right">
            <p className="text-2xl font-bold text-red-400">{alert.daysOverdue}</p>
            <p className="text-xs text-gray-500">days overdue</p>
          </div>
        )}
      </div>

      <div className="px-6 py-5 space-y-4">
        <p className="text-sm text-gray-300">{alert.message}</p>
        <div className="grid grid-cols-2 gap-3">
          <div className="bg-gray-800/60 rounded-xl px-4 py-3 space-y-1">
            <p className="text-xs text-gray-500">Deadline Was</p>
            <p className="text-sm font-semibold text-red-400">
              {alert.deadlineDate ? formatDate(alert.deadlineDate) : "—"}
            </p>
          </div>
          <div className="bg-gray-800/60 rounded-xl px-4 py-3 space-y-1">
            <p className="text-xs text-gray-500">Current Date</p>
            <p className="text-sm font-semibold text-white">{formatDate(alert.currentDate)}</p>
          </div>
        </div>

        {alert.affectedGroups && alert.affectedGroups.length > 0 ? (
          <div className="space-y-2">
            <button onClick={() => setGroupsExpanded(!groupsExpanded)} className="flex items-center gap-2 text-xs text-gray-400 hover:text-white transition-colors">
              <svg className={`w-3.5 h-3.5 transition-transform ${groupsExpanded ? "rotate-90" : ""}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
              </svg>
              {alert.affectedGroups.length} affected group{alert.affectedGroups.length !== 1 ? "s" : ""}
            </button>
            {groupsExpanded && (
              <div className="space-y-1.5">
                {alert.affectedGroups.map((g) => (
                  <div key={g.groupId} className="flex items-center justify-between px-3 py-2.5 bg-gray-800/50 rounded-lg border border-white/5">
                    <div className="flex items-center gap-2.5">
                      <div className="w-7 h-7 rounded-lg bg-blue-600/20 border border-blue-500/20 flex items-center justify-center">
                        <span className="text-xs font-bold text-blue-400">G</span>
                      </div>
                      <span className="text-sm text-white">{g.groupName || `Group #${g.groupId}`}</span>
                    </div>
                    <span className="text-xs text-gray-500">{g.memberCount} member{g.memberCount !== 1 ? "s" : ""}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        ) : (
          <p className="text-xs text-gray-600">Group details not available yet</p>
        )}
      </div>

      <div className="px-6 py-4 border-t border-white/5 flex items-center justify-between">
        <div className="flex items-center gap-2 text-xs text-gray-500">
          <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
          </svg>
          Suggested: <span className="text-gray-300 font-medium ml-1">
            {alert.suggestedAction === "extend_deadline" ? "Extend Deadline" : "Disband Groups"}
          </span>
        </div>
        <button
          id={`action-btn-${alert.alertId}`}
          onClick={onAction}
          className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-medium transition-all active:scale-95
                      ${alert.suggestedAction === "extend_deadline" ? "bg-blue-600/90 text-white hover:bg-blue-500" : "bg-red-600/90 text-white hover:bg-red-500"}`}
        >
          {alert.suggestedAction === "extend_deadline" ? "Extend Deadline" : "Trigger Disband"}
        </button>
      </div>
    </div>
  );
}

function EmptyState() {
  return (
    <div className="flex flex-col items-center justify-center h-64 space-y-4">
      <div className="w-16 h-16 rounded-2xl bg-green-500/10 border border-green-500/20 flex items-center justify-center">
        <svg className="w-7 h-7 text-green-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      </div>
      <div className="text-center">
        <p className="text-sm font-semibold text-white">All Clear</p>
        <p className="text-xs text-gray-500 mt-1">No missed deadlines detected. Everything is on track.</p>
      </div>
    </div>
  );
}

function StatCard({ label, value, color }: { label: string; value: number; color: "red" | "amber" | "blue" }) {
  const colors = {
    red:   { text: "text-red-400",   bg: "bg-red-500/10",   border: "border-red-500/20" },
    amber: { text: "text-amber-400", bg: "bg-amber-500/10", border: "border-amber-500/20" },
    blue:  { text: "text-blue-400",  bg: "bg-blue-500/10",  border: "border-blue-500/20" },
  };
  const c = colors[color];
  return (
    <div className={`${c.bg} border ${c.border} rounded-2xl px-5 py-4 text-center`}>
      <p className={`text-3xl font-bold ${c.text}`}>{value}</p>
      <p className="text-xs text-gray-500 mt-1">{label}</p>
    </div>
  );
}



function formatDate(isoString: string): string {
  return new Date(isoString).toLocaleString("tr-TR", {
    day: "2-digit", month: "short", year: "numeric",
    hour: "2-digit", minute: "2-digit",
  });
}
