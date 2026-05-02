"use client";

import { useState, useEffect } from "react";
import { Trash2, UserPlus, Loader2 } from "lucide-react";
import { JuryAssignment, JuryType } from "@/lib/committee-types";
import { assignJury, removeJury } from "@/lib/committee-assignment-api";
import apiClient from "@/lib/client";
import { toast } from "sonner";
import { handleApiError } from "@/lib/error-handler";

interface Props {
    committeeId: number;
    jury: JuryAssignment[];
    onRefresh: () => void;
}

interface ProfessorOption {
    userId: number;
    fullName: string;
}

export default function JuryAssignmentPanel({ committeeId, jury, onRefresh }: Props) {
    const [professors, setProfessors] = useState<ProfessorOption[]>([]);
    const [loadingProfs, setLoadingProfs] = useState(true);
    const [localJury, setLocalJury] = useState<JuryAssignment[]>(jury);

    const [selectedUserId, setSelectedUserId] = useState<number | null>(null);
    const [selectedType, setSelectedType] = useState<JuryType>("CORE");
    const [isSubmitting, setIsSubmitting] = useState(false);

    useEffect(() => {
        setLocalJury(jury);
    }, [jury]);

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

        const isDuplicate = localJury.some(j => j.juryId === selectedUserId);
        if (isDuplicate) {
            toast.error("This professor is already assigned.");
            return;
        }

        setIsSubmitting(true);
        try {
            await assignJury(committeeId, { juryId: selectedUserId, juryType: selectedType });
            toast.success("Jury member assigned.");
            setSelectedUserId(null);
            onRefresh();
        } catch (err) {
            handleApiError(err, "Jury Assignment");
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleRemove = async (assignmentId: number, name: string) => {
        if (!window.confirm(`Remove ${name} from jury?`)) return;

        const backup = [...localJury];
        setLocalJury(prev => prev.filter(j => j.assignmentId !== assignmentId));

        try {
            await removeJury(committeeId, assignmentId);
            toast.success("Jury member removed.");
        } catch (err) {
            setLocalJury(backup);
            handleApiError(err, "Jury Removal");
        }
    };

    return (
        <div className="rounded-2xl border border-white/10 bg-gray-900/70 p-6">
            <h3 className="mb-4 text-xl font-semibold text-white">Jury Members</h3>
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
                        value={selectedType}
                        onChange={(e) => setSelectedType(e.target.value as JuryType)}
                        disabled={isSubmitting}
                        className="w-full rounded-xl border border-white/10 bg-gray-900 px-4 py-3 text-sm text-white outline-none focus:border-blue-500 disabled:opacity-50"
                    >
                        <option value="CORE">Core</option>
                        <option value="SUBSTITUTE">Substitute</option>
                    </select>
                </div>
                <button type="submit" disabled={isSubmitting || !selectedUserId} className="flex h-[46px] items-center gap-2 rounded-xl bg-purple-600 px-6 text-sm font-semibold text-white hover:bg-purple-500 disabled:opacity-50 transition-colors">
                    {isSubmitting ? <Loader2 size={18} className="animate-spin" /> : <UserPlus size={18} />}
                    Assign
                </button>
            </form>

            <div className="space-y-3">
                {localJury.map(member => (
                    <div key={member.assignmentId} className="flex items-center justify-between rounded-xl border border-white/5 bg-gray-800/50 p-4 hover:bg-gray-800 transition-colors">
                        <div>
                            <p className="font-medium text-white">{member.fullName}</p>
                            <p className="text-sm text-gray-400">{member.email}</p>
                        </div>
                        <div className="flex items-center gap-4">
                            <span className="rounded-full bg-purple-500/20 px-3 py-1 text-xs font-medium text-purple-400">
                                {member.juryType}
                            </span>
                            <button onClick={() => handleRemove(member.assignmentId, member.fullName)} className="p-2 text-gray-400 hover:text-red-400 transition-colors">
                                <Trash2 size={18} />
                            </button>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}