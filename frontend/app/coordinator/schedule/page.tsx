"use client";

import { useState, useEffect } from "react";
import { toast } from "sonner";
import { getToken } from "@/lib/auth";
import { useAuthGuard } from "@/hooks/useAuthGuard";
import Sidebar from "@/components/Sidebar";
import { API_BASE } from "@/lib/api-utils";

interface ScheduleData {
  id: number;
  groupFormationDeadline: string;
  advisorAssignmentDeadline: string;
  updatedAt: string;
}

interface SprintData {
  id: number;
  sprintName: string;
  startDate: string;
  endDate: string;
  status: "UPCOMING" | "ACTIVE" | "COMPLETED";
  requiredStoryPoints: number | null;
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
            <SprintManagementCard />
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

// ─── Deadline Schedule Card ──────────────────────────────────────────────────

function ScheduleCard() {
  const [schedule, setSchedule] = useState<ScheduleData | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [formationDeadline, setFormationDeadline] = useState("");
  const [advisorDeadline, setAdvisorDeadline] = useState("");

  useEffect(() => {
    (async () => {
      try {
        const res = await fetch(`${API_BASE}/coordinator/schedule`, { headers: buildHeaders() });
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
      const res = await fetch(`${API_BASE}/coordinator/schedule`, {
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

// ─── Sprint Management Card ──────────────────────────────────────────────────

function SprintManagementCard() {
  const [sprints, setSprints] = useState<SprintData[]>([]);
  const [loading, setLoading] = useState(true);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [showAddForm, setShowAddForm] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);

  const [addName, setAddName] = useState("");
  const [addStart, setAddStart] = useState("");
  const [addEnd, setAddEnd] = useState("");
  const [addRequiredSp, setAddRequiredSp] = useState("");
  const [addSaving, setAddSaving] = useState(false);

  useEffect(() => {
    fetchSprints();
  }, []);

  async function fetchSprints() {
    try {
      const res = await fetch(`${API_BASE}/coordinator/sprints`, { headers: buildHeaders() });
      if (!res.ok) throw new Error();
      setSprints(await res.json());
    } catch {
      toast.error("Failed to load sprints.");
    } finally {
      setLoading(false);
    }
  }

  async function handleAdd() {
    if (!addName.trim() || !addStart || !addEnd) {
      toast.error("All fields are required.");
      return;
    }
    setAddSaving(true);
    try {
      const res = await fetch(`${API_BASE}/coordinator/sprints`, {
        method: "POST",
        headers: buildHeaders(),
        body: JSON.stringify({
          sprintName: addName.trim(),
          startDate: addStart,
          endDate: addEnd,
          requiredStoryPoints: addRequiredSp ? parseInt(addRequiredSp, 10) : null,
        }),
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error((err as { message?: string }).message || `Error ${res.status}`);
      }
      const created: SprintData = await res.json();
      setSprints(prev => [...prev, created].sort((a, b) => a.startDate.localeCompare(b.startDate)));
      setAddName(""); setAddStart(""); setAddEnd(""); setAddRequiredSp("");
      setShowAddForm(false);
      toast.success("Sprint created.");
    } catch (err: unknown) {
      toast.error(err instanceof Error ? err.message : "Failed to create sprint.");
    } finally {
      setAddSaving(false);
    }
  }

  async function handleDelete(id: number) {
    setDeletingId(id);
    try {
      const res = await fetch(`${API_BASE}/coordinator/sprints/${id}`, {
        method: "DELETE",
        headers: buildHeaders(),
      });
      if (!res.ok) throw new Error();
      setSprints(prev => prev.filter(s => s.id !== id));
      toast.success("Sprint deleted.");
    } catch {
      toast.error("Failed to delete sprint.");
    } finally {
      setDeletingId(null);
    }
  }

  return (
    <div className="bg-gray-900 border border-white/8 rounded-2xl overflow-hidden">
      <div className="px-6 py-4 border-b border-white/5 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-violet-500/10 border border-violet-500/20 flex items-center justify-center">
            <svg className="w-4 h-4 text-violet-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M3.75 12h16.5m-16.5 3.75h16.5M3.75 19.5h16.5M5.625 4.5h12.75a1.875 1.875 0 010 3.75H5.625a1.875 1.875 0 010-3.75z" />
            </svg>
          </div>
          <div>
            <p className="text-sm font-semibold text-white">Scrum Schedule</p>
            <p className="text-xs text-gray-500">Manage sprint timelines</p>
          </div>
        </div>
        <button
          onClick={() => { setShowAddForm(v => !v); setEditingId(null); }}
          className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium
                     bg-violet-600/20 border border-violet-500/30 text-violet-300
                     hover:bg-violet-600/30 transition-all"
        >
          <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d={showAddForm ? "M6 18L18 6M6 6l12 12" : "M12 4v16m8-8H4"} />
          </svg>
          {showAddForm ? "Cancel" : "Add Sprint"}
        </button>
      </div>

      {showAddForm && (
        <div className="px-6 py-5 border-b border-white/5 bg-white/2 space-y-4">
          <p className="text-xs font-medium text-gray-400 uppercase tracking-wider">New Sprint</p>
          <div className="space-y-3">
            <input
              type="text"
              placeholder="Sprint name"
              value={addName}
              onChange={e => setAddName(e.target.value)}
              className="w-full px-4 py-2.5 bg-gray-800 border border-white/10 rounded-xl text-sm text-white placeholder-gray-600
                         focus:outline-none focus:border-violet-500/60 focus:ring-1 focus:ring-violet-500/30 transition-all"
            />
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <label className="text-xs text-gray-500">Start Date</label>
                <input
                  type="date"
                  value={addStart}
                  onChange={e => setAddStart(e.target.value)}
                  className="w-full px-4 py-2.5 bg-gray-800 border border-white/10 rounded-xl text-sm text-white
                             focus:outline-none focus:border-violet-500/60 focus:ring-1 focus:ring-violet-500/30
                             transition-all [color-scheme:dark]"
                />
              </div>
              <div className="space-y-1">
                <label className="text-xs text-gray-500">End Date</label>
                <input
                  type="date"
                  value={addEnd}
                  onChange={e => setAddEnd(e.target.value)}
                  className="w-full px-4 py-2.5 bg-gray-800 border border-white/10 rounded-xl text-sm text-white
                             focus:outline-none focus:border-violet-500/60 focus:ring-1 focus:ring-violet-500/30
                             transition-all [color-scheme:dark]"
                />
              </div>
            </div>
            <div className="space-y-1">
              <label className="text-xs text-gray-500">Required Story Points per Student <span className="text-gray-600">(optional)</span></label>
              <input
                type="number"
                min="1"
                placeholder="e.g. 5"
                value={addRequiredSp}
                onChange={e => setAddRequiredSp(e.target.value)}
                className="w-full px-4 py-2.5 bg-gray-800 border border-white/10 rounded-xl text-sm text-white placeholder-gray-600
                           focus:outline-none focus:border-violet-500/60 focus:ring-1 focus:ring-violet-500/30 transition-all"
              />
            </div>
          </div>
          <button
            onClick={handleAdd}
            disabled={addSaving}
            className="flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium transition-all
                       bg-violet-600 text-white hover:bg-violet-500 active:scale-95
                       disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none"
          >
            {addSaving ? (
              <>
                <svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                </svg>
                Creating...
              </>
            ) : "Create Sprint"}
          </button>
        </div>
      )}

      <div className="divide-y divide-white/5">
        {loading ? (
          <div className="px-6 py-8 flex items-center justify-center">
            <svg className="w-5 h-5 animate-spin text-violet-500" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
            </svg>
          </div>
        ) : sprints.length === 0 ? (
          <div className="px-6 py-8 text-center">
            <p className="text-sm text-gray-600">No sprints configured yet.</p>
          </div>
        ) : (
          sprints.map(sprint => (
            <SprintRow
              key={sprint.id}
              sprint={sprint}
              isEditing={editingId === sprint.id}
              isDeleting={deletingId === sprint.id}
              onEdit={() => setEditingId(sprint.id)}
              onCancelEdit={() => setEditingId(null)}
              onDelete={() => handleDelete(sprint.id)}
              onSaved={(updated) => {
                setSprints(prev =>
                  prev.map(s => s.id === updated.id ? updated : s)
                    .sort((a, b) => a.startDate.localeCompare(b.startDate))
                );
                setEditingId(null);
              }}
            />
          ))
        )}
      </div>
    </div>
  );
}

function SprintRow({
  sprint,
  isEditing,
  isDeleting,
  onEdit,
  onCancelEdit,
  onDelete,
  onSaved,
}: {
  sprint: SprintData;
  isEditing: boolean;
  isDeleting: boolean;
  onEdit: () => void;
  onCancelEdit: () => void;
  onDelete: () => void;
  onSaved: (updated: SprintData) => void;
}) {
  const [name, setName] = useState(sprint.sprintName);
  const [start, setStart] = useState(sprint.startDate);
  const [end, setEnd] = useState(sprint.endDate);
  const [requiredSp, setRequiredSp] = useState(sprint.requiredStoryPoints?.toString() ?? "");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (isEditing) {
      setName(sprint.sprintName);
      setStart(sprint.startDate);
      setEnd(sprint.endDate);
      setRequiredSp(sprint.requiredStoryPoints?.toString() ?? "");
    }
  }, [isEditing, sprint]);

  async function handleSave() {
    if (!name.trim() || !start || !end) {
      toast.error("All fields are required.");
      return;
    }
    setSaving(true);
    try {
      const res = await fetch(`${API_BASE}/coordinator/sprints/${sprint.id}`, {
        method: "PUT",
        headers: buildHeaders(),
        body: JSON.stringify({
          sprintName: name.trim(),
          startDate: start,
          endDate: end,
          requiredStoryPoints: requiredSp ? parseInt(requiredSp, 10) : null,
        }),
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error((err as { message?: string }).message || `Error ${res.status}`);
      }
      const updated: SprintData = await res.json();
      onSaved(updated);
      toast.success("Sprint updated.");
    } catch (err: unknown) {
      toast.error(err instanceof Error ? err.message : "Failed to update sprint.");
    } finally {
      setSaving(false);
    }
  }

  const statusStyle = {
    ACTIVE: "bg-green-500/10 border-green-500/20 text-green-400",
    UPCOMING: "bg-blue-500/10 border-blue-500/20 text-blue-400",
    COMPLETED: "bg-gray-500/10 border-gray-500/20 text-gray-400",
  }[sprint.status] ?? "bg-gray-500/10 border-gray-500/20 text-gray-400";

  if (isEditing) {
    return (
      <div className="px-6 py-4 bg-white/2 space-y-3">
        <input
          type="text"
          value={name}
          onChange={e => setName(e.target.value)}
          className="w-full px-3 py-2 bg-gray-800 border border-white/10 rounded-lg text-sm text-white
                     focus:outline-none focus:border-violet-500/60 focus:ring-1 focus:ring-violet-500/30 transition-all"
        />
        <div className="grid grid-cols-2 gap-3">
          <div className="space-y-1">
            <label className="text-xs text-gray-500">Start Date</label>
            <input
              type="date"
              value={start}
              onChange={e => setStart(e.target.value)}
              className="w-full px-3 py-2 bg-gray-800 border border-white/10 rounded-lg text-sm text-white
                         focus:outline-none focus:border-violet-500/60 focus:ring-1 focus:ring-violet-500/30
                         transition-all [color-scheme:dark]"
            />
          </div>
          <div className="space-y-1">
            <label className="text-xs text-gray-500">End Date</label>
            <input
              type="date"
              value={end}
              onChange={e => setEnd(e.target.value)}
              className="w-full px-3 py-2 bg-gray-800 border border-white/10 rounded-lg text-sm text-white
                         focus:outline-none focus:border-violet-500/60 focus:ring-1 focus:ring-violet-500/30
                         transition-all [color-scheme:dark]"
            />
          </div>
        </div>
        <div className="space-y-1">
          <label className="text-xs text-gray-500">Required Story Points per Student <span className="text-gray-600">(optional)</span></label>
          <input
            type="number"
            min="1"
            placeholder="e.g. 5"
            value={requiredSp}
            onChange={e => setRequiredSp(e.target.value)}
            className="w-full px-3 py-2 bg-gray-800 border border-white/10 rounded-lg text-sm text-white placeholder-gray-600
                       focus:outline-none focus:border-violet-500/60 focus:ring-1 focus:ring-violet-500/30 transition-all"
          />
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={handleSave}
            disabled={saving}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-all
                       bg-violet-600 text-white hover:bg-violet-500 active:scale-95
                       disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {saving ? "Saving..." : "Save"}
          </button>
          <button
            onClick={onCancelEdit}
            className="px-3 py-1.5 rounded-lg text-xs font-medium text-gray-400
                       hover:text-white hover:bg-white/5 transition-all"
          >
            Cancel
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="px-6 py-4 flex items-center justify-between gap-4">
      <div className="flex items-center gap-3 min-w-0">
        <span className={`shrink-0 px-2 py-0.5 rounded-md text-xs font-medium border ${statusStyle}`}>
          {sprint.status}
        </span>
        <div className="min-w-0">
          <p className="text-sm font-medium text-white truncate">{sprint.sprintName}</p>
          <p className="text-xs text-gray-500 mt-0.5">
            {formatShortDate(sprint.startDate)} – {formatShortDate(sprint.endDate)}
            {sprint.requiredStoryPoints != null && (
              <span className="ml-2 text-gray-600">· {sprint.requiredStoryPoints} SP/student</span>
            )}
          </p>
        </div>
      </div>
      <div className="flex items-center gap-1 shrink-0">
        <button
          onClick={onEdit}
          className="p-1.5 rounded-lg text-gray-500 hover:text-white hover:bg-white/5 transition-all"
          title="Edit"
        >
          <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L10.582 16.07a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931zm0 0L19.5 7.125" />
          </svg>
        </button>
        <button
          onClick={onDelete}
          disabled={isDeleting}
          className="p-1.5 rounded-lg text-gray-500 hover:text-red-400 hover:bg-red-500/10 transition-all
                     disabled:opacity-40 disabled:cursor-not-allowed"
          title="Delete"
        >
          {isDeleting ? (
            <svg className="w-3.5 h-3.5 animate-spin" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
            </svg>
          ) : (
            <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0" />
            </svg>
          )}
        </button>
      </div>
    </div>
  );
}

// ─── Shared helpers ───────────────────────────────────────────────────────────

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

function formatShortDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString("tr-TR", {
    day: "2-digit", month: "short", year: "numeric",
  });
}
