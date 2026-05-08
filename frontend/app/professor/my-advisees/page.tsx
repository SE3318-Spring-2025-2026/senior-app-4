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
import {
    fetchAdvisorSprintSummary,
    fetchGroupTrackingDetails,
    type SprintSummaryResponse,
    type GroupTrackingDetail,
} from "@/lib/sprint-tracking-api";
import {
    fetchAiCodeReviewSuggestion,
    fetchSprintAdvisorGrades,
    saveSprintAdvisorGrade,
    type SoftGrade,
} from "@/lib/final-grading-api";

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
                        <span className="text-xs text-gray-400">System Online</span>
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
    const [sprintSummary, setSprintSummary] = useState<SprintSummaryResponse | null>(null);
    const [expandedGroupId, setExpandedGroupId] = useState<number | null>(null);
    const [trackingDetails, setTrackingDetails] = useState<Record<number, GroupTrackingDetail | null>>({});
    const [loadingDetails, setLoadingDetails] = useState<Record<number, boolean>>({});

    const loadAdvisees = useCallback(async () => {
        setLoading(true);
        setError(null);

        try {
            const token = getToken();
            if (!token) {
                throw new Error("No authentication token");
            }

            const data = await fetchAdvisorAssignments({ hasAdvisor: true });
            setAdvisees(data);

            // Load sprint summary
            try {
                const summary = await fetchAdvisorSprintSummary(token);
                setSprintSummary(summary);
            } catch (err: unknown) {
                console.warn("Failed to load sprint summary:", err);
                // Don't block page load if sprint data fails
            }
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

    const handleExpandRow = async (groupId: number) => {
        if (expandedGroupId === groupId) {
            setExpandedGroupId(null);
            return;
        }

        if (trackingDetails[groupId]) {
            setExpandedGroupId(groupId);
            return;
        }

        setLoadingDetails((prev) => ({ ...prev, [groupId]: true }));

        try {
            const token = getToken();
            if (!token) {
                throw new Error("No authentication token");
            }

            const details = await fetchGroupTrackingDetails(groupId, token);
            setTrackingDetails((prev) => ({ ...prev, [groupId]: details }));
            setExpandedGroupId(groupId);
        } catch (err: unknown) {
            console.error("Failed to load tracking details:", err);
            toast.error("Failed to load tracking details");
        } finally {
            setLoadingDetails((prev) => ({ ...prev, [groupId]: false }));
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
            {sprintSummary?.activeSprint && (
                <div className="bg-gradient-to-r from-blue-600/20 to-cyan-600/20 border border-blue-500/20 rounded-2xl p-6 mb-6">
                    <div className="flex items-center justify-between gap-4">
                        <div className="flex items-center gap-4">
                            <div className="w-12 h-12 rounded-xl bg-blue-500/20 border border-blue-500/30 flex items-center justify-center">
                                <svg className="w-6 h-6 text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                                </svg>
                            </div>
                            <div>
                                <p className="text-sm font-semibold text-white">{sprintSummary.activeSprint.sprintName}</p>
                                <p className="text-xs text-gray-400 mt-1">
                                    {sprintSummary.activeSprint.daysRemaining} days remaining • {new Date(sprintSummary.activeSprint.endDate).toLocaleDateString("en-US", { month: "short", day: "numeric" })}
                                </p>
                            </div>
                        </div>
                        <div className="text-right">
                            <p className="text-xs text-gray-400">Groups synced</p>
                            <p className="text-lg font-semibold text-white">{sprintSummary.groups.length}</p>
                        </div>
                    </div>
                </div>
            )}

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
                    <table className="w-full min-w-[1200px]">
                        <thead>
                            <tr className="border-b border-white/5">
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-12"></th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Group</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Leader</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Sprint Progress</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Integrations</th>
                                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-white/5">
                            {advisees.map((assignment) => (
                                <SprintAwareAdviseeRow
                                    key={assignment.teamId}
                                    assignment={assignment}
                                    releasingId={releasingId}
                                    onReleaseClick={() => setConfirmGroupId(assignment.teamId)}
                                    sprintData={sprintSummary?.groups.find((g) => g.groupId === assignment.teamId) || null}
                                    isExpanded={expandedGroupId === assignment.teamId}
                                    onExpandClick={() => handleExpandRow(assignment.teamId)}
                                    trackingDetails={trackingDetails[assignment.teamId] || null}
                                    loadingDetails={loadingDetails[assignment.teamId] || false}
                                    activeSprint={sprintSummary?.activeSprint ?? null}
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

function SprintAwareAdviseeRow({
    assignment,
    releasingId,
    onReleaseClick,
    sprintData,
    isExpanded,
    onExpandClick,
    trackingDetails,
    loadingDetails,
    activeSprint,
}: {
    assignment: AdvisorAssignment;
    releasingId: number | null;
    onReleaseClick: () => void;
    sprintData: SprintSummaryResponse["groups"][0] | null;
    isExpanded: boolean;
    onExpandClick: () => void;
    trackingDetails: GroupTrackingDetail | null;
    loadingDetails: boolean;
    activeSprint: SprintSummaryResponse["activeSprint"] | null;
}) {
    const router = useRouter();
    const status = assignment.status?.toUpperCase() ?? "UNKNOWN";
    const canRelease = status === "ADVISED";
    const { status: integrationStatus, loading: integrationLoading } = useIntegrationStatus(assignment.teamId);

    const totalPoints = sprintData?.perStudentSummary.reduce((sum, s) => sum + s.totalAssignedStoryPoints, 0) ?? 0;
    const completedPoints = sprintData?.perStudentSummary.reduce((sum, s) => sum + s.completedStoryPoints, 0) ?? 0;
    const progressPercent = totalPoints > 0 ? (completedPoints / totalPoints) * 100 : 0;

    return (
        <>
            <tr className="hover:bg-white/[0.02] transition-colors">
                <td className="px-6 py-4">
                    <button
                        onClick={onExpandClick}
                        className="w-6 h-6 flex items-center justify-center text-gray-500 hover:text-white transition-colors disabled:opacity-50"
                        disabled={!sprintData || loadingDetails}
                        title={sprintData ? (isExpanded ? "Collapse details" : "Expand details") : "No sprint data"}
                    >
                        {loadingDetails ? (
                            <svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                            </svg>
                        ) : (
                            <svg
                                className={`w-4 h-4 transition-transform ${isExpanded ? "rotate-90" : ""}`}
                                fill="none"
                                viewBox="0 0 24 24"
                                stroke="currentColor"
                            >
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                            </svg>
                        )}
                    </button>
                </td>
                <td className="px-6 py-4">
                    <button
                        onClick={() => router.push(`/professor/submissions?groupId=${assignment.teamId}`)}
                        className="flex items-center gap-3 text-left hover:opacity-80 transition-opacity"
                    >
                        <div className="w-8 h-8 rounded-lg bg-blue-600/10 border border-blue-500/20 flex items-center justify-center shrink-0">
                            <span className="text-xs font-bold text-blue-400 uppercase">
                                {getInitials(assignment.teamName)}
                            </span>
                        </div>
                        <div className="min-w-0">
                            <p className="text-sm font-medium text-white truncate hover:underline">{assignment.teamName}</p>
                            <p className="text-xs text-gray-600">Group #{assignment.teamId}</p>
                        </div>
                    </button>
                </td>
                <td className="px-6 py-4 text-sm text-gray-300">
                    {assignment.leaderName || "Not available"}
                </td>
                <td className="px-6 py-4">
                    <StatusPill status={status} />
                </td>
                <td className="px-6 py-4">
                    {sprintData ? (
                        <div className="flex items-center gap-3 min-w-[200px]">
                            <div className="flex-1">
                                <div className="flex items-center justify-between mb-1">
                                    <p className="text-xs text-gray-400">{completedPoints}/{totalPoints} points</p>
                                    <p className="text-xs font-medium text-white">{Math.round(progressPercent)}%</p>
                                </div>
                                <div className="h-2 bg-gray-800 rounded-full overflow-hidden">
                                    <div
                                        className="h-full bg-gradient-to-r from-green-500 to-emerald-500 transition-all"
                                        style={{ width: `${progressPercent}%` }}
                                    />
                                </div>
                                <p className="text-xs text-gray-500 mt-1">{sprintData.mergedPRCount} PRs merged</p>
                            </div>
                        </div>
                    ) : (
                        <p className="text-xs text-gray-500">No sprint data</p>
                    )}
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

            {isExpanded && trackingDetails && (
                <tr className="bg-gray-950/50 border-t border-white/5">
                    <td colSpan={7} className="px-6 py-6">
                        <div className="space-y-6">
                            {activeSprint?.sprintId && (
                                <AdvisorSprintGradeForm
                                    groupId={assignment.teamId}
                                    sprintId={activeSprint.sprintId}
                                />
                            )}

                            {/* Per-Student Summary */}
                            {trackingDetails && (
                                <div className="bg-gray-900 rounded-lg border border-white/5 p-4">
                                    <h4 className="text-sm font-semibold text-white mb-4">Team Member Progress</h4>
                                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                                        {trackingDetails.issues && trackingDetails.issues.length > 0 ? (
                                            Array.from(
                                                new Map(
                                                    trackingDetails.issues
                                                        .filter((i) => i.assigneeGithubUsername)
                                                        .map((i) => [i.assigneeGithubUsername, i])
                                                ).entries()
                                            ).map(([username, issue]) => {
                                                const memberIssues = trackingDetails.issues.filter((i) => i.assigneeGithubUsername === username);
                                                const memberPoints = memberIssues.reduce((sum, i) => sum + (i.storyPoints ?? 0), 0);
                                                const memberMergedPoints = memberIssues
                                                    .filter((i) => i.prMerged)
                                                    .reduce((sum, i) => sum + (i.storyPoints ?? 0), 0);

                                                return (
                                                    <div key={username} className="bg-gray-800/50 rounded border border-white/5 p-3">
                                                        <p className="text-xs font-medium text-gray-300 mb-2">{username}</p>
                                                        <div className="space-y-1.5 text-xs">
                                                            <div className="flex justify-between">
                                                                <span className="text-gray-500">Completed:</span>
                                                                <span className="text-green-400 font-medium">{memberMergedPoints} pts</span>
                                                            </div>
                                                            <div className="flex justify-between">
                                                                <span className="text-gray-500">Total:</span>
                                                                <span className="text-white font-medium">{memberPoints} pts</span>
                                                            </div>
                                                            <div className="h-1.5 bg-gray-700 rounded-full overflow-hidden mt-2">
                                                                <div
                                                                    className="h-full bg-green-500"
                                                                    style={{ width: `${memberPoints > 0 ? (memberMergedPoints / memberPoints) * 100 : 0}%` }}
                                                                />
                                                            </div>
                                                        </div>
                                                    </div>
                                                );
                                            })
                                        ) : (
                                            <p className="text-xs text-gray-500">No member data available</p>
                                        )}
                                    </div>
                                </div>
                            )}

                            {/* Issue Tracking Table */}
                            {trackingDetails?.issues && trackingDetails.issues.length > 0 && (
                                <div className="bg-gray-900 rounded-lg border border-white/5 overflow-hidden">
                                    <div className="px-4 py-3 border-b border-white/5 bg-gray-800/50">
                                        <h4 className="text-sm font-semibold text-white">Issue Details</h4>
                                    </div>
                                    <div className="overflow-x-auto">
                                        <table className="w-full min-w-[600px]">
                                            <thead>
                                                <tr className="border-b border-white/5 bg-gray-800/30">
                                                    <th className="px-4 py-2 text-left text-xs font-medium text-gray-400 uppercase">Issue</th>
                                                    <th className="px-4 py-2 text-left text-xs font-medium text-gray-400 uppercase">Points</th>
                                                    <th className="px-4 py-2 text-left text-xs font-medium text-gray-400 uppercase">Assignee</th>
                                                    <th className="px-4 py-2 text-left text-xs font-medium text-gray-400 uppercase">PR #</th>
                                                    <th className="px-4 py-2 text-left text-xs font-medium text-gray-400 uppercase">Status</th>
                                                </tr>
                                            </thead>
                                            <tbody className="divide-y divide-white/5">
                                                {trackingDetails.issues.map((issue) => (
                                                    <tr key={issue.issueKey} className="hover:bg-white/[0.02]">
                                                        <td className="px-4 py-2 text-xs font-medium text-blue-400">{issue.issueKey}</td>
                                                        <td className="px-4 py-2 text-xs text-gray-300">{issue.storyPoints ?? "-"}</td>
                                                        <td className="px-4 py-2 text-xs text-gray-400">{issue.assigneeGithubUsername ?? "-"}</td>
                                                        <td className="px-4 py-2 text-xs text-gray-400">{issue.prNumber ?? "-"}</td>
                                                        <td className="px-4 py-2 text-xs">
                                                            {issue.prMerged ? (
                                                                <span className="inline-flex items-center gap-1 px-2 py-1 rounded-full bg-green-500/10 text-green-400 border border-green-500/20">
                                                                    ✓ Merged
                                                                </span>
                                                            ) : issue.prNumber ? (
                                                                <span className="inline-flex items-center gap-1 px-2 py-1 rounded-full bg-yellow-500/10 text-yellow-400 border border-yellow-500/20">
                                                                    ○ Open
                                                                </span>
                                                            ) : (
                                                                <span className="text-gray-500">—</span>
                                                            )}
                                                        </td>
                                                    </tr>
                                                ))}
                                            </tbody>
                                        </table>
                                    </div>
                                </div>
                            )}
                        </div>
                    </td>
                </tr>
            )}
        </>
    );
}

function AdvisorSprintGradeForm({ groupId, sprintId }: { groupId: number; sprintId: number }) {
    const [scrumGrade, setScrumGrade] = useState<SoftGrade>("A");
    const [codeReviewGrade, setCodeReviewGrade] = useState<SoftGrade>("A");
    const [saving, setSaving] = useState(false);
    const [suggesting, setSuggesting] = useState(false);

    useEffect(() => {
        let cancelled = false;
        fetchSprintAdvisorGrades(groupId)
            .then((grades) => {
                if (cancelled) return;
                const current = grades.find((grade) => grade.sprintId === sprintId);
                if (current) {
                    setScrumGrade(current.scrumGrade);
                    setCodeReviewGrade(current.codeReviewGrade);
                }
            })
            .catch(() => undefined);
        return () => {
            cancelled = true;
        };
    }, [groupId, sprintId]);

    const handleSuggestion = async () => {
        setSuggesting(true);
        try {
            const suggestion = await fetchAiCodeReviewSuggestion(groupId, sprintId);
            setCodeReviewGrade(suggestion.data.suggestedGrade);
            toast.success(`AI suggests ${suggestion.data.suggestedGrade} for code review.`);
        } catch (err) {
            toast.error(err instanceof Error ? err.message : "AI suggestion is unavailable.");
        } finally {
            setSuggesting(false);
        }
    };

    const handleSave = async () => {
        setSaving(true);
        try {
            await saveSprintAdvisorGrade({ groupId, sprintId, scrumGrade, codeReviewGrade });
            toast.success("Sprint grade saved.");
        } catch (err) {
            toast.error(err instanceof Error ? err.message : "Sprint grade could not be saved.");
        } finally {
            setSaving(false);
        }
    };

    return (
        <div className="rounded-lg border border-white/10 bg-gray-900 p-4">
            <div className="mb-4 flex items-center justify-between gap-3">
                <h4 className="text-sm font-semibold text-white">Sprint Grade</h4>
                <button
                    type="button"
                    onClick={handleSuggestion}
                    disabled={suggesting}
                    className="rounded-lg border border-white/10 bg-white/5 px-3 py-1.5 text-xs text-gray-300 transition hover:bg-white/10 disabled:opacity-50"
                >
                    {suggesting ? "Reading..." : "AI review"}
                </button>
            </div>
            <div className="grid gap-4 md:grid-cols-2">
                <GradeSelect label="Scrum" value={scrumGrade} onChange={setScrumGrade} />
                <GradeSelect label="Code Review" value={codeReviewGrade} onChange={setCodeReviewGrade} />
            </div>
            <button
                type="button"
                onClick={handleSave}
                disabled={saving}
                className="mt-4 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-blue-500 disabled:opacity-50"
            >
                {saving ? "Saving..." : "Save Sprint Grade"}
            </button>
        </div>
    );
}

function GradeSelect({ label, value, onChange }: { label: string; value: SoftGrade; onChange: (value: SoftGrade) => void }) {
    const grades: SoftGrade[] = ["A", "B", "C", "D", "F"];
    return (
        <div>
            <p className="mb-2 text-xs font-medium uppercase tracking-wider text-gray-500">{label}</p>
            <div className="flex flex-wrap gap-2">
                {grades.map((grade) => (
                    <button
                        key={grade}
                        type="button"
                        onClick={() => onChange(grade)}
                        className={`flex h-8 min-w-8 items-center justify-center rounded-lg border px-2 text-xs font-semibold transition ${
                            value === grade
                                ? "border-blue-400/40 bg-blue-500 text-white"
                                : "border-white/10 bg-white/5 text-gray-400 hover:text-white"
                        }`}
                    >
                        {grade}
                    </button>
                ))}
            </div>
        </div>
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
