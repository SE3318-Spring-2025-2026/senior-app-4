"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { getToken, getUser } from "@/lib/auth";
import Sidebar from "@/components/Sidebar";
import {
    fetchAdvisorAssignments,
    overrideAdvisorAssignment,
    type AdvisorAssignment,
} from "@/lib/advisor-requests-api";

const API_BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api/v1";

type Professor = { userId: number; fullName: string; email: string };

export default function AdvisorOverridePage() {
    const router = useRouter();
    const [role, setRole] = useState<string | null>(null);

    useEffect(() => {
        const token = getToken();
        const user = getUser();
        if (!token || !user) { router.replace("/auth/login"); return; }
        if (user.requiresPasswordChange) { router.replace("/auth/change-password"); return; }
        if (user.role !== "coordinator") { setRole("denied"); return; }
        setRole(user.role);
    }, [router]);

    if (role === null) return <Spinner />;
    if (role === "denied") return <AccessDenied />;
    return <AdvisorOverrideLayout />;
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

function AdvisorOverrideLayout() {
    return (
        <div className="min-h-screen bg-gray-950 flex">
            <Sidebar activePage="advisor-override" />
            <main className="flex-1 flex flex-col min-w-0">
                <div className="border-b border-white/5 px-8 py-4 flex items-center justify-between">
                    <div>
                        <h1 className="text-base font-semibold text-white">Advisor Override</h1>
                        <p className="text-xs text-gray-500 mt-0.5">Force-assign a professor as advisor to any group</p>
                    </div>
                    <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-white/5 border border-white/10">
                        <div className="w-1.5 h-1.5 rounded-full bg-green-400 animate-pulse" />
                        <span className="text-xs text-gray-400">Coordinator</span>
                    </div>
                </div>
                <div className="flex-1 p-8">
                    <OverridePanel />
                </div>
            </main>
        </div>
    );
}

function OverridePanel() {
    const [assignments, setAssignments] = useState<AdvisorAssignment[]>([]);
    const [professors, setProfessors] = useState<Professor[]>([]);
    const [loading, setLoading] = useState(true);

    const [selectedTeamId, setSelectedTeamId] = useState("");
    const [selectedAdvisorId, setSelectedAdvisorId] = useState("");
    const [overrideReason, setOverrideReason] = useState("");
    const [showConfirm, setShowConfirm] = useState(false);
    const [processing, setProcessing] = useState(false);

    useEffect(() => {
        const token = getToken();
        Promise.all([
            fetchAdvisorAssignments(),
            fetch(`${API_BASE}/users?role=professor`, {
                headers: { Authorization: `Bearer ${token ?? ""}` },
            }).then((r) => r.ok ? r.json() : { data: [] }),
        ]).then(([assignmentData, userData]) => {
            setAssignments(assignmentData);
            setProfessors(userData.data ?? []);
        }).catch(() => {
            toast.error("Failed to load data.");
        }).finally(() => setLoading(false));
    }, []);

    const selectedTeam = assignments.find((a) => a.teamId === selectedTeamId);
    const hasExistingAdvisor = selectedTeam?.advisorId != null;

    const handleOverrideClick = () => {
        if (!selectedTeamId || !selectedAdvisorId) {
            toast.error("Please select both a group and a professor.");
            return;
        }
        setShowConfirm(true);
    };

    const handleConfirm = async () => {
        setProcessing(true);
        try {
            await overrideAdvisorAssignment({
                teamId: selectedTeamId,
                advisorId: selectedAdvisorId,
                reason: overrideReason.trim() || undefined,
            });
            toast.success("Advisor assignment overridden successfully.");
            // refresh assignments
            const updated = await fetchAdvisorAssignments();
            setAssignments(updated);
            setSelectedTeamId("");
            setSelectedAdvisorId("");
            setOverrideReason("");
        } catch (err: unknown) {
            toast.error(err instanceof Error ? err.message : "Override failed.");
        } finally {
            setProcessing(false);
            setShowConfirm(false);
        }
    };

    if (loading) {
        return (
            <div className="bg-gray-900 border border-white/8 rounded-2xl p-12 flex items-center justify-center">
                <svg className="w-5 h-5 animate-spin text-blue-500" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                </svg>
            </div>
        );
    }

    return (
        <div className="max-w-2xl mx-auto space-y-5">
            {/* Control Panel */}
            <div className="bg-gray-900 border border-white/8 rounded-2xl overflow-hidden">
                <div className="px-6 py-4 border-b border-white/5 flex items-center gap-3">
                    <div className="w-8 h-8 rounded-lg bg-orange-500/10 border border-orange-500/20 flex items-center justify-center">
                        <svg className="w-4 h-4 text-orange-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M13.5 16.875h3.375m0 0h3.375m-3.375 0V13.5m0 3.375v3.375M6 10.5h2.25a2.25 2.25 0 002.25-2.25V6a2.25 2.25 0 00-2.25-2.25H6A2.25 2.25 0 003.75 6v2.25A2.25 2.25 0 006 10.5zm0 9.75h2.25A2.25 2.25 0 0010.5 18v-2.25a2.25 2.25 0 00-2.25-2.25H6a2.25 2.25 0 00-2.25 2.25V18A2.25 2.25 0 006 20.25zm9.75-9.75H18a2.25 2.25 0 002.25-2.25V6A2.25 2.25 0 0018 3.75h-2.25A2.25 2.25 0 0013.5 6v2.25a2.25 2.25 0 002.25 2.25z" />
                        </svg>
                    </div>
                    <div>
                        <p className="text-sm font-semibold text-white">Group-Advisor Match</p>
                        <p className="text-xs text-gray-500">Force-assign any professor to any group</p>
                    </div>
                </div>

                <div className="p-6 space-y-5">
                    {/* Group Select */}
                    <div className="space-y-2">
                        <label className="text-xs font-medium text-gray-400 uppercase tracking-wider">Select Group</label>
                        <select
                            value={selectedTeamId}
                            onChange={(e) => setSelectedTeamId(e.target.value)}
                            className="w-full px-4 py-3 bg-gray-800 border border-white/10 rounded-xl text-sm text-white
                                       focus:outline-none focus:border-blue-500/60 focus:ring-1 focus:ring-blue-500/30
                                       transition-all appearance-none"
                        >
                            <option value="" className="bg-gray-800">— Choose a group —</option>
                            {assignments.map((a) => (
                                <option key={a.teamId} value={a.teamId} className="bg-gray-800">
                                    {a.teamName}{a.advisorId ? ` (advisor: ${a.advisorName})` : " (no advisor)"}
                                </option>
                            ))}
                        </select>
                        {selectedTeam && hasExistingAdvisor && (
                            <p className="text-xs text-orange-400 flex items-center gap-1">
                                <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
                                </svg>
                                This group already has an advisor. Override will replace the existing assignment.
                            </p>
                        )}
                    </div>

                    {/* Professor Select */}
                    <div className="space-y-2">
                        <label className="text-xs font-medium text-gray-400 uppercase tracking-wider">Select Professor</label>
                        <select
                            value={selectedAdvisorId}
                            onChange={(e) => setSelectedAdvisorId(e.target.value)}
                            className="w-full px-4 py-3 bg-gray-800 border border-white/10 rounded-xl text-sm text-white
                                       focus:outline-none focus:border-blue-500/60 focus:ring-1 focus:ring-blue-500/30
                                       transition-all appearance-none"
                        >
                            <option value="" className="bg-gray-800">— Choose a professor —</option>
                            {professors.map((p) => (
                                <option key={p.userId} value={String(p.userId)} className="bg-gray-800">
                                    {p.fullName}
                                </option>
                            ))}
                        </select>
                    </div>

                    {/* Reason */}
                    <div className="space-y-2">
                        <label className="text-xs font-medium text-gray-400 uppercase tracking-wider">
                            Reason
                            <span className="ml-1 text-gray-600 normal-case font-normal">(optional)</span>
                        </label>
                        <textarea
                            value={overrideReason}
                            onChange={(e) => setOverrideReason(e.target.value)}
                            placeholder="Reason for this override assignment..."
                            rows={2}
                            className="w-full px-4 py-3 bg-gray-800 border border-white/10 rounded-xl text-sm text-white
                                       placeholder:text-gray-600 focus:outline-none focus:border-blue-500/60
                                       focus:ring-1 focus:ring-blue-500/30 transition-all resize-none"
                        />
                    </div>
                </div>

                <div className="px-6 pb-6">
                    <button
                        onClick={handleOverrideClick}
                        className="flex items-center gap-2 px-5 py-2.5 rounded-xl text-sm font-medium transition-all
                                   bg-orange-600 text-white hover:bg-orange-500 active:scale-95
                                   shadow-lg shadow-orange-600/20"
                    >
                        <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.5 16.875h3.375m0 0h3.375m-3.375 0V13.5m0 3.375v3.375M6 10.5h2.25a2.25 2.25 0 002.25-2.25V6a2.25 2.25 0 00-2.25-2.25H6A2.25 2.25 0 003.75 6v2.25A2.25 2.25 0 006 10.5z" />
                        </svg>
                        Override Assignment
                    </button>
                </div>
            </div>

            {/* Current Assignments Table */}
            <div className="bg-gray-900 border border-white/8 rounded-2xl overflow-hidden">
                <div className="px-6 py-4 border-b border-white/5">
                    <p className="text-sm font-semibold text-white">Current Assignments</p>
                    <p className="text-xs text-gray-500 mt-0.5">{assignments.filter(a => a.advisorId).length} of {assignments.length} groups have an advisor</p>
                </div>
                <div className="overflow-x-auto">
                    <table className="w-full">
                        <thead>
                            <tr className="border-b border-white/5">
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Group</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Advisor</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Type</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-white/5">
                            {assignments.map((a) => (
                                <tr key={a.teamId} className="hover:bg-white/[0.02] transition-colors">
                                    <td className="px-6 py-3 text-sm text-white">{a.teamName}</td>
                                    <td className="px-6 py-3 text-sm">
                                        {a.advisorName
                                            ? <span className="text-gray-300">{a.advisorName}</span>
                                            : <span className="text-gray-600 italic">Unassigned</span>
                                        }
                                    </td>
                                    <td className="px-6 py-3">
                                        {a.assignmentType ? (
                                            <span className={`text-xs font-medium px-2 py-0.5 rounded-full border ${
                                                a.assignmentType === "OVERRIDDEN"
                                                    ? "text-orange-400 bg-orange-400/10 border-orange-400/20"
                                                    : "text-blue-400 bg-blue-400/10 border-blue-400/20"
                                            }`}>
                                                {a.assignmentType}
                                            </span>
                                        ) : (
                                            <span className="text-xs text-gray-600">—</span>
                                        )}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* Confirm Modal */}
            {showConfirm && (
                <ConfirmOverrideModal
                    hasExistingAdvisor={hasExistingAdvisor}
                    processing={processing}
                    onConfirm={handleConfirm}
                    onCancel={() => setShowConfirm(false)}
                />
            )}
        </div>
    );
}

function ConfirmOverrideModal({
    hasExistingAdvisor,
    processing,
    onConfirm,
    onCancel,
}: {
    hasExistingAdvisor: boolean;
    processing: boolean;
    onConfirm: () => void;
    onCancel: () => void;
}) {
    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm">
            <div className="bg-gray-900 border border-white/10 rounded-2xl p-6 w-full max-w-md mx-4 shadow-2xl">
                <div className="flex items-start gap-4">
                    <div className="w-10 h-10 rounded-xl bg-orange-500/10 border border-orange-500/20 flex items-center justify-center shrink-0">
                        <svg className="w-5 h-5 text-orange-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
                        </svg>
                    </div>
                    <div className="flex-1">
                        <h3 className="text-sm font-semibold text-white">Confirm Override</h3>
                        <p className="text-sm text-gray-400 mt-1">
                            {hasExistingAdvisor
                                ? "Existing assignment will be overwritten. Both the previous and new advisor will be notified."
                                : "This group will be assigned to the selected professor. The professor will be notified."}
                        </p>
                    </div>
                </div>

                <div className="flex items-center justify-end gap-3 mt-6">
                    <button
                        onClick={onCancel}
                        disabled={processing}
                        className="px-4 py-2 rounded-lg text-sm font-medium text-gray-400
                                   hover:text-white hover:bg-white/5 transition-colors
                                   disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        Cancel
                    </button>
                    <button
                        onClick={onConfirm}
                        disabled={processing}
                        className="flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium
                                   bg-orange-600 text-white hover:bg-orange-500 active:scale-95
                                   transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        {processing ? (
                            <>
                                <svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                                </svg>
                                Overriding...
                            </>
                        ) : "Confirm Override"}
                    </button>
                </div>
            </div>
        </div>
    );
}
