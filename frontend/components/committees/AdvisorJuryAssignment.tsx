"use client";

import { useState, useEffect, useCallback } from "react";
import apiClient from "@/lib/client";
import { toast } from "sonner";

interface User {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
}

interface Advisor {
  id: number;
  advisor: User;
  role: string;
  assignedAt: string;
}

interface Jury {
  id: number;
  juryMember: User;
  juryType: string;
  assignedAt: string;
}

export default function AdvisorJuryAssignment({ committeeId }: { committeeId: string }) {
  const [advisors, setAdvisors] = useState<Advisor[]>([]);
  const [juries, setJuries] = useState<Jury[]>([]);
  const [availableProfessors, setAvailableProfessors] = useState<User[]>([]);
  
  const [loading, setLoading] = useState(true);
  const [assigningAdvisor, setAssigningAdvisor] = useState(false);
  const [assigningJury, setAssigningJury] = useState(false);

  // Form states
  const [selectedAdvisorId, setSelectedAdvisorId] = useState("");
  const [advisorRole, setAdvisorRole] = useState("MEMBER");

  const [selectedJuryId, setSelectedJuryId] = useState("");
  const [juryType, setJuryType] = useState("INTERNAL");

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [advRes, juryRes, profRes] = await Promise.all([
        apiClient.get(`/committees/${committeeId}/advisors`),
        apiClient.get(`/committees/${committeeId}/jury`),
        apiClient.get(`/users?role=PROFESSOR`), // Assuming we can fetch professors
      ]);
      setAdvisors(advRes.data || []);
      setJuries(juryRes.data || []);
      setAvailableProfessors(profRes.data || []);
    } catch (error) {
      // Errors handled by interceptor
    } finally {
      setLoading(false);
    }
  }, [committeeId]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleAssignAdvisor = async () => {
    if (!selectedAdvisorId) return;
    setAssigningAdvisor(true);
    try {
      await apiClient.post(`/committees/${committeeId}/advisors`, {
        advisorId: parseInt(selectedAdvisorId),
        role: advisorRole,
      });
      toast.success("Advisor assigned successfully.");
      setSelectedAdvisorId("");
      fetchData();
    } catch (error) {
      // Error toast handled by apiClient interceptor
    } finally {
      setAssigningAdvisor(false);
    }
  };

  const handleRemoveAdvisor = async (assignmentId: number) => {
    if (!confirm("Are you sure you want to remove this advisor?")) return;
    try {
      await apiClient.delete(`/committees/${committeeId}/advisors/${assignmentId}`);
      toast.success("Advisor removed.");
      fetchData();
    } catch (error) {}
  };

  const handleAssignJury = async () => {
    if (!selectedJuryId) return;
    setAssigningJury(true);
    try {
      await apiClient.post(`/committees/${committeeId}/jury`, {
        juryMemberId: parseInt(selectedJuryId),
        juryType: juryType,
      });
      toast.success("Jury member assigned successfully.");
      setSelectedJuryId("");
      fetchData();
    } catch (error) {} finally {
      setAssigningJury(false);
    }
  };

  const handleRemoveJury = async (assignmentId: number) => {
    if (!confirm("Are you sure you want to remove this jury member?")) return;
    try {
      await apiClient.delete(`/committees/${committeeId}/jury/${assignmentId}`);
      toast.success("Jury member removed.");
      fetchData();
    } catch (error) {}
  };

  if (loading && advisors.length === 0) {
    return <div className="animate-pulse h-64 bg-white/5 rounded-xl border border-white/10 mb-6"></div>;
  }

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
      {/* Advisor Section */}
      <div className="bg-gray-900 border border-white/10 rounded-xl overflow-hidden flex flex-col">
        <div className="px-5 py-4 border-b border-white/5 bg-white/5 flex justify-between items-center">
          <h2 className="text-sm font-semibold text-white">Advisors</h2>
          <span className="text-xs bg-white/10 text-gray-300 px-2 py-1 rounded-full">{advisors.length} Assigned</span>
        </div>
        
        <div className="p-5 flex-1 flex flex-col">
          {/* Assignment Form */}
          <div className="flex flex-col sm:flex-row gap-3 mb-6 relative">
            <select
              value={selectedAdvisorId}
              onChange={(e) => setSelectedAdvisorId(e.target.value)}
              className="flex-1 bg-black/40 border border-white/10 text-sm text-white rounded-lg px-3 py-2 focus:ring-1 focus:ring-blue-500 outline-none"
              title="Select a professor from the system"
            >
              <option value="">Select Professor...</option>
              {availableProfessors.map((p) => (
                <option key={p.id} value={p.id}>{p.firstName} {p.lastName}</option>
              ))}
            </select>
            <select
              value={advisorRole}
              onChange={(e) => setAdvisorRole(e.target.value)}
              className="w-full sm:w-40 bg-black/40 border border-white/10 text-sm text-white rounded-lg px-3 py-2 focus:ring-1 focus:ring-blue-500 outline-none"
              title="Select the role of the advisor in this committee"
            >
              <option value="PRESIDENT">President</option>
              <option value="VICE_PRESIDENT">Vice President</option>
              <option value="MEMBER">Member</option>
            </select>
            <button
              onClick={handleAssignAdvisor}
              disabled={!selectedAdvisorId || assigningAdvisor}
              className="bg-blue-600 hover:bg-blue-500 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center min-w-[80px]"
            >
              {assigningAdvisor ? <span className="animate-spin text-lg">↻</span> : "Assign"}
            </button>
          </div>

          {/* List */}
          <div className="flex-1 space-y-3 overflow-y-auto max-h-[300px] pr-2">
            {advisors.length === 0 ? (
              <div className="text-center py-8 text-gray-500 text-sm">No advisors assigned yet.</div>
            ) : (
              advisors.map((adv) => (
                <div key={adv.id} className="flex items-center justify-between p-3 bg-white/5 border border-white/5 rounded-lg group hover:border-white/10 transition-colors">
                  <div>
                    <p className="text-sm font-medium text-white">{adv.advisor?.firstName} {adv.advisor?.lastName}</p>
                    <p className="text-xs text-gray-400">{adv.role} • Assigned {new Date(adv.assignedAt).toLocaleDateString()}</p>
                  </div>
                  <button
                    onClick={() => handleRemoveAdvisor(adv.id)}
                    className="text-red-400 opacity-0 group-hover:opacity-100 transition-opacity p-2 hover:bg-red-500/10 rounded-md"
                    title="Remove Advisor"
                  >
                    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" /></svg>
                  </button>
                </div>
              ))
            )}
          </div>
        </div>
      </div>

      {/* Jury Section */}
      <div className="bg-gray-900 border border-white/10 rounded-xl overflow-hidden flex flex-col">
        <div className="px-5 py-4 border-b border-white/5 bg-white/5 flex justify-between items-center">
          <h2 className="text-sm font-semibold text-white">Jury Members</h2>
          <span className="text-xs bg-white/10 text-gray-300 px-2 py-1 rounded-full">{juries.length} Assigned</span>
        </div>
        
        <div className="p-5 flex-1 flex flex-col">
          {/* Assignment Form */}
          <div className="flex flex-col sm:flex-row gap-3 mb-6">
            <select
              value={selectedJuryId}
              onChange={(e) => setSelectedJuryId(e.target.value)}
              className="flex-1 bg-black/40 border border-white/10 text-sm text-white rounded-lg px-3 py-2 focus:ring-1 focus:ring-blue-500 outline-none"
              title="Select a professor for the jury"
            >
              <option value="">Select Professor...</option>
              {availableProfessors.map((p) => (
                <option key={p.id} value={p.id}>{p.firstName} {p.lastName}</option>
              ))}
            </select>
            <select
              value={juryType}
              onChange={(e) => setJuryType(e.target.value)}
              className="w-full sm:w-40 bg-black/40 border border-white/10 text-sm text-white rounded-lg px-3 py-2 focus:ring-1 focus:ring-blue-500 outline-none"
              title="Internal: from the university, External: from outside, Additional: extra member"
            >
              <option value="INTERNAL">Internal</option>
              <option value="EXTERNAL">External</option>
              <option value="ADDITIONAL">Additional</option>
            </select>
            <button
              onClick={handleAssignJury}
              disabled={!selectedJuryId || assigningJury}
              className="bg-blue-600 hover:bg-blue-500 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center min-w-[80px]"
            >
              {assigningJury ? <span className="animate-spin text-lg">↻</span> : "Assign"}
            </button>
          </div>

          {/* List */}
          <div className="flex-1 space-y-3 overflow-y-auto max-h-[300px] pr-2">
            {juries.length === 0 ? (
              <div className="text-center py-8 text-gray-500 text-sm">No jury members assigned yet.</div>
            ) : (
              juries.map((j) => (
                <div key={j.id} className="flex items-center justify-between p-3 bg-white/5 border border-white/5 rounded-lg group hover:border-white/10 transition-colors">
                  <div>
                    <p className="text-sm font-medium text-white">{j.juryMember?.firstName} {j.juryMember?.lastName}</p>
                    <p className="text-xs text-gray-400">{j.juryType} • Assigned {new Date(j.assignedAt).toLocaleDateString()}</p>
                  </div>
                  <button
                    onClick={() => handleRemoveJury(j.id)}
                    className="text-red-400 opacity-0 group-hover:opacity-100 transition-opacity p-2 hover:bg-red-500/10 rounded-md"
                    title="Remove Jury Member"
                  >
                    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" /></svg>
                  </button>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
