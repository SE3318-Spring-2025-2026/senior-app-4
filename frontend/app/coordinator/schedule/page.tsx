"use client";

import { useState, useEffect } from "react";
import { toast } from "sonner";
import { getToken } from "@/lib/auth";
import { useAuthGuard } from "@/hooks/useAuthGuard";
import Sidebar from "@/components/Sidebar";

const API = process.env.NEXT_PUBLIC_API_URL;

interface ScheduleData {
  id: number;
  groupFormationDeadline: string;
  advisorAssignmentDeadline: string;
  updatedAt: string;
}

export default function SchedulePage() {
  const authStatus = useAuthGuard("coordinator");
  if (authStatus === "loading") return <Spinner />;
  if (authStatus === "denied") return <AccessDenied />;
  return <ScheduleLayout />;
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

function ScheduleLayout() {
  return (
    <div className="min-h-screen bg-gray-950 flex">
      <Sidebar activePage="schedule" />
      <main className="flex-1 flex flex-col min-w-0">
        <div className="border-b border-white/5 px-8 py-4 flex items-center justify-between">
          <div>
            <h1 className="text-base font-semibold text-white">Deadline Schedule</h1>
            <p className="text-xs text-gray-500 mt-0.5">Set group formation and advisor assignment deadlines</p>
          </div>
          <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-white/5 border border-white/10">
            <div className="w-1.5 h-1.5 rounded-full bg-green-400 animate-pulse" />
            <span className="text-xs text-gray-400">System Online</span>
          </div>
        </div>
        <div className="flex-1 p-8">
          <div className="max-w-2xl mx-auto space-y-5">
            <ScheduleCard />
          </div>
        </div>
      </main>
    </div>
  );
}

function buildHeaders(): Record<string, string> {
  const h: Record<string, string> = { "Content-Type": "application/json" };
  const token = getToken();
  if (token) h["Authorization"] = `Bearer ${token}`;
  return h;
}

function ScheduleCard() {
  const [schedule, setSchedule] = useState<ScheduleData | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [formationDeadline, setFormationDeadline] = useState("");
  const [advisorDeadline, setAdvisorDeadline] = useState("");

  useEffect(() => {
    (async () => {
      try {
        const res = await fetch(`${API}/coordinator/schedule`, { headers: buildHeaders() });
        if (res.status === 404) { setLoading(false); return; }
        if (!res.ok) throw new Error("Failed to load schedule");
        const data: ScheduleData = await res.json();
        setSchedule(data);
        setFormationDeadline(toLocalInput(data.groupFormationDeadline));
        setAdvisorDeadline(toLocalInput(data.advisorAssignmentDeadline));
      } catch {
        toast.error("Failed to load schedule.");
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const handleSave = async () => {
    if (!formationDeadline || !advisorDeadline) {
      toast.error("Both deadlines are required.");
      return;
    }
    const now = new Date();
    if (new Date(formationDeadline) <= now) {
      toast.error("Group formation deadline must be in the future.");
      return;
    }
    if (new Date(advisorDeadline) <= now) {
      toast.error("Advisor assignment deadline must be in the future.");
      return;
    }
    setSaving(true);
    try {
      const body = {
        groupFormationDeadline: new Date(formationDeadline).toISOString(),
        advisorAssignmentDeadline: new Date(advisorDeadline).toISOString(),
      };
      const res = await fetch(`${API}/coordinator/schedule`, {
        method: "PUT",
        headers: buildHeaders(),
        body: JSON.stringify(body),
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error((err as { message?: string }).message || `Error ${res.status}`);
      }
      const data: ScheduleData = await res.json();
      setSchedule(data);
      toast.success("Schedule updated successfully.");
    } catch (err: unknown) {
      toast.error(err instanceof Error ? err.message : "Failed to save schedule.");
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="bg-gray-900 border border-white/8 rounded-2xl p-8 flex items-center justify-center">
        <svg className="w-5 h-5 animate-spin text-blue-500" fill="none" viewBox="0 0 24 24">
          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
        </svg>
      </div>
    );
  }

  return (
    <>
      {schedule && (
        <div className="bg-gray-900 border border-white/8 rounded-2xl overflow-hidden">
          <div className="px-6 py-4 border-b border-white/5 flex items-center gap-3">
            <div className="w-8 h-8 rounded-lg bg-green-500/10 border border-green-500/20 flex items-center justify-center">
              <svg className="w-4 h-4 text-green-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <div>
              <p className="text-sm font-semibold text-white">Current Schedule</p>
              <p className="text-xs text-gray-500">Last updated: {schedule.updatedAt ? formatDate(schedule.updatedAt) : "—"}</p>
            </div>
          </div>
          <div className="grid grid-cols-2 divide-x divide-white/5">
            <DeadlineInfo label="Group Formation Deadline" value={schedule.groupFormationDeadline} />
            <DeadlineInfo label="Advisor Assignment Deadline" value={schedule.advisorAssignmentDeadline} />
          </div>
        </div>
      )}

      <div className="bg-gray-900 border border-white/8 rounded-2xl overflow-hidden">
        <div className="px-6 py-4 border-b border-white/5 flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-blue-500/10 border border-blue-500/20 flex items-center justify-center">
            <svg className="w-4 h-4 text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 012.25-2.25h13.5A2.25 2.25 0 0121 7.5v11.25m-18 0A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75m-18 0v-7.5A2.25 2.25 0 015.25 9h13.5A2.25 2.25 0 0121 9v7.5" />
            </svg>
          </div>
          <div>
            <p className="text-sm font-semibold text-white">{schedule ? "Update Schedule" : "Set Schedule"}</p>
            <p className="text-xs text-gray-500">Dates must be in the future</p>
          </div>
        </div>

        <div className="p-6 space-y-5">
          <div className="space-y-2">
            <label className="text-xs font-medium text-gray-400 uppercase tracking-wider">
              Group Formation Deadline
            </label>
            <input
              id="formationDeadline"
              type="datetime-local"
              value={formationDeadline}
              onChange={(e) => setFormationDeadline(e.target.value)}
              min={toLocalInput(new Date().toISOString())}
              className="w-full px-4 py-3 bg-gray-800 border border-white/10 rounded-xl text-sm text-white
                         focus:outline-none focus:border-blue-500/60 focus:ring-1 focus:ring-blue-500/30
                         transition-all [color-scheme:dark]"
            />
            <p className="text-xs text-gray-600">Groups not fully formed by this date will be flagged as incomplete.</p>
          </div>

          <div className="space-y-2">
            <label className="text-xs font-medium text-gray-400 uppercase tracking-wider">
              Advisor Assignment Deadline
            </label>
            <input
              id="advisorDeadline"
              type="datetime-local"
              value={advisorDeadline}
              onChange={(e) => setAdvisorDeadline(e.target.value)}
              min={toLocalInput(new Date().toISOString())}
              className="w-full px-4 py-3 bg-gray-800 border border-white/10 rounded-xl text-sm text-white
                         focus:outline-none focus:border-blue-500/60 focus:ring-1 focus:ring-blue-500/30
                         transition-all [color-scheme:dark]"
            />
            <p className="text-xs text-gray-600">Groups without an advisor by this date will be queued for disbandment.</p>
          </div>
        </div>

        <div className="px-6 pb-6">
          <button
            id="saveScheduleBtn"
            onClick={handleSave}
            disabled={saving}
            className="flex items-center gap-2 px-5 py-2.5 rounded-xl text-sm font-medium transition-all
                       bg-blue-600 text-white hover:bg-blue-500 active:scale-95 shadow-lg shadow-blue-600/20
                       disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none"
          >
            {saving ? (
              <>
                <svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                </svg>
                Saving...
              </>
            ) : (
              <>
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                </svg>
                {schedule ? "Update Schedule" : "Set Schedule"}
              </>
            )}
          </button>
        </div>
      </div>
    </>
  );
}

function DeadlineInfo({ label, value }: { label: string; value: string }) {
  const isPast = value && new Date(value) < new Date();
  return (
    <div className="px-6 py-5 space-y-1">
      <p className="text-xs text-gray-500">{label}</p>
      <p className={`text-sm font-semibold ${isPast ? "text-red-400" : "text-white"}`}>
        {value ? formatDate(value) : "Not set"}
      </p>
      {isPast && <p className="text-xs text-red-500">⚠ Deadline has passed</p>}
    </div>
  );
}

function toLocalInput(isoString: string): string {
  const d = new Date(isoString);
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

function formatDate(isoString: string): string {
  return new Date(isoString).toLocaleString("tr-TR", {
    day: "2-digit", month: "short", year: "numeric",
    hour: "2-digit", minute: "2-digit",
  });
}


