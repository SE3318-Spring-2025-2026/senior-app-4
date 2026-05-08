"use client";

import { Suspense, useCallback, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { toast } from "sonner";
import { getToken, getUser } from "@/lib/auth";
import Sidebar from "@/components/Sidebar";
import { fetchSubmissions, type SubmissionSummary } from "@/lib/submissions-api";

function Spinner() {
    return (
        <div className="flex items-center justify-center py-16">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500" />
        </div>
    );
}

const STATUS_LABELS: Record<string, string> = {
    PENDING_REVIEW: "Pending Review",
    UNDER_REVIEW: "Under Review",
    REVISION_REQUESTED: "Revision Requested",
    APPROVED: "Approved",
    GRADED: "Graded",
    SUBMITTED: "Submitted",
    SUPERSEDED: "Superseded",
    REJECTED: "Rejected",
};

const STATUS_COLORS: Record<string, string> = {
    PENDING_REVIEW: "bg-yellow-500/10 text-yellow-400 border border-yellow-500/20",
    UNDER_REVIEW: "bg-blue-500/10 text-blue-400 border border-blue-500/20",
    REVISION_REQUESTED: "bg-orange-500/10 text-orange-400 border border-orange-500/20",
    APPROVED: "bg-green-500/10 text-green-400 border border-green-500/20",
    GRADED: "bg-purple-500/10 text-purple-400 border border-purple-500/20",
    SUBMITTED: "bg-gray-500/10 text-gray-400 border border-gray-500/20",
    SUPERSEDED: "bg-gray-500/10 text-gray-500 border border-gray-500/20",
    REJECTED: "bg-red-500/10 text-red-400 border border-red-500/20",
};

export default function ProfessorSubmissionsPage() {
    return (
        <Suspense fallback={<div className="min-h-screen bg-gray-950" />}>
            <ProfessorSubmissionsPageContent />
        </Suspense>
    );
}

function ProfessorSubmissionsPageContent() {
    const router = useRouter();
    const searchParams = useSearchParams();
    const groupId = searchParams.get("groupId");

    const [role, setRole] = useState<string | null>(null);
    const [submissions, setSubmissions] = useState<SubmissionSummary[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const token = getToken();
        const user = getUser();
        if (!token || !user) { router.replace("/auth/login"); return; }
        if (user.requiresPasswordChange) { router.replace("/auth/change-password"); return; }
        if (user.role !== "professor") { queueMicrotask(() => setRole("denied")); return; }
        queueMicrotask(() => setRole(user.role));
    }, [router]);

    const loadSubmissions = useCallback(async () => {
        setLoading(true);
        try {
            const res = await fetchSubmissions({ size: 100, teamId: groupId ?? undefined });
            setSubmissions(res.data ?? []);
        } catch (err) {
            toast.error(err instanceof Error ? err.message : "Failed to load submissions.");
        } finally {
            setLoading(false);
        }
    }, [groupId]);

    useEffect(() => {
        if (role === "professor") loadSubmissions();
    }, [role, loadSubmissions]);

    if (role === null) return <div className="min-h-screen bg-gray-950" />;
    if (role === "denied") return <p className="p-8 text-red-400">Access denied.</p>;

    return (
        <div className="flex min-h-screen bg-gray-950">
            <Sidebar activePage="my-advisees" />
            <main className="flex-1 p-8">
                <div className="mb-6 flex items-center gap-4">
                    <button
                        onClick={() => router.push("/professor/my-advisees")}
                        className="flex items-center gap-2 text-sm text-gray-400 hover:text-white transition-colors"
                    >
                        <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
                        </svg>
                        Back
                    </button>
                    <div>
                        <h1 className="text-lg font-semibold text-white">Submissions to Grade</h1>
                        {groupId && <p className="text-xs text-gray-500 mt-0.5">Group #{groupId}</p>}
                    </div>
                </div>

                {loading ? (
                    <Spinner />
                ) : submissions.length === 0 ? (
                    <div className="bg-gray-900 border border-white/8 rounded-2xl p-12 flex flex-col items-center gap-3">
                        <p className="text-sm font-medium text-white">No submissions found</p>
                        <p className="text-xs text-gray-500">This group has not submitted any deliverables yet.</p>
                    </div>
                ) : (
                    <div className="bg-gray-900 border border-white/8 rounded-2xl overflow-hidden">
                        <table className="min-w-full divide-y divide-white/5">
                            <thead>
                                <tr>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Team</th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Type</th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Version</th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Submitted</th>
                                    <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-white/5">
                                {submissions.map((s) => (
                                    <tr key={s.id} className="hover:bg-white/[0.02] transition-colors">
                                        <td className="px-6 py-4 text-sm font-medium text-white">
                                            {s.teamName ?? `Team #${s.teamId}`}
                                        </td>
                                        <td className="px-6 py-4 text-sm text-gray-300">
                                            {s.deliverableType.replace(/_/g, " ")}
                                        </td>
                                        <td className="px-6 py-4 text-sm text-gray-300">
                                            {s.revisionNumber != null ? `v${s.revisionNumber}` : "v1"}
                                        </td>
                                        <td className="px-6 py-4">
                                            <span className={`px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full ${STATUS_COLORS[s.status] ?? "bg-gray-500/10 text-gray-400 border border-gray-500/20"}`}>
                                                {STATUS_LABELS[s.status] ?? s.status}
                                            </span>
                                        </td>
                                        <td className="px-6 py-4 text-sm text-gray-400">
                                            {new Date(s.submittedAt).toLocaleDateString("tr-TR")}
                                        </td>
                                        <td className="px-6 py-4 text-right">
                                            <button
                                                onClick={() => router.push(`/submissions/${s.id}`)}
                                                className="text-sm text-blue-400 hover:text-blue-300 font-medium transition-colors"
                                            >
                                                View &amp; Grade
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </main>
        </div>
    );
}
