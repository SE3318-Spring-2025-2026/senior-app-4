"use client";

import Sidebar from "@/components/Sidebar";
import { showToast } from "@/components/toast/ToastContext";
import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import AdvisorRequestPanel from "@/components/AdvisorRequestPanel";
import StatusBadge from "@/components/StatusBadge";
import GithubStatusCard from "@/components/GithubStatusCard";
import JiraStatusCard from "@/components/JiraStatusCard";
import {
    ApiGroupDetail,
    ApiGroupMember,
    fetchGroupDetail,
    fetchGroupMembers,
    updateGroupNameApi,
    removeMemberApi,
    leaveGroupApi,
    addMemberApi,
    coordinatorAddMemberApi,
    deleteGroupApi,
    updateGroupStatusApi
} from "@/lib/groups-api";
//import { inviteMemberApi } from "@/lib/notifications-api";
import { getUser, getToken, decodeToken } from "@/lib/auth";
import {
    fetchGithubIntegration,
    fetchJiraIntegration,
    type GithubIntegrationApiResponse,
    type JiraIntegrationApiResponse,
} from "@/lib/integrations-api";
import { fetchFinalGrades, type FinalGradeResponse } from "@/lib/final-grading-api";
import { fetchSubmissions, fetchSubmissionReviews, type SubmissionSummary } from "@/lib/submissions-api";

function formatDate(dateString: string) {
    return new Date(dateString).toLocaleString("en-US", {
        day: "numeric",
        month: "long",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
    });
}

