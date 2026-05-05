"use client";

import { useState, useEffect } from "react";
import { toast } from "sonner";
import apiClient from "@/lib/client";
import { assignJury } from "@/lib/committees-api";
import { UserRole } from "@/types/enums";

interface AssignJuryFormProps {
    committeeId: number;
    /** IDs already assigned so we can disable them */
    assignedProfessorIds?: number[];
    onSuccess: () => void;
    onCancel: () => void;
}

type Professor = {
    userId: number;
    fullName: string;
    email: string;
};

const JURY_TYPES = [
    { value: "INTERNAL", label: "Internal" },
    { value: "EXTERNAL", label: "External" },
    { value: "ADDITIONAL", label: "Additional" },
];

export default function AssignJuryForm({
    committeeId,
    assignedProfessorIds = [],
    onSuccess,
    onCancel,
}: AssignJuryFormProps) {
    const [professors, setProfessors] = useState<Professor[]>([]);
    const [selectedProfessorId, setSelectedProfessorId] = useState<string>("");
    const [juryType, setJuryType] = useState<string>("INTERNAL");
    const [loading, setLoading] = useState(false);
    const [fetching, setFetching] = useState(true);

    useEffect(() => {
        apiClient
            .get(`/users?role=${UserRole.PROFESSOR}`)
            .then((res) => {
                const data = Array.isArray(res.data)
                    ? res.data
                    : res.data?.data ?? [];
                setProfessors(data);
            })
            .catch((err) => {
                console.error("Failed to fetch professors", err);
                toast.error("Failed to load professors");
            })
            .finally(() => setFetching(false));
    }, []);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!selectedProfessorId) {
            toast.error("Please select a professor");
            return;
        }

        setLoading(true);
        try {
            await assignJury(committeeId, Number(selectedProfessorId), juryType);
            toast.success("Jury member assigned successfully");
            onSuccess();
        } catch (error: any) {
            const msg =
                error?.response?.data?.message ||
                error?.message ||
                "Failed to assign jury member";
            toast.error(msg);
        } finally {
            setLoading(false);
        }
    };

    const available = professors.filter(
        (p) => !assignedProfessorIds.includes(p.userId)
    );

    return (
        <form onSubmit={handleSubmit} className="space-y-4">
            {/* Professor dropdown */}
            <div>
                <label className="block text-sm font-medium text-gray-300 mb-1">
                    Select Professor
                </label>
                {fetching ? (
                    <div className="text-sm text-gray-400 py-2">Loading professors...</div>
                ) : (
                    <select
                        value={selectedProfessorId}
                        onChange={(e) => setSelectedProfessorId(e.target.value)}
                        className="w-full bg-black/40 border border-white/10 text-white rounded-xl px-4 py-3 focus:ring-1 focus:ring-violet-500 outline-none"
                        required
                    >
                        <option value="" disabled>
                            -- Select a Professor --
                        </option>
                        {available.map((prof) => (
                            <option key={prof.userId} value={prof.userId}>
                                {prof.fullName} ({prof.email})
                            </option>
                        ))}
                        {available.length === 0 && (
                            <option disabled value="">
                                All professors already assigned
                            </option>
                        )}
                    </select>
                )}
            </div>

            {/* Jury type dropdown */}
            <div>
                <label className="block text-sm font-medium text-gray-300 mb-1">
                    Jury Type
                </label>
                <select
                    value={juryType}
                    onChange={(e) => setJuryType(e.target.value)}
                    className="w-full bg-black/40 border border-white/10 text-white rounded-xl px-4 py-3 focus:ring-1 focus:ring-violet-500 outline-none"
                    required
                >
                    {JURY_TYPES.map((t) => (
                        <option key={t.value} value={t.value}>
                            {t.label}
                        </option>
                    ))}
                </select>
            </div>

            {/* Actions */}
            <div className="flex justify-end gap-3 pt-4 border-t border-white/10">
                <button
                    type="button"
                    onClick={onCancel}
                    disabled={loading}
                    className="px-4 py-2 text-sm font-medium text-gray-400 hover:text-white transition-colors"
                >
                    Cancel
                </button>
                <button
                    type="submit"
                    disabled={loading || !selectedProfessorId || fetching}
                    className="px-4 py-2 text-sm font-medium bg-violet-600 text-white rounded-lg hover:bg-violet-500 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                >
                    {loading ? "Assigning..." : "Assign Jury Member"}
                </button>
            </div>
        </form>
    );
}
