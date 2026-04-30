"use client";

import { useState, useEffect } from "react";
import { useParams, useRouter } from "next/navigation";
import Sidebar from "@/components/Sidebar";
import { useAuthGuard } from "@/hooks/useAuthGuard";
import { fetchCommitteeById, removeAdvisor, Committee } from "@/lib/committees-api";
import AssignAdvisorForm from "@/components/committees/AssignAdvisorForm";
import { toast } from "sonner";
import Link from "next/link";

export default function CoordinatorCommitteeDetailsPage() {
    const authStatus = useAuthGuard("coordinator");

    if (authStatus === "loading") return (
        <div className="min-h-screen bg-gray-950 flex items-center justify-center">
            <svg className="w-6 h-6 animate-spin text-blue-500" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
            </svg>
        </div>
    );

    if (authStatus === "denied") return <AccessDenied />;
    return <DashboardLayout />;
}

function AccessDenied() {
    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-950">
            <div className="text-center">
                <h1 className="text-lg font-semibold text-white">Access Restricted</h1>
            </div>
        </div>
    );
}

function DashboardLayout() {
    const { id } = useParams();
    const router = useRouter();
    const committeeId = Number(id);

    const [committee, setCommittee] = useState<Committee | null>(null);
    const [loading, setLoading] = useState(true);
    const [showAssignModal, setShowAssignModal] = useState(false);
    const [removingId, setRemovingId] = useState<number | null>(null);

    const loadCommittee = async () => {
        setLoading(true);
        try {
            const data = await fetchCommitteeById(committeeId);
            setCommittee(data);
        } catch (error) {
            console.error("Failed to fetch committee", error);
            toast.error("Committee not found");
            router.push("/coordinator/committees");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (committeeId) {
            loadCommittee();
        }
    }, [committeeId]);

    const handleRemoveAdvisor = async (advisorId: number) => {
        if (!confirm("Are you sure you want to remove this advisor from the committee?")) return;
        
        setRemovingId(advisorId);
        try {
            await removeAdvisor(committeeId, advisorId);
            toast.success("Advisor removed from committee");
            loadCommittee();
        } catch (error) {
            console.error("Failed to remove advisor", error);
        } finally {
            setRemovingId(null);
        }
    };

    const handleAssignSuccess = () => {
        setShowAssignModal(false);
        loadCommittee();
    };

    return (
        <div className="min-h-screen bg-gray-950 flex">
            <Sidebar activePage="committees" />

            <main className="flex-1 flex flex-col min-w-0 relative">
                {/* Header */}
                <div className="border-b border-white/5 px-8 py-4 flex items-center justify-between">
                    <div>
                        <div className="flex items-center gap-2 text-sm text-gray-500 mb-1">
                            <Link href="/coordinator/committees" className="hover:text-white transition-colors">Committees</Link>
                            <span>/</span>
                            <span className="text-gray-300">Details</span>
                        </div>
                        <h1 className="text-base font-semibold text-white flex items-center gap-3">
                            {committee?.committeeName || "Loading..."}
                            {committee && (
                                <span className={`px-2 py-0.5 rounded text-xs font-medium border ${
                                    committee.status === 'ACTIVE' 
                                    ? 'bg-green-500/10 text-green-400 border-green-500/20' 
                                    : 'bg-gray-500/10 text-gray-400 border-gray-500/20'
                                }`}>
                                    {committee.status}
                                </span>
                            )}
                        </h1>
                    </div>
                </div>

                <div className="flex-1 p-8">
                    {loading && !committee ? (
                        <div className="text-center py-10">
                            <div className="inline-block w-8 h-8 border-4 border-white/20 border-t-blue-500 rounded-full animate-spin"></div>
                        </div>
                    ) : committee && (
                        <div className="max-w-5xl mx-auto space-y-6">
                            
                            <div className="bg-gray-900 border border-white/10 rounded-2xl p-6">
                                <h2 className="text-lg font-semibold text-white mb-2">Description</h2>
                                <p className="text-gray-400 text-sm">{committee.description || "No description provided."}</p>
                            </div>

                            <div className="bg-gray-900 border border-white/10 rounded-2xl overflow-hidden shadow-xl shadow-black/20">
                                <div className="p-6 border-b border-white/5 flex justify-between items-center">
                                    <h2 className="text-lg font-semibold text-white">Assigned Advisors</h2>
                                    <button
                                        onClick={() => setShowAssignModal(true)}
                                        className="bg-blue-600/20 text-blue-400 border border-blue-500/30 px-3 py-1.5 rounded-lg text-sm font-medium hover:bg-blue-600/30 transition-colors"
                                    >
                                        + Assign Advisor
                                    </button>
                                </div>
                                
                                {committee.advisors && committee.advisors.length > 0 ? (
                                    <table className="w-full text-left border-collapse">
                                        <thead>
                                            <tr className="bg-white/5 border-b border-white/10">
                                                <th className="px-6 py-4 text-xs font-semibold text-gray-400 uppercase tracking-wider">Advisor Name</th>
                                                <th className="px-6 py-4 text-xs font-semibold text-gray-400 uppercase tracking-wider">Role</th>
                                                <th className="px-6 py-4 text-xs font-semibold text-gray-400 uppercase tracking-wider text-right">Actions</th>
                                            </tr>
                                        </thead>
                                        <tbody className="divide-y divide-white/5">
                                            {committee.advisors.map((assignment) => (
                                                <tr key={assignment.committeeAdvisorId} className="hover:bg-white/5 transition-colors">
                                                    <td className="px-6 py-4">
                                                        <div className="text-sm font-medium text-white">{assignment.advisor?.fullName || 'Unknown'}</div>
                                                        <div className="text-xs text-gray-500">{assignment.advisor?.email}</div>
                                                    </td>
                                                    <td className="px-6 py-4">
                                                        <span className="inline-flex items-center px-2.5 py-1 rounded-md bg-purple-500/10 text-purple-400 border border-purple-500/20 text-xs font-medium">
                                                            {assignment.role}
                                                        </span>
                                                    </td>
                                                    <td className="px-6 py-4 text-right">
                                                        <button
                                                            onClick={() => handleRemoveAdvisor(assignment.advisor.userId)}
                                                            disabled={removingId === assignment.advisor.userId}
                                                            className="text-sm bg-red-500/10 text-red-400 border border-red-500/20 px-3 py-1.5 rounded-lg hover:bg-red-500/20 transition-colors disabled:opacity-50"
                                                        >
                                                            {removingId === assignment.advisor.userId ? 'Removing...' : 'Remove'}
                                                        </button>
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                ) : (
                                    <div className="text-center py-10">
                                        <p className="text-gray-400">No advisors assigned to this committee yet.</p>
                                    </div>
                                )}
                            </div>
                        </div>
                    )}
                </div>

                {/* Assign Modal */}
                {showAssignModal && (
                    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
                        <div className="bg-gray-900 border border-white/10 w-full max-w-md rounded-2xl shadow-2xl p-6">
                            <h2 className="text-xl font-bold text-white mb-6">Assign Advisor to Committee</h2>
                            <AssignAdvisorForm 
                                committeeId={committeeId}
                                onSuccess={handleAssignSuccess} 
                                onCancel={() => setShowAssignModal(false)} 
                            />
                        </div>
                    </div>
                )}
            </main>
        </div>
    );
}
