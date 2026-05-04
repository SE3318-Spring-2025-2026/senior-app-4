"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { getToken, getUser } from "@/lib/auth";
import Sidebar from "@/components/Sidebar";
import IntegrationStatusIndicator from "@/components/IntegrationStatusIndicator";
import { useIntegrationStatus } from "@/hooks/useIntegrationStatus";
import {
    fetchAdvisorAssignments,
    releaseAdviseeGroup,
    type AdvisorAssignment,
} from "@/lib/advisor-requests-api";

export default function MyAdviseesPage() {
    const router = useRouter();
    const [role, setRole] = useState<string | null>(null);

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

    if (role === null) return <Spinner />;
    if (role === "denied") return <AccessDenied />;
    return <MyAdviseesLayout />;
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
                <p className="text-sm text-gray-500">Only Professors can access this page.</p>
            </div>
        </div>
    );
}

function MyAdviseesLayout() {
    return (
        <div className="min-h-screen bg-gray-950 flex">
            <Sidebar activePage="my-advisees" />
            <main className="flex-1 flex flex-col min-w-0">
                <div className="border-b border-white/5 px-8 py-4 flex items-center justify-between">
                    <div>
                        <h1 className="text-base font-semibold text-white">My Advisees</h1>
                        <p className="text-xs text-gray-500 mt-0.5">Groups currently assigned to you as advisor</p>
                    </div>
                    <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-white/5 border border-white/10">
                        <div className="w-1.5 h-1.5 rounded-full bg-green-400 animate-pulse" />
                        <span className="text-xs text-gray-400">D2 Groups</span>
                    </div>
                </div>
                <div className="flex-1 p-8">
                    <AdviseesTable />
                </div>
            </main>
        </div>
    );
}

