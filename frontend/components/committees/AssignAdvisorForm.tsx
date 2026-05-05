"use client";

import { useState, useEffect } from "react";
import { toast } from "sonner";
import apiClient from "@/lib/client";
import { assignAdvisor } from "@/lib/committees-api";
import { UserRole } from "@/types/enums";

interface AssignAdvisorFormProps {
    committeeId: number;
    onSuccess: () => void;
    onCancel: () => void;
}

type Professor = {
    userId: number;
    fullName: string;
    email: string;
};

export default function AssignAdvisorForm({ committeeId, onSuccess, onCancel }: AssignAdvisorFormProps) {
    const [professors, setProfessors] = useState<Professor[]>([]);
    const [selectedAdvisorId, setSelectedAdvisorId] = useState<string>("");
    const [role, setRole] = useState<string>("MEMBER");
    const [loading, setLoading] = useState(false);
    const [fetching, setFetching] = useState(true);

    useEffect(() => {
        apiClient.get(`/users?role=${UserRole.PROFESSOR}`)
            .then(res => {
                const data = Array.isArray(res.data) ? res.data : res.data.data || [];
                setProfessors(data);
            })
            .catch(err => {
                console.error("Failed to fetch professors", err);
                toast.error("Failed to load professors");
            })
            .finally(() => setFetching(false));
    }, []);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        
        if (!selectedAdvisorId) {
            toast.error("Please select an advisor");
            return;
        }

        setLoading(true);
        try {
            await assignAdvisor(committeeId, Number(selectedAdvisorId), role);
            toast.success("Advisor assigned successfully");
            onSuccess();
        } catch (error: any) {
            const msg = error?.message ?? "Failed to assign advisor";
            console.error("Failed to assign advisor", error);
            toast.error(msg);
        } finally {
            setLoading(false);
        }
    };

    return (
        <form onSubmit={handleSubmit} className="space-y-4">
            <div>
                <label className="block text-sm font-medium text-gray-300 mb-1">Select Professor</label>
                {fetching ? (
                    <div className="text-sm text-gray-400">Loading professors...</div>
                ) : (
                    <select
                        value={selectedAdvisorId}
                        onChange={(e) => setSelectedAdvisorId(e.target.value)}
                        className="w-full bg-black/40 border border-white/10 text-white rounded-xl px-4 py-3 focus:ring-1 focus:ring-blue-500 outline-none"
                        required
                    >
                        <option value="" disabled>-- Select a Professor --</option>
                        {professors.map(prof => (
                            <option key={prof.userId} value={prof.userId}>
                                {prof.fullName} ({prof.email})
                            </option>
                        ))}
                    </select>
                )}
            </div>
            
            <div>
                <label className="block text-sm font-medium text-gray-300 mb-1">Role in Committee</label>
                <select
                    value={role}
                    onChange={(e) => setRole(e.target.value)}
                    className="w-full bg-black/40 border border-white/10 text-white rounded-xl px-4 py-3 focus:ring-1 focus:ring-blue-500 outline-none"
                    required
                >
                    <option value="PRESIDENT">President</option>
                    <option value="VICE_PRESIDENT">Vice President</option>
                    <option value="MEMBER">Member</option>
                </select>
            </div>

            <div className="flex justify-end gap-3 pt-4 border-t border-white/10">
                <button
                    type="button"
                    onClick={onCancel}
                    className="px-4 py-2 text-sm font-medium text-gray-400 hover:text-white transition-colors"
                    disabled={loading}
                >
                    Cancel
                </button>
                <button
                    type="submit"
                    disabled={loading || !selectedAdvisorId || fetching}
                    className="px-4 py-2 text-sm font-medium bg-blue-600 text-white rounded-lg hover:bg-blue-500 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                >
                    {loading ? "Assigning..." : "Assign Advisor"}
                </button>
            </div>
        </form>
    );
}