function FinalGradeCard({ data }: { data: FinalGradeResponse["data"] }) {
    return (
        <div className="rounded-2xl border border-emerald-500/20 bg-emerald-500/10 p-6 shadow-lg shadow-black/20 backdrop-blur">
            <div className="flex items-start justify-between gap-4">
                <div>
                    <p className="mb-2 text-sm text-emerald-200">Final Grade</p>
                    <p className="text-3xl font-semibold text-white">{Number(data.teamGrade).toFixed(2)}</p>
                </div>
                <span className="rounded-full border border-emerald-400/30 bg-emerald-400/10 px-3 py-1 text-xs font-medium text-emerald-200">
                    {data.published ? "Published" : "Draft"}
                </span>
            </div>
            {data.students.length > 0 && (
                <div className="mt-4 space-y-2">
                    {data.students.map((student) => (
                        <div key={student.userId} className="flex items-center justify-between gap-3 rounded-lg bg-black/15 px-3 py-2">
                            <p className="truncate text-sm text-emerald-50">{student.fullName}</p>
                            <p className="text-sm font-semibold text-white">{Number(student.finalGrade).toFixed(2)}</p>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}

export default function GroupDetailPage() {
    const params = useParams();
    const groupId = Number(params.groupId);

    const [group, setGroup] = useState<ApiGroupDetail | null>(null);
    const [githubIntegration, setGithubIntegration] =
        useState<GithubIntegrationApiResponse | null>(null);
    const [jiraIntegration, setJiraIntegration] =
        useState<JiraIntegrationApiResponse | null>(null);
    const [finalGrades, setFinalGrades] =
        useState<FinalGradeResponse["data"] | null>(null);
    const [revisionRequested, setRevisionRequested] =
        useState<{ submission: SubmissionSummary; feedback: string } | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    const [newGroupName, setNewGroupName] = useState("");
    const [updatingName, setUpdatingName] = useState(false);
    const [updateSuccess, setUpdateSuccess] = useState("");
    const [updateError, setUpdateError] = useState("");

    const [inviteStudentId, setInviteStudentId] = useState("");
    const [inviting, setInviting] = useState(false);
    const [inviteSuccess, setInviteSuccess] = useState("");
    const [inviteError, setInviteError] = useState("");

    const [removingMemberId, setRemovingMemberId] = useState<string | null>(null);
    const [removeError, setRemoveError] = useState("");
    const [removeSuccess, setRemoveSuccess] = useState("");

    const [leaving, setLeaving] = useState(false);
    const [leaveError, setLeaveError] = useState("");

    const [disbanding, setDisbanding] = useState(false);
    const [disbandError, setDisbandError] = useState("");

    const [showLeaderModal, setShowLeaderModal] = useState(false);
    const [memberToRemoveAsLeader, setMemberToRemoveAsLeader] = useState<string | null>(null);
    const [newLeaderIdSelection, setNewLeaderIdSelection] = useState<string>("");

    const [coordInviteId, setCoordInviteId] = useState("");
    const [coordInviting, setCoordInviting] = useState(false);

    const [updatingStatus, setUpdatingStatus] = useState(false);

    const currentUser = getUser();
    const token = getToken();
    const decoded = token ? decodeToken(token) : null;

    const currentUserId = Number(
        decoded?.userId ??
        decoded?.jwt_userId ??
        decoded?.user_id ??
        decoded?.id ??
        decoded?.sub
    );

    const isLeader =
        Number.isFinite(currentUserId) &&
        Number(group?.leaderId) === currentUserId;

    const isStudent = currentUser?.role?.toLowerCase() === "student";
    const isCoordinator = currentUser?.role?.toLowerCase() === "coordinator";

    const isGroupMember =
        isStudent &&
        group?.members?.some(
            (member) =>
                Number(member.userId) === currentUserId ||
                member.studentId === currentUser?.studentId
        );

    useEffect(() => {
        let cancelled = false;

        async function loadGroup() {
            try {
                setLoading(true);
                setError("");

                const [groupData, membersData, githubData, jiraData] = await Promise.all([
                    fetchGroupDetail(groupId),
                    fetchGroupMembers(groupId),
                    fetchGithubIntegration(groupId),
                    fetchJiraIntegration(groupId),
                ]);
                const finalGradeData = await fetchFinalGrades(groupId).catch(() => null);

                // Find any submission with REVISION_REQUESTED status for this group
                const submissionsData = await fetchSubmissions({ teamId: String(groupId), size: 50 }).catch(() => null);
                const revisionSub = submissionsData?.data?.find((s) => s.status === "REVISION_REQUESTED") ?? null;
                let revisionFeedback = "";
                if (revisionSub) {
                    const reviews = await fetchSubmissionReviews(String(revisionSub.id)).catch(() => null);
                    const latestRevisionReview = reviews?.data
                        ?.filter((r) => r.status === "REVISION_REQUESTED")
                        .at(-1);
                    revisionFeedback = latestRevisionReview?.comments ?? "";
                }

                if (cancelled) return;

                setGroup({
                    ...groupData,
                    members: membersData,
                });
                setNewGroupName(groupData.groupName);
                setGithubIntegration(githubData);
                setJiraIntegration(jiraData);
                setFinalGrades(finalGradeData?.data ?? null);
                setRevisionRequested(revisionSub ? { submission: revisionSub, feedback: revisionFeedback } : null);
            } catch (err) {
                if (cancelled) return;
                const message =
                    err instanceof Error ? err.message : "Failed to load group details.";
                setError(message);
            } finally {
                if (!cancelled) setLoading(false);
            }
        }

        if (!Number.isNaN(groupId)) loadGroup();

        return () => {
            cancelled = true;
        };
    }, [groupId]);

    async function handleUpdateGroupName(e: React.FormEvent) {
        e.preventDefault();
        setUpdateError("");
        setUpdateSuccess("");

        const trimmedName = newGroupName.trim();

        if (trimmedName.length < 3 || trimmedName.length > 100) {
            const message = "Group name must be between 3 and 100 characters.";
            setUpdateError(message);
            showToast(message, "warning");
            return;
        }

        setUpdatingName(true);
        try {
            await updateGroupNameApi(groupId, trimmedName);
            setGroup((prev) => (prev ? { ...prev, groupName: trimmedName } : prev));
            setUpdateSuccess("Group name updated successfully!");
            showToast("Group name updated successfully!", "success");
        } catch (err) {
            const message =
                err instanceof Error ? err.message : "Failed to update group name.";
            setUpdateError(message);
            showToast(message, "error");
        } finally {
            setUpdatingName(false);
        }
    }

    async function handleInvite(e: React.FormEvent) {
        e.preventDefault();
        setInviteError("");
        setInviteSuccess("");

        const studentId = parseInt(inviteStudentId.trim(), 10);
        if (Number.isNaN(studentId) || studentId <= 0) {
            const message = "Please enter a valid numeric student ID.";
            setInviteError(message);
            showToast(message, "warning");
            return;
        }

        setInviting(true);
        try {
            await addMemberApi(groupId, studentId);
            setInviteSuccess("Invitation sent successfully!");
            showToast("Invitation sent successfully!", "success");
            setInviteStudentId("");
        } catch (err) {
            const message =
                err instanceof Error ? err.message : "Failed to send invitation.";
            setInviteError(message);
            showToast(message, "error");
        } finally {
            setInviting(false);
        }
    }

    async function handleCoordInvite(e: React.FormEvent) {
        e.preventDefault();
        const studentId = parseInt(coordInviteId.trim(), 10);
        if (Number.isNaN(studentId) || studentId <= 0) {
            showToast("Please enter a valid numeric student ID.", "warning");
            return;
        }

        setCoordInviting(true);
        try {
            await coordinatorAddMemberApi(groupId, studentId);
            showToast("Student added successfully!", "success");
            setCoordInviteId("");
            const membersData = await fetchGroupMembers(groupId);
            setGroup(prev => prev ? { ...prev, members: membersData } : prev);
        } catch (err) {
            showToast(err instanceof Error ? err.message : "Failed to add student.", "error");
        } finally {
            setCoordInviting(false);
        }
    }

    async function handleRemoveMember(studentId: string, isLeaderMember: boolean) {
        setRemoveError("");
        setRemoveSuccess("");

        if (isLeaderMember && isCoordinator && group?.members && group.members.length > 1) {
            setMemberToRemoveAsLeader(studentId);
            setShowLeaderModal(true);
            return;
        }

        const confirmed = window.confirm(
            isLeaderMember ? "This group will be completely deleted because you are removing the only member (the leader). Are you sure?" : "Are you sure you want to remove this member from the group?"
        );
        if (!confirmed) return;

        setRemovingMemberId(studentId);
        try {
            await removeMemberApi(groupId, studentId);

            if (isLeaderMember && group?.members?.length === 1) {
                showToast("Leader removed and group deleted.", "success");
                window.location.href = "/groups";
                return;
            }

            setGroup((prev) => {
                if (!prev) return prev;
                return {
                    ...prev,
                    members: prev.members.filter(
                        (member: ApiGroupMember) => member.studentId !== studentId
                    ),
                };
            });

            setRemoveSuccess("Member removed successfully!");
            showToast("Member removed successfully!", "success");
        } catch (err) {
            const message =
                err instanceof Error ? err.message : "Failed to remove member.";
            setRemoveError(message);
            showToast(message, "error");
        } finally {
            setRemovingMemberId(null);
        }
    }

    async function submitLeaderRemoval() {
        if (!memberToRemoveAsLeader || !newLeaderIdSelection) {
            showToast("Please select a new leader.", "warning");
            return;
        }
        
        const newLeaderIdNum = Number(newLeaderIdSelection);

        setRemovingMemberId(memberToRemoveAsLeader);
        setShowLeaderModal(false);
        
        try {
            await removeMemberApi(groupId, memberToRemoveAsLeader, newLeaderIdNum);
            showToast("Leader removed successfully. New leader assigned.", "success");
            const membersData = await fetchGroupMembers(groupId);
            setGroup(prev => prev ? { ...prev, members: membersData, leaderId: newLeaderIdNum } : prev);
        } catch (err) {
            showToast(err instanceof Error ? err.message : "Failed to remove leader.", "error");
        } finally {
            setRemovingMemberId(null);
            setMemberToRemoveAsLeader(null);
            setNewLeaderIdSelection("");
        }
    }

    async function handleLeaveGroup() {
        setLeaveError("");

        const confirmed = window.confirm("Are you sure you want to leave this group?");
        if (!confirmed) return;

        setLeaving(true);
        try {
            await leaveGroupApi(groupId);
            showToast("You left the group successfully!", "success");
            window.location.href = "/groups";
        } catch (err) {
            const message =
                err instanceof Error ? err.message : "Failed to leave group.";
            setLeaveError(message);
            showToast(message, "error");
        } finally {
            setLeaving(false);
        }
    }

    async function handleDisbandGroup() {
        setDisbandError("");

        const confirmed = window.confirm("Are you sure you want to completely disband and delete this group? This action cannot be undone.");
        if (!confirmed) return;

        setDisbanding(true);
        try {
            await deleteGroupApi(groupId);
            showToast("Group disbanded successfully!", "success");
            window.location.href = "/groups";
        } catch (err) {
            const message = err instanceof Error ? err.message : "Failed to disband group.";
            setDisbandError(message);
            showToast(message, "error");
        } finally {
            setDisbanding(false);
        }
    }

    if (loading) {
        return (
            <main className="min-h-screen bg-gray-950 flex items-center justify-center text-white">
                Loading...
            </main>
        );
    }

    if (error || !group) {
        return (
            <main className="min-h-screen bg-gray-950 flex items-center justify-center text-white">
                {error || "Group not found"}
            </main>
        );
    }

    return (
        <div className="min-h-screen bg-gray-950 flex">
            <Sidebar activePage="groups" />
            <main className="flex-1 min-w-0 px-6 py-10 text-white">
                <div className="mx-auto max-w-6xl">
                    <Link href="/groups" className="text-sm text-blue-400 hover:underline">
                        ← Back to groups
                    </Link>

                    <div className="mt-6 mb-6 flex items-start justify-between gap-4">
                        <div>
                            <h1 className="text-4xl font-bold tracking-tight">{group.groupName}</h1>
                            <p className="mt-2 text-lg text-gray-400">
                                Advisor: {group.advisorId ? `Advisor #${group.advisorId}` : "Not Assigned"}
                            </p>
                        </div>
                        {isCoordinator ? (
                            <div className="flex items-center gap-3">
                                <span className="text-sm text-gray-400">Status:</span>
                                <select
                                    value={group.status}
                                    disabled={updatingStatus}
                                    onChange={async (e) => {
                                        const newStatus = e.target.value;
                                        setUpdatingStatus(true);
                                        try {
                                            await updateGroupStatusApi(groupId, newStatus);
                                            setGroup(prev => prev ? { ...prev, status: newStatus } : prev);
                                            showToast("Group status updated successfully!", "success");
                                        } catch (err) {
                                            showToast(err instanceof Error ? err.message : "Failed to update status", "error");
                                        } finally {
                                            setUpdatingStatus(false);
                                        }
                                    }}
                                    className="rounded-lg border border-white/10 bg-gray-900 px-3 py-1.5 text-sm font-medium text-white focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 disabled:opacity-50"
                                >
                                    <option value="FORMING">Forming</option>
                                    <option value="FORMED">Formed</option>
                                    <option value="ADVISED">Advised</option>
                                    <option value="DISBANDED">Disbanded</option>
                                </select>
                            </div>
                        ) : (
                            <StatusBadge
                                status={group.status.toLowerCase() as "forming" | "formed" | "advised" | "disbanded"}
                            />
                        )}
                    </div>

                    {isLeader && (
                        <div className="mb-6 rounded-2xl border border-yellow-500/20 bg-yellow-500/5 p-7 shadow-lg shadow-black/20 backdrop-blur">
                            <h2 className="text-xl font-semibold mb-1">Update Group Name</h2>
                            <p className="text-sm text-gray-400 mb-5">
                                Only the group leader can update the group name.
                            </p>

                            <form
                                onSubmit={handleUpdateGroupName}
                                className="flex flex-col gap-4 sm:flex-row sm:items-end"
                            >
                                <div className="flex-1">
                                    <label
                                        htmlFor="group-name"
                                        className="block text-sm text-gray-300 mb-2 font-medium"
                                    >
                                        Group Name
                                    </label>
                                    <input
                                        id="group-name"
                                        type="text"
                                        value={newGroupName}
                                        onChange={(e) => setNewGroupName(e.target.value)}
                                        className="w-full rounded-xl border border-white/10 bg-gray-900 px-4 py-3 text-white placeholder-gray-500 focus:border-yellow-500 focus:outline-none focus:ring-1 focus:ring-yellow-500 transition"
                                        placeholder="Enter a new group name"
                                        maxLength={100}
                                    />
                                </div>

                                <button
                                    type="submit"
                                    disabled={updatingName}
                                    className="rounded-xl bg-yellow-600 px-6 py-3 text-sm font-semibold hover:bg-yellow-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                                >
                                    {updatingName ? "Updating..." : "Update Name"}
                                </button>
                            </form>

                            {updateSuccess && (
                                <p className="mt-3 text-sm text-green-400 font-medium">
                                    ✓ {updateSuccess}
                                </p>
                            )}
                            {updateError && (
                                <p className="mt-3 text-sm text-red-400 font-medium">
                                    ✗ {updateError}
                                </p>
                            )}
                        </div>
                    )}

                    {/* Revision Requested Banner */}
                    {revisionRequested && (
                        <div className="mb-6 rounded-2xl border border-orange-500/30 bg-orange-500/10 p-6 shadow-lg shadow-black/20">
                            <div className="flex items-start gap-4">
                                <div className="mt-0.5 flex-shrink-0 rounded-xl bg-orange-500/20 p-2">
                                    <svg className="h-5 w-5 text-orange-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L10.582 16.07a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931z" />
                                    </svg>
                                </div>
                                <div className="flex-1 min-w-0">
                                    <p className="text-sm font-semibold text-orange-200">
                                        Revision Requested — {revisionRequested.submission.deliverableType.replace("_", " ")}
                                    </p>
                                    {revisionRequested.feedback ? (
                                        <div className="mt-2 rounded-xl border border-orange-500/20 bg-black/20 px-4 py-3">
                                            <p className="text-xs text-gray-400 mb-1">Advisor Feedback:</p>
                                            <p className="text-sm text-orange-100/90">{revisionRequested.feedback}</p>
                                        </div>
                                    ) : (
                                        <p className="mt-1 text-sm text-orange-100/70">Your advisor has requested revisions. Please review and resubmit.</p>
                                    )}
                                    {isLeader && (
                                        <Link
                                            href={`/groups/${group.id}/submissions/new?parentSubmissionId=${revisionRequested.submission.id}`}
                                            className="mt-4 inline-flex items-center gap-2 rounded-xl bg-orange-500 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-orange-400"
                                        >
                                            <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5m-13.5-9L12 3m0 0l4.5 4.5M12 3v13.5" />
                                            </svg>
                                            Submit Revision
                                        </Link>
                                    )}
                                </div>
                            </div>
                        </div>
                    )}

                    <div className="grid gap-6 md:grid-cols-2 mb-8">
                        <Link href={`/groups/${group.id}/committee-grading`}>
                            <div className="cursor-pointer rounded-2xl border border-blue-500/20 bg-blue-500/10 p-6 shadow-lg shadow-blue-950/20 backdrop-blur transition-all hover:border-blue-400/40 hover:bg-blue-500/15">
                                <p className="mb-2 text-sm text-blue-200">Committee Grading</p>
                                <p className="font-medium text-white">Open grading drawer</p>
                                <p className="mt-2 text-sm text-blue-100/70">
                                    Review submission details, add comments, and submit the final score.
                                </p>
                            </div>
                        </Link>

                        <Link href={`/groups/${group.id}/submissions/new`}>
                            <div className="cursor-pointer rounded-2xl border border-blue-500/20 bg-blue-500/10 p-6 shadow-lg shadow-blue-950/20 backdrop-blur transition-all hover:border-blue-400/40 hover:bg-blue-500/15">
                                <p className="mb-2 text-sm text-blue-200">Deliverables</p>
                                <p className="font-medium text-white">Submit Proposal / SoW</p>
                                <p className="mt-2 text-sm text-blue-100/70">
                                    Open the student submission form for Process 3 deliverables.
                                </p>
                            </div>
                        </Link>

                        <div className="cursor-default">
                            <GithubStatusCard
                                integration={
                                    githubIntegration?.data?.status === "inactive"
                                        ? undefined
                                        : githubIntegration?.data
                                }
                            />
                        </div>

                        <div className="cursor-default">
                            <JiraStatusCard
                                integration={
                                    jiraIntegration?.data?.status === "inactive"
                                        ? undefined
                                        : jiraIntegration?.data
                                }
                            />
                        </div>

                        {finalGrades && <FinalGradeCard data={finalGrades} />}

                        <div className="rounded-2xl border border-white/10 bg-gray-900/70 p-6 shadow-lg shadow-black/20 backdrop-blur">
                            <p className="text-sm text-gray-400 mb-2">Created At</p>
                            <p className="text-white font-medium">{formatDate(group.createdAt)}</p>
                        </div>

                        <div className="rounded-2xl border border-white/10 bg-gray-900/70 p-6 shadow-lg shadow-black/20 backdrop-blur">
                            <p className="text-sm text-gray-400 mb-2">Updated At</p>
                            <p className="text-white font-medium">{formatDate(group.updatedAt)}</p>
                        </div>
                    </div>

                    <div className="rounded-2xl border border-white/10 bg-gray-900/70 p-7 shadow-lg shadow-black/20 backdrop-blur mb-6">
                        <div className="flex flex-col sm:flex-row sm:items-center justify-between mb-5 gap-4">
                            <div>
                                <h2 className="text-2xl font-semibold">Members</h2>
                                <span className="text-sm text-gray-400">
                                    {group.members?.length ?? 0} / 8
                                </span>
                            </div>

                            {isCoordinator && (
                                <form onSubmit={handleCoordInvite} className="flex items-center gap-2">
                                    <input
                                        type="number"
                                        placeholder="Student ID"
                                        value={coordInviteId}
                                        onChange={(e) => setCoordInviteId(e.target.value)}
                                        className="rounded-lg border border-white/10 bg-gray-900 px-3 py-2 text-sm text-white placeholder-gray-500 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 transition w-32"
                                        min={1}
                                        required
                                    />
                                    <button
                                        type="submit"
                                        disabled={coordInviting || !coordInviteId.trim()}
                                        className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold hover:bg-blue-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                                    >
                                        {coordInviting ? "Adding..." : "Add"}
                                    </button>
                                </form>
                            )}
                        </div>

                        <div className="space-y-4">
                            {group.members?.map((member: ApiGroupMember) => {
                                const isLeaderMember = member.role?.toLowerCase() === "leader";
                                const canRemove = isCoordinator || (isLeader && !isLeaderMember);

                                return (
                                <div
                                    key={member.userId}
                                    className="flex items-center justify-between rounded-xl bg-white/5 px-5 py-4"
                                >
                                    <div>
                                        <p className="text-lg font-medium">{member.fullName}</p>
                                        <p className="text-sm text-gray-400 mt-1">
                                            Student ID: {member.studentId}
                                        </p>
                                    </div>

                                    <div className="flex items-center gap-3">
                                        <span
                                            className={[
                                                "rounded-full px-3 py-1 text-xs font-medium capitalize",
                                                isLeaderMember
                                                    ? "bg-blue-500/20 text-blue-400"
                                                    : "bg-white/10 text-gray-300",
                                            ].join(" ")}
                                        >
                                            {member.role}
                                        </span>

                                        {canRemove && (
                                            <button
                                                onClick={() => handleRemoveMember(member.studentId, isLeaderMember)}
                                                disabled={removingMemberId === member.studentId}
                                                className="rounded-lg bg-red-500/15 px-3 py-2 text-xs font-medium text-red-300 hover:bg-red-500/25 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                                            >
                                                {removingMemberId === member.studentId
                                                    ? "Removing..."
                                                    : "Remove"}
                                            </button>
                                        )}
                                    </div>
                                </div>
                            )})}

                            {(!group.members || group.members.length === 0) && (
                                <p className="text-gray-500 text-sm">No members yet.</p>
                            )}
                        </div>

                        {removeSuccess && (
                            <p className="mt-4 text-sm text-green-400 font-medium">
                                ✓ {removeSuccess}
                            </p>
                        )}
                        {removeError && (
                            <p className="mt-4 text-sm text-red-400 font-medium">
                                ✗ {removeError}
                            </p>
                        )}
                    </div>

                    <AdvisorRequestPanel
                        groupId={group.id}
                        leaderId={group.leaderId}
                        advisorId={group.advisorId ?? null}
                    />

                    {isLeader && (
                        <div className="rounded-2xl border border-blue-500/20 bg-blue-500/5 p-7 shadow-lg shadow-black/20 backdrop-blur mb-6">
                            <h2 className="text-xl font-semibold mb-1">Invite a Member</h2>
                            <p className="text-sm text-gray-400 mb-5">
                                Enter the student&apos;s ID to send them a group membership invite.
                            </p>

                            <form
                                onSubmit={handleInvite}
                                className="flex flex-col gap-4 sm:flex-row sm:items-end"
                            >
                                <div className="flex-1">
                                    <label
                                        htmlFor="invite-student-id"
                                        className="block text-sm text-gray-300 mb-2 font-medium"
                                    >
                                        Student ID
                                    </label>
                                    <input
                                        id="invite-student-id"
                                        type="number"
                                        placeholder="e.g. 42"
                                        value={inviteStudentId}
                                        onChange={(e) => setInviteStudentId(e.target.value)}
                                        className="w-full rounded-xl border border-white/10 bg-gray-900 px-4 py-3 text-white placeholder-gray-500 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 transition"
                                        min={1}
                                        required
                                    />
                                </div>

                                <button
                                    type="submit"
                                    disabled={inviting || !inviteStudentId.trim()}
                                    className="rounded-xl bg-blue-600 px-6 py-3 text-sm font-semibold hover:bg-blue-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                                >
                                    {inviting ? "Sending..." : "Send Invite"}
                                </button>
                            </form>

                            {inviteSuccess && (
                                <p className="mt-3 text-sm text-green-400 font-medium">
                                    ✓ {inviteSuccess}
                                </p>
                            )}
                            {inviteError && (
                                <p className="mt-3 text-sm text-red-400 font-medium">
                                    ✗ {inviteError}
                                </p>
                            )}
                        </div>
                    )}

                    {isLeader && (
                        <div className="rounded-2xl border border-red-500/20 bg-red-500/5 p-7 shadow-lg shadow-black/20 backdrop-blur mb-6">
                            <h2 className="text-xl font-semibold mb-1 text-red-400">Danger Zone: Disband Group</h2>
                            <p className="text-sm text-gray-400 mb-5">
                                Completely delete the group and remove all members. This action cannot be undone.
                            </p>

                            <button
                                onClick={handleDisbandGroup}
                                disabled={disbanding}
                                className="rounded-xl bg-red-600 px-6 py-3 text-sm font-semibold hover:bg-red-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                            >
                                {disbanding ? "Disbanding..." : "Disband Group"}
                            </button>

                            {disbandError && (
                                <p className="mt-3 text-sm text-red-400 font-medium">
                                    ✗ {disbandError}
                                </p>
                            )}
                        </div>
                    )}

                    {isStudent && isGroupMember && !isLeader && (
                        <div className="rounded-2xl border border-red-500/20 bg-red-500/5 p-7 shadow-lg shadow-black/20 backdrop-blur">
                            <h2 className="text-xl font-semibold mb-1">Leave Group</h2>
                            <p className="text-sm text-gray-400 mb-5">
                                You can leave this group voluntarily. This action requires confirmation.
                            </p>

                            <button
                                onClick={handleLeaveGroup}
                                disabled={leaving}
                                className="rounded-xl bg-red-600 px-6 py-3 text-sm font-semibold hover:bg-red-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                            >
                                {leaving ? "Leaving..." : "Leave Group"}
                            </button>

                            {leaveError && (
                                <p className="mt-3 text-sm text-red-400 font-medium">
                                    ✗ {leaveError}
                                </p>
                            )}
                        </div>
                    )}
                </div>

                {/* Coordinator Leader Removal Modal */}
                {showLeaderModal && (
                    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm px-4">
                        <div className="bg-gray-900 border border-white/10 p-6 rounded-2xl shadow-xl w-full max-w-md">
                            <h3 className="text-xl font-semibold text-white mb-2">Reassign Leader</h3>
                            <p className="text-sm text-gray-400 mb-4">
                                You are removing the current group leader. You must assign a new leader before proceeding.
                            </p>

                            <div className="mb-6">
                                <label className="block text-sm font-medium text-gray-300 mb-2">Select New Leader</label>
                                <select 
                                    className="w-full bg-gray-800 border border-white/10 rounded-xl px-4 py-3 text-white focus:outline-none focus:ring-1 focus:ring-blue-500"
                                    value={newLeaderIdSelection}
                                    onChange={(e) => setNewLeaderIdSelection(e.target.value)}
                                >
                                    <option value="" disabled>-- Select a member --</option>
                                    {group.members
                                        ?.filter(m => m.studentId !== memberToRemoveAsLeader)
                                        .map(m => (
                                            <option key={m.userId} value={m.userId}>{m.fullName} ({m.studentId})</option>
                                        ))}
                                </select>
                            </div>

                            <div className="flex justify-end gap-3">
                                <button 
                                    onClick={() => {
                                        setShowLeaderModal(false);
                                        setMemberToRemoveAsLeader(null);
                                        setNewLeaderIdSelection("");
                                    }}
                                    className="px-4 py-2 rounded-xl text-sm font-medium text-gray-300 hover:bg-white/5 transition"
                                >
                                    Cancel
                                </button>
                                <button 
                                    onClick={submitLeaderRemoval}
                                    disabled={!newLeaderIdSelection}
                                    className="px-4 py-2 rounded-xl bg-red-600 text-sm font-medium text-white hover:bg-red-500 disabled:opacity-50 transition"
                                >
                                    Confirm Removal
                                </button>
                            </div>
                        </div>
                    </div>
                )}
            </main>
        </div>
    );
}
