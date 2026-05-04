"use client";

import { useState, useEffect } from "react";
import { Trash2, UserPlus, Loader2 } from "lucide-react";
import { AdvisorAssignment, AdvisorRole } from "@/lib/committee-types";
import { assignAdvisor, removeAdvisor } from "@/lib/committee-assignment-api";
import apiClient from "@/lib/client";
import { toast } from "sonner";
import { handleApiError } from "@/lib/error-handler";

interface Props {
    committeeId: number;
    advisors: AdvisorAssignment[];
    onRefresh: () => void;
}

interface ProfessorOption {
    userId: number;
    fullName: string;
}

export default function AdvisorAssignmentPanel({ committeeId, advisors, onRefresh }: Props) {
    const [professors, setProfessors] = useState<ProfessorOption[]>([]);
    const [loadingProfs, setLoadingProfs] = useState(true);
    const [localAdvisors, setLocalAdvisors] = useState<AdvisorAssignment[]>(advisors);
    
    const [selectedUserId, setSelectedUserId] = useState<number | null>(null);
    const [selectedRole, setSelectedRole] = useState<AdvisorRole>("MEMBER");
    const [isSubmitting, setIsSubmitting] = useState(false);

    useEffect(() => {
        setLocalAdvisors(advisors);
    }, [advisors]);

    useEffect(() => {
        let isMounted = true;
        apiClient.get(`/users?role=PROFESSOR`)
            .then((res: any) => { 
                if (!isMounted) return;
                const data = Array.isArray(res.data) ? res.data : (res.data?.data || []);
                setProfessors(data.map((u: any) => ({
                    userId: u.userId || u.id,
                    fullName: u.fullName || u.full_name || `${u.firstName || ''} ${u.lastName || ''}`.trim()
                })));
            })
            .catch((err) => { 
                if (isMounted) handleApiError(err, "Professors");
            })
            .finally(() => {
                if (isMounted) setLoadingProfs(false);
            });
        return () => { isMounted = false; };
    }, []);

    const handleAssign = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!selectedUserId) return;

        const isDuplicate = localAdvisors.some(a => a.advisorId === selectedUserId);
        if (isDuplicate) {
            toast.error("This professor is already assigned.");
            return;
        }

        setIsSubmitting(true);
        try {
            await assignAdvisor(committeeId, { advisorId: selectedUserId, role: selectedRole });
            toast.success("Advisor assigned.");
            setSelectedUserId(null);
            onRefresh();
        } catch (err) {
            handleApiError(err, "Assignment");
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleRemove = async (assignmentId: number, name: string) => {
        if (!window.confirm(`Are you sure you want to remove ${name}?`)) return;

        const backup = [...localAdvisors];
        setLocalAdvisors(prev => prev.filter(a => a.assignmentId !== assignmentId));

        try {
            await removeAdvisor(committeeId, assignmentId);
            toast.success("Advisor removed.");
        } catch (err) {
            setLocalAdvisors(backup);
            handleApiError(err, "Removal");
        }
    };

    return (
        <div className="rounded-2xl border border-white/10 bg-gray-900/70 p-6">
            <h3 className="mb-4 text-xl font-semibold text-white">Advisors</h3>
            <form onSubmit={handleAssign} className="mb-8 flex flex-wrap items-end gap-4 rounded-xl border border-white/5 bg-black/20 p-4">
                <div className="flex-1 min-w-[200px]">
                    <select
                        value={selectedUserId || ""}
                        onChange={(e) => setSelectedUserId(e.target.value === "" ? null : Number(e.target.value))}
                        disabled={loadingProfs || isSubmitting}
                        className="w-full rounded-xl border border-white/10 bg-gray-900 px-4 py-3 text-sm text-white outline-none focus:border-blue-500 disabled:opacity-50"
                    >
                        <option value="">-- Choose Professor --</option>
                        {professors.map(prof => <option key={prof.userId} value={prof.userId}>{prof.fullName}</option>)}
                    </select>
                </div>
                <div className="w-48">
                    <select
                        value={selectedRole}
                        onChange={(e) => setSelectedRole(e.target.value as AdvisorRole)}
                        disabled={isSubmitting}
                        className="w-full rounded-xl border border-white/10 bg-gray-900 px-4 py-3 text-sm text-white outline-none focus:border-blue-500 disabled:opacity-50"
                    >
                        <option value="PRESIDENT">President</option>
                        <option value="VICE_PRESIDENT">Vice President</option>
                        <option value="MEMBER">Member</option>
                    </select>
                </div>
                <button type="submit" disabled={isSubmitting || !selectedUserId} className="flex h-[46px] items-center gap-2 rounded-xl bg-blue-600 px-6 text-sm font-semibold text-white hover:bg-blue-500 disabled:opacity-50 transition-colors">
                    {isSubmitting ? <Loader2 size={18} className="animate-spin" /> : <UserPlus size={18} />}
                    Assign
                </button>
            </form>

            <div className="space-y-3">
                {localAdvisors.map(adv => (
                    <div key={adv.assignmentId} className="flex items-center justify-between rounded-xl border border-white/5 bg-gray-800/50 p-4 hover:bg-gray-800 transition-colors">
                        <div>
                            <p className="font-medium text-white">{adv.fullName}</p>
                            <p className="text-sm text-gray-400">{adv.email}</p>
                        </div>
                        <div className="flex items-center gap-4">
                            <span className="rounded-full bg-blue-500/20 px-3 py-1 text-xs font-medium text-blue-400">
                                {adv.role.replace('_', ' ')}
                            </span>
                            <button onClick={() => handleRemove(adv.assignmentId, adv.fullName)} className="p-2 text-gray-400 hover:text-red-400 transition-colors">
                                <Trash2 size={18} />
                            </button>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}