function AdviseesTable() {
    const [advisees, setAdvisees] = useState<AdvisorAssignment[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [releasingId, setReleasingId] = useState<number | null>(null);
    const [confirmGroupId, setConfirmGroupId] = useState<number | null>(null);

    const loadAdvisees = useCallback(async () => {
        setLoading(true);
        setError(null);

        try {
            const data = await fetchAdvisorAssignments({ hasAdvisor: true });
            setAdvisees(data);
        } catch (err: unknown) {
            const message = err instanceof Error ? err.message : "Failed to load active advisees.";
            setError(message);
            toast.error(message);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        loadAdvisees();
    }, [loadAdvisees]);

    const selectedGroup = useMemo(
        () => advisees.find((a) => a.teamId === confirmGroupId),
        [advisees, confirmGroupId]
    );

    const handleRelease = async (groupId: number) => {
        setReleasingId(groupId);

        try {
            await releaseAdviseeGroup(groupId);
            toast.success("Group released successfully. The group leader has been notified.");
            await loadAdvisees();
        } catch (err: unknown) {
            toast.error(err instanceof Error ? err.message : "Failed to release group.");
        } finally {
            setReleasingId(null);
            setConfirmGroupId(null);
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

    if (error) {
        return (
            <div className="bg-gray-900 border border-red-500/20 rounded-2xl p-8 flex items-start justify-between gap-6">
                <div className="flex items-start gap-4">
                    <div className="w-10 h-10 rounded-xl bg-red-500/10 border border-red-500/20 flex items-center justify-center shrink-0">
                        <svg className="w-5 h-5 text-red-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 9v3.75m0 3.75h.008v.008H12v-.008zm8.25-3.75A8.25 8.25 0 113.75 12a8.25 8.25 0 0116.5 0z" />
                        </svg>
                    </div>
                    <div>
                        <p className="text-sm font-semibold text-white">Active advisees could not be loaded</p>
                        <p className="text-sm text-gray-400 mt-1">{error}</p>
                    </div>
                </div>
                <button
                    onClick={loadAdvisees}
                    className="px-4 py-2 rounded-lg text-sm font-medium bg-white/5 border border-white/10 text-gray-300 hover:text-white hover:bg-white/10 transition-colors"
                >
                    Retry
                </button>
            </div>
        );
    }

    if (advisees.length === 0) {
        return (
            <div className="bg-gray-900 border border-white/8 rounded-2xl p-12 flex flex-col items-center justify-center gap-3">
                <div className="w-14 h-14 rounded-2xl bg-gray-800 border border-white/5 flex items-center justify-center">
                    <svg className="w-6 h-6 text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M18 18.72a9.094 9.094 0 003.741-.479 3 3 0 00-4.682-2.72m.94 3.198l.001.031c0 .225-.012.447-.037.666A11.944 11.944 0 0112 21c-2.17 0-4.207-.576-5.963-1.584A6.062 6.062 0 016 18.719m12 0a5.971 5.971 0 00-.941-3.197m0 0A5.995 5.995 0 0012 12.75a5.995 5.995 0 00-5.058 2.772m0 0a3 3 0 00-4.681 2.72 8.986 8.986 0 003.74.477m.94-3.197a5.971 5.971 0 00-.94 3.197" />
                    </svg>
                </div>
                <p className="text-sm font-medium text-white">No active advisees</p>
                <p className="text-xs text-gray-500">Groups assigned to you in D2 will appear here.</p>
            </div>
        );
    }

    return (
        <>
            <div className="bg-gray-900 border border-white/8 rounded-2xl overflow-hidden">
                <div className="px-6 py-4 border-b border-white/5 flex items-center justify-between gap-4">
                    <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-lg bg-blue-500/10 border border-blue-500/20 flex items-center justify-center">
                            <svg className="w-4 h-4 text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M18 18.72a9.094 9.094 0 003.741-.479 3 3 0 00-4.682-2.72m.94 3.198l.001.031c0 .225-.012.447-.037.666A11.944 11.944 0 0112 21c-2.17 0-4.207-.576-5.963-1.584A6.062 6.062 0 016 18.719m12 0a5.971 5.971 0 00-.941-3.197m0 0A5.995 5.995 0 0012 12.75a5.995 5.995 0 00-5.058 2.772m0 0a3 3 0 00-4.681 2.72 8.986 8.986 0 003.74.477m.94-3.197a5.971 5.971 0 00-.94 3.197" />
                            </svg>
                        </div>
                        <div>
                            <p className="text-sm font-semibold text-white">Active Advisee Groups</p>
                            <p className="text-xs text-gray-500">
                                {advisees.length} group{advisees.length !== 1 ? "s" : ""} currently assigned
                            </p>
                        </div>
                    </div>
                    <button
                        onClick={loadAdvisees}
                        className="px-3 py-1.5 rounded-lg text-xs font-medium bg-white/5 border border-white/10 text-gray-300 hover:text-white hover:bg-white/10 transition-colors"
                    >
                        Refresh
                    </button>
                </div>

                <div className="overflow-x-auto">
                    <table className="w-full min-w-[1100px]">
                        <thead>
                            <tr className="border-b border-white/5">
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Group</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Leader</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Assigned</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Source</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Integrations</th>
                                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-white/5">
                            {advisees.map((assignment) => (
                                <AdviseeRow
                                    key={assignment.teamId}
                                    assignment={assignment}
                                    releasingId={releasingId}
                                    onReleaseClick={() => setConfirmGroupId(assignment.teamId)}
                                />
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>

            {confirmGroupId && selectedGroup && (
                <ConfirmReleaseModal
                    groupName={selectedGroup.teamName}
                    processing={releasingId === confirmGroupId}
                    onConfirm={() => handleRelease(confirmGroupId)}
                    onCancel={() => setConfirmGroupId(null)}
                />
            )}
        </>
    );
}

function AdviseeRow({
    assignment,
    releasingId,
    onReleaseClick,
}: {
    assignment: AdvisorAssignment;
    releasingId: number | null;
    onReleaseClick: () => void;
}) {
    const status = assignment.status?.toUpperCase() ?? "UNKNOWN";
    const canRelease = status === "ADVISED";
    const { status: integrationStatus, loading: integrationLoading } = useIntegrationStatus(assignment.teamId);

    return (
        <tr className="hover:bg-white/[0.02] transition-colors">
            <td className="px-6 py-4">
                <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-lg bg-blue-600/10 border border-blue-500/20 flex items-center justify-center shrink-0">
                        <span className="text-xs font-bold text-blue-400 uppercase">
                            {getInitials(assignment.teamName)}
                        </span>
                    </div>
                    <div className="min-w-0">
                        <p className="text-sm font-medium text-white truncate">{assignment.teamName}</p>
                        <p className="text-xs text-gray-600">Group #{assignment.teamId}</p>
                    </div>
                </div>
            </td>
            <td className="px-6 py-4 text-sm text-gray-300">
                {assignment.leaderName || "Not available"}
            </td>
            <td className="px-6 py-4">
                <StatusPill status={status} />
            </td>
            <td className="px-6 py-4 text-sm text-gray-400">
                {assignment.assignedAt ? formatRelativeDate(assignment.assignedAt) : "Not tracked"}
            </td>
            <td className="px-6 py-4">
                <AssignmentTypePill assignmentType={assignment.assignmentType} />
            </td>
            <td className="px-6 py-4">
                <div className="flex flex-col gap-1.5 min-w-[200px]">
                    <IntegrationStatusIndicator
                        label="GitHub"
                        connected={integrationStatus?.github.connected ?? false}
                        connectedAt={integrationStatus?.github.connectedAt ?? null}
                        loading={integrationLoading}
                        data-testid={`integration-github-group-${assignment.teamId}`}
                    />
                    <IntegrationStatusIndicator
                        label="JIRA"
                        connected={integrationStatus?.jira.connected ?? false}
                        connectedAt={integrationStatus?.jira.connectedAt ?? null}
                        loading={integrationLoading}
                        data-testid={`integration-jira-group-${assignment.teamId}`}
                    />
                </div>
            </td>
            <td className="px-6 py-4 text-right">
                <button
                    onClick={onReleaseClick}
                    disabled={releasingId === assignment.teamId || !canRelease}
                    title={canRelease ? "Release this advisee group" : "Only advised groups can be released"}
                    className="flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg text-xs font-medium ml-auto
                               bg-red-600/10 border border-red-500/30 text-red-400
                               hover:bg-red-600/20 hover:border-red-500/50 active:scale-95
                               transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                >
                    {releasingId === assignment.teamId ? (
                        <svg className="w-3.5 h-3.5 animate-spin" fill="none" viewBox="0 0 24 24">
                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                        </svg>
                    ) : (
                        <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15" />
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M18 12H9m0 0l3-3m-3 3l3 3" />
                        </svg>
                    )}
                    Release
                </button>
            </td>
        </tr>
    );
}

function StatusPill({ status }: { status: string }) {
    const styles =
        status === "ADVISED"
            ? "text-green-400 bg-green-400/10 border-green-400/20"
            : "text-gray-400 bg-white/5 border-white/10";

    return (
        <span className={`text-xs font-medium px-2 py-0.5 rounded-full border ${styles}`}>
            {formatEnumLabel(status)}
        </span>
    );
}

function AssignmentTypePill({ assignmentType }: { assignmentType: AdvisorAssignment["assignmentType"] }) {
    if (!assignmentType) {
        return <span className="text-xs text-gray-600">Pending backend metadata</span>;
    }

    const styles =
        assignmentType === "OVERRIDDEN"
            ? "text-orange-400 bg-orange-400/10 border-orange-400/20"
            : "text-blue-400 bg-blue-400/10 border-blue-400/20";

    return (
        <span className={`text-xs font-medium px-2 py-0.5 rounded-full border ${styles}`}>
            {formatEnumLabel(assignmentType)}
        </span>
    );
}

function ConfirmReleaseModal({
    groupName,
    processing,
    onConfirm,
    onCancel,
}: {
    groupName: string;
    processing: boolean;
    onConfirm: () => void;
    onCancel: () => void;
}) {
    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm">
            <div className="bg-gray-900 border border-white/10 rounded-2xl p-6 w-full max-w-md mx-4 shadow-2xl">
                <div className="flex items-start gap-4">
                    <div className="w-10 h-10 rounded-xl bg-red-500/10 border border-red-500/20 flex items-center justify-center shrink-0">
                        <svg className="w-5 h-5 text-red-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15" />
                        </svg>
                    </div>
                    <div className="flex-1">
                        <h3 className="text-sm font-semibold text-white">Release Group</h3>
                        <p className="text-sm text-gray-400 mt-1">
                            Are you sure you want to release <span className="text-white font-medium">{groupName}</span>?
                            The group will be able to submit a new advisor request.
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
                                   bg-red-600 text-white hover:bg-red-500 active:scale-95
                                   transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        {processing ? (
                            <>
                                <svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                                </svg>
                                Releasing...
                            </>
                        ) : "Confirm Release"}
                    </button>
                </div>
            </div>
        </div>
    );
}

function formatRelativeDate(isoString: string): string {
    const date = new Date(isoString);
    if (Number.isNaN(date.getTime())) return "Invalid date";

    const diff = Date.now() - date.getTime();
    if (diff < 0) return date.toLocaleDateString("tr-TR", { day: "2-digit", month: "short" });

    const minutes = Math.floor(diff / 60000);
    if (minutes < 1) return "Just now";
    if (minutes < 60) return `${minutes}m ago`;

    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours}h ago`;

    const days = Math.floor(hours / 24);
    if (days < 7) return `${days}d ago`;

    return date.toLocaleDateString("tr-TR", { day: "2-digit", month: "short" });
}

function getInitials(value: string): string {
    const initials = value
        .split(/\s+/)
        .filter(Boolean)
        .slice(0, 2)
        .map((part) => part[0])
        .join("");

    return initials || "G";
}

function formatEnumLabel(value: string): string {
    return value
        .toLowerCase()
        .split("_")
        .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
        .join(" ");
}
