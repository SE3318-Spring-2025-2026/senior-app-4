"use client";

import { useState, useEffect, useCallback } from "react";
import apiClient from "@/lib/client";
import { toast } from "sonner";
import ScheduleConflictModal, { ScheduleConflictData } from "./ScheduleConflictModal";

interface Group {
  id: number;
  name: string;
}

interface AssignedGroup {
  id: number; // assignment id
  group: Group;
  examDate: string;
  status: string; // ASSIGNED, SCHEDULED, COMPLETED, CANCELLED
}

export default function GroupAssignment({ committeeId }: { committeeId: string }) {
  const [assignedGroups, setAssignedGroups] = useState<AssignedGroup[]>([]);
  const [availableGroups, setAvailableGroups] = useState<Group[]>([]);
  const [loading, setLoading] = useState(true);
  const [assigning, setAssigning] = useState(false);

  // Form State
  const [selectedGroupId, setSelectedGroupId] = useState("");
  const [examDate, setExamDate] = useState("");

  // Conflict Modal State
  const [conflictData, setConflictData] = useState<ScheduleConflictData | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [assignedRes, availableRes] = await Promise.all([
        apiClient.get(`/committees/${committeeId}/groups`),
        apiClient.get(`/groups`), // Assumes this endpoint returns all groups
      ]);
      setAssignedGroups(assignedRes.data || []);
      setAvailableGroups(availableRes.data || []);
    } catch (error) {
    } finally {
      setLoading(false);
    }
  }, [committeeId]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleAssign = async () => {
    if (!selectedGroupId || !examDate) return;
    setAssigning(true);
    try {
      await apiClient.post(`/committees/${committeeId}/groups`, {
        groupId: parseInt(selectedGroupId),
        examDate: new Date(examDate).toISOString(),
      });
      toast.success("Group assigned successfully.");
      setSelectedGroupId("");
      setExamDate("");
      fetchData();
    } catch (error: any) {
      // Check if it's a conflict error (400)
      if (error.response && error.response.status === 400 && error.response.data?.conflict) {
        setConflictData(error.response.data.conflict);
        setIsModalOpen(true);
      }
    } finally {
      setAssigning(false);
    }
  };

  const handleRemove = async (assignmentId: number) => {
    if (!confirm("Are you sure you want to remove this group assignment?")) return;
    try {
      await apiClient.delete(`/committees/${committeeId}/groups/${assignmentId}`);
      toast.success("Group assignment removed.");
      fetchData();
    } catch (error) {}
  };

  const handleUpdateStatus = async (assignmentId: number, newStatus: string) => {
    if (!confirm(`Update status to ${newStatus}?`)) return;
    try {
      await apiClient.patch(`/committees/${committeeId}/groups/${assignmentId}/status`, { status: newStatus });
      toast.success(`Status updated to ${newStatus}`);
      fetchData();
    } catch (error) {}
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "ASSIGNED": return "bg-orange-500/10 text-orange-400 border-orange-500/20";
      case "SCHEDULED": return "bg-blue-500/10 text-blue-400 border-blue-500/20";
      case "COMPLETED": return "bg-green-500/10 text-green-400 border-green-500/20";
      case "CANCELLED": return "bg-red-500/10 text-red-400 border-red-500/20";
      default: return "bg-gray-500/10 text-gray-400 border-gray-500/20";
    }
  };

  if (loading && assignedGroups.length === 0) {
    return <div className="animate-pulse h-64 bg-white/5 rounded-xl border border-white/10 mb-6"></div>;
  }

  // Get current date formatted for datetime-local min attribute
  const today = new Date().toISOString().slice(0, 16);

  return (
    <>
      <div className="bg-gray-900 border border-white/10 rounded-xl overflow-hidden mb-6 flex flex-col">
        <div className="px-5 py-4 border-b border-white/5 bg-white/5 flex justify-between items-center">
          <h2 className="text-sm font-semibold text-white">Assigned Groups & Schedule</h2>
          <span className="text-xs bg-white/10 text-gray-300 px-2 py-1 rounded-full">{assignedGroups.length} Assigned</span>
        </div>
        
        <div className="p-5 flex-1 flex flex-col">
          {/* Assignment Form */}
          <div className="flex flex-col md:flex-row gap-3 mb-6">
            <select
              value={selectedGroupId}
              onChange={(e) => setSelectedGroupId(e.target.value)}
              className="flex-1 bg-black/40 border border-white/10 text-sm text-white rounded-lg px-3 py-2 focus:ring-1 focus:ring-blue-500 outline-none"
              title="Select a group to assign"
            >
              <option value="">Select Group...</option>
              {availableGroups.map((g) => (
                <option key={g.id} value={g.id}>{g.name}</option>
              ))}
            </select>
            <input
              type="datetime-local"
              value={examDate}
              min={today}
              onChange={(e) => setExamDate(e.target.value)}
              className="w-full md:w-56 bg-black/40 border border-white/10 text-sm text-white rounded-lg px-3 py-2 focus:ring-1 focus:ring-blue-500 outline-none [color-scheme:dark]"
              title="Select the exam date and time"
            />
            <button
              onClick={handleAssign}
              disabled={!selectedGroupId || !examDate || assigning}
              className="bg-blue-600 hover:bg-blue-500 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed min-w-[80px] flex items-center justify-center"
            >
              {assigning ? <span className="animate-spin text-lg">↻</span> : "Assign"}
            </button>
          </div>

          {/* List */}
          <div className="flex-1 space-y-3 overflow-y-auto max-h-[400px] pr-2">
            {assignedGroups.length === 0 ? (
              <div className="text-center py-8 text-gray-500 text-sm">No groups assigned yet.</div>
            ) : (
              assignedGroups.map((ag) => (
                <div key={ag.id} className="flex flex-col sm:flex-row sm:items-center justify-between p-4 bg-white/5 border border-white/5 rounded-lg group hover:border-white/10 transition-colors gap-4">
                  <div>
                    <div className="flex items-center gap-3 mb-1">
                      <p className="text-sm font-medium text-white">{ag.group?.name || "Unknown Group"}</p>
                      <span className={`text-[10px] uppercase font-semibold px-2 py-0.5 rounded border ${getStatusBadge(ag.status)}`}>
                        {ag.status}
                      </span>
                    </div>
                    <p className="text-xs text-gray-400">Exam: {new Date(ag.examDate).toLocaleString()}</p>
                  </div>
                  
                  <div className="flex items-center gap-2 self-end sm:self-auto opacity-100 sm:opacity-0 sm:group-hover:opacity-100 transition-opacity">
                    <select
                      value={ag.status}
                      onChange={(e) => handleUpdateStatus(ag.id, e.target.value)}
                      className="bg-black/40 border border-white/10 text-xs text-gray-300 rounded-md px-2 py-1.5 outline-none"
                    >
                      <option value="ASSIGNED">Assigned</option>
                      <option value="SCHEDULED">Scheduled</option>
                      <option value="COMPLETED">Completed</option>
                      <option value="CANCELLED">Cancelled</option>
                    </select>
                    
                    <button
                      onClick={() => handleRemove(ag.id)}
                      className="text-red-400 p-1.5 hover:bg-red-500/10 rounded-md transition-colors"
                      title="Remove Group"
                    >
                      <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" /></svg>
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>

      <ScheduleConflictModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        conflictData={conflictData}
      />
    </>
  );
}
