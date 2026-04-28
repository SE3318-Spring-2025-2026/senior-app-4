"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { getToken, getUser } from "@/lib/auth";
import Sidebar from "@/components/Sidebar";
import { fetchSubmissions, type SubmissionSummary } from "@/lib/submissions-api";

function Spinner() {
    return (
        <div className="flex items-center justify-center min-h-screen">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600" />
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
    PENDING_REVIEW: "bg-yellow-100 text-yellow-800",
    UNDER_REVIEW: "bg-blue-100 text-blue-800",
    REVISION_REQUESTED: "bg-orange-100 text-orange-800",
    APPROVED: "bg-green-100 text-green-800",
    GRADED: "bg-purple-100 text-purple-800",
    SUBMITTED: "bg-gray-100 text-gray-800",
    SUPERSEDED: "bg-gray-100 text-gray-500",
    REJECTED: "bg-red-100 text-red-800",
};

export default function ProfessorSubmissionsPage() {
    const router = useRouter();
    const [role, setRole] = useState<string | null>(null);
    const [submissions, setSubmissions] = useState<SubmissionSummary[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const token = getToken();
        const user = getUser();

        if (!token || !user) {
            router.replace("/auth/login");
            return;
        }
        if (user.requiresPasswordChange) {
            router.replace("/auth/change-password");
            return;
        }
        if (user.role !== "professor") {
            queueMicrotask(() => setRole("denied"));
            return;
        }
        queueMicrotask(() => setRole(user.role));
    }, [router]);

    const loadSubmissions = useCallback(async () => {
        setLoading(true);
        try {
            const res = await fetchSubmissions({ size: 50 });
            setSubmissions(res.data ?? []);
        } catch (err) {
            toast.error(err instanceof Error ? err.message : "Failed to load submissions.");
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        if (role === "professor") loadSubmissions();
    }, [role, loadSubmissions]);

    if (role === null) return <Spinner />;
    if (role === "denied") return <p className="p-8 text-red-600">Access denied.</p>;

    return (
        <div className="flex min-h-screen bg-gray-50">
            <Sidebar role="professor" />
            <main className="flex-1 p-8">
                <h1 className="text-2xl font-bold text-gray-900 mb-6">Submissions to Grade</h1>

                {loading ? (
                    <Spinner />
                ) : submissions.length === 0 ? (
                    <p className="text-gray-500">No submissions assigned to your committees.</p>
                ) : (
                    <div className="bg-white rounded-lg shadow overflow-hidden">
                        <table className="min-w-full divide-y divide-gray-200">
                            <thead className="bg-gray-50">
                                <tr>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Team</th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Type</th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Version</th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Submitted</th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
                                </tr>
                            </thead>
                            <tbody className="bg-white divide-y divide-gray-200">
                                {submissions.map((s) => (
                                    <tr key={s.id} className="hover:bg-gray-50">
                                        <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                                            {s.teamName ?? `Team #${s.teamId}`}
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                                            {s.deliverableType.replace(/_/g, " ")}
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                                            {s.revisionNumber != null ? `v${s.revisionNumber}` : "v1"}
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap">
                                            <span className={`px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full ${STATUS_COLORS[s.status] ?? "bg-gray-100 text-gray-800"}`}>
                                                {STATUS_LABELS[s.status] ?? s.status}
                                            </span>
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                            {new Date(s.submittedAt).toLocaleDateString()}
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm">
                                            <button
                                                onClick={() => router.push(`/submissions/${s.id}`)}
                                                className="text-blue-600 hover:text-blue-800 font-medium"
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
