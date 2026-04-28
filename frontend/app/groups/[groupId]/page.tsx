"use client";

import Sidebar from "@/components/Sidebar";
import { showToast } from "@/components/toast/ToastContext";
import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
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
    deleteGroupApi,
    transferAdvisorApi,
    addMemberDirectApi
} from "@/lib/groups-api";
import { fetchProfessors, ProfessorDirectoryItem } from "@/lib/advisor-api";
//import { inviteMemberApi } from "@/lib/notifications-api";
import { getUser, getToken, decodeToken } from "@/lib/auth";
import {
    fetchGithubIntegration,
    fetchJiraIntegration,
    type GithubIntegrationApiResponse,
    type JiraIntegrationApiResponse,
} from "@/lib/integrations-api";

function formatDate(dateString: string) {
    return new Date(dateString).toLocaleString("en-US", {
        day: "numeric",
        month: "long",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
    });
}

export default function GroupDetailPage() {
    const params = useParams();
    const groupId = Number(params.groupId);
    const router = useRouter();

    const [group, setGroup] = useState<ApiGroupDetail | null>(null);
    const [githubIntegration, setGithubIntegration] =
        useState<GithubIntegrationApiResponse | null>(null);
    const [jiraIntegration, setJiraIntegration] =
        useState<JiraIntegrationApiResponse | null>(null);
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

    const [professors, setProfessors] = useState<ProfessorDirectoryItem[]>([]);
    const [selectedAdvisorId, setSelectedAdvisorId] = useState("");
    const [assigningAdvisor, setAssigningAdvisor] = useState(false);
    const [coordinatorStudentId, setCoordinatorStudentId] = useState("");
    const [coordinatorAddingStudent, setCoordinatorAddingStudent] = useState(false);
    const [deletingGroup, setDeletingGroup] = useState(false);

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

                if (cancelled) return;

                setGroup({
                    ...groupData,
                    members: membersData,
                });
                setNewGroupName(groupData.groupName);
                setGithubIntegration(githubData);
                setJiraIntegration(jiraData);
                if (currentUser?.role?.toLowerCase() === "coordinator") {
                    const professorData = await fetchProfessors();
                    if (!cancelled) setProfessors(professorData);
                }
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

    async function handleRemoveMember(studentId: string) {
        setRemoveError("");
        setRemoveSuccess("");

        const confirmed = window.confirm(
            "Are you sure you want to remove this member from the group?"
        );
        if (!confirmed) return;

        setRemovingMemberId(studentId);
        try {
            await removeMemberApi(groupId, studentId);

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

    async function handleAssignAdvisor(e: React.FormEvent) {
        e.preventDefault();

        const professorId = Number(selectedAdvisorId);
        if (!professorId) {
            showToast("Please select an advisor.", "warning");
            return;
        }

        setAssigningAdvisor(true);
        try {
            await transferAdvisorApi(groupId, professorId);

            const updatedGroup = await fetchGroupDetail(groupId);
            const updatedMembers = await fetchGroupMembers(groupId);

            setGroup({
                ...updatedGroup,
                members: updatedMembers,
            });

            showToast("Advisor assigned successfully.", "success");
            setSelectedAdvisorId("");
        } catch (err) {
            showToast(
                err instanceof Error ? err.message : "Failed to assign advisor.",
                "error"
            );
        } finally {
            setAssigningAdvisor(false);
        }
    }

    async function handleCoordinatorAddStudent(e: React.FormEvent) {
        e.preventDefault();

        const studentId = Number(coordinatorStudentId.trim());
        if (!studentId) {
            showToast("Please enter a valid student ID.", "warning");
            return;
        }

        setCoordinatorAddingStudent(true);
        try {
            await addMemberDirectApi(groupId, studentId);
            const updatedMembers = await fetchGroupMembers(groupId);
            setGroup((prev) => prev ? { ...prev, members: updatedMembers } : prev);

            showToast("Student added successfully.", "success");
            setCoordinatorStudentId("");
        } catch (err) {
            showToast(
                err instanceof Error ? err.message : "Failed to add student.",
                "error"
            );
        } finally {
            setCoordinatorAddingStudent(false);
        }
    }

    async function handleDeleteGroup() {
        const confirmed = window.confirm(
            "Are you sure you want to delete/disband this group?"
        );

        if (!confirmed) return;

        setDeletingGroup(true);
        try {
            await deleteGroupApi(groupId);
            showToast("Group deleted successfully.", "success");
            router.push("/groups");
        } catch (err) {
            showToast(
                err instanceof Error ? err.message : "Failed to delete group.",
                "error"
            );
        } finally {
            setDeletingGroup(false);
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
                        <StatusBadge
                            status={group.status.toLowerCase() as "forming" | "formed" | "advised" | "disbanded"}
                        />
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

                        <Link href={`/groups/${group.id}/integrations/github`}>
                            <div className="cursor-pointer">
                                <GithubStatusCard
                                    integration={
                                        githubIntegration?.data?.status === "inactive"
                                            ? undefined
                                            : githubIntegration?.data
                                    }
                                />
                            </div>
                        </Link>

                        <JiraStatusCard
                            integration={
                                jiraIntegration?.data?.status === "inactive"
                                    ? undefined
                                    : jiraIntegration?.data
                            }
                        />

                        <div className="rounded-2xl border border-white/10 bg-gray-900/70 p-6 shadow-lg shadow-black/20 backdrop-blur">
                            <p className="text-sm text-gray-400 mb-2">Created At</p>
                            <p className="text-white font-medium">{formatDate(group.createdAt)}</p>
                        </div>

                        <div className="rounded-2xl border border-white/10 bg-gray-900/70 p-6 shadow-lg shadow-black/20 backdrop-blur">
                            <p className="text-sm text-gray-400 mb-2">Updated At</p>
                            <p className="text-white font-medium">{formatDate(group.updatedAt)}</p>
                        </div>
                    </div>

                    {!isCoordinator && (
                        <AdvisorRequestPanel
                            groupId={group.id}
                            leaderId={group.leaderId}
                            advisorId={group.advisorId ?? null}
                        />
                    )}

                    {isCoordinator && (
                        <div className="mb-8 rounded-2xl border border-orange-500/20 bg-orange-500/5 p-7 shadow-lg shadow-black/20 backdrop-blur">
                            <h2 className="text-2xl font-semibold text-white">Coordinator Controls</h2>
                            <p className="mt-2 text-sm text-gray-400">
                                Assign an advisor, add students directly, or delete this group.
                            </p>

                            <form onSubmit={handleAssignAdvisor} className="mt-6 flex flex-col gap-4 sm:flex-row sm:items-end">
                                <div className="flex-1">
                                    <label className="mb-2 block text-sm font-medium text-gray-300">
                                        Assign Advisor
                                    </label>
                                    <select
                                        value={selectedAdvisorId}
                                        onChange={(e) => setSelectedAdvisorId(e.target.value)}
                                        className="w-full rounded-xl border border-white/10 bg-gray-900 pl-4 pr-10 py-3 text-white outline-none focus:border-orange-500"
                                    >
                                        <option value="">Select advisor...</option>
                                        {professors.map((professor) => (
                                            <option key={professor.userId} value={professor.userId}>
                                                {professor.fullName}
                                            </option>
                                        ))}
                                    </select>
                                </div>

                                <button
                                    type="submit"
                                    disabled={assigningAdvisor || !selectedAdvisorId}
                                    className="rounded-xl bg-orange-600 px-6 py-3 text-sm font-semibold text-white hover:bg-orange-500 disabled:opacity-50"
                                >
                                    {assigningAdvisor ? "Assigning..." : "Assign Advisor"}
                                </button>
                            </form>

                            <form onSubmit={handleCoordinatorAddStudent} className="mt-6 flex flex-col gap-4 sm:flex-row sm:items-end">
                                <div className="flex-1">
                                    <label className="mb-2 block text-sm font-medium text-gray-300">
                                        Add Student
                                    </label>
                                    <input
                                        type="number"
                                        value={coordinatorStudentId}
                                        onChange={(e) => setCoordinatorStudentId(e.target.value)}
                                        placeholder="Student ID"
                                        className="w-full rounded-xl border border-white/10 bg-gray-900 pl-4 pr-10 py-3 text-white placeholder-gray-500 outline-none focus:border-blue-500"
                                    />
                                </div>

                                <button
                                    type="submit"
                                    disabled={coordinatorAddingStudent || !coordinatorStudentId.trim()}
                                    className="rounded-xl bg-blue-600 px-6 py-3 text-sm font-semibold text-white hover:bg-blue-500 disabled:opacity-50"
                                >
                                    {coordinatorAddingStudent ? "Adding..." : "Add Student"}
                                </button>
                            </form>

                            <div className="mt-6 border-t border-white/10 pt-6">
                                <button
                                    onClick={handleDeleteGroup}
                                    disabled={deletingGroup}
                                    className="rounded-xl bg-red-600 px-6 py-3 text-sm font-semibold text-white hover:bg-red-500 disabled:opacity-50"
                                >
                                    {deletingGroup ? "Deleting..." : "Delete Group"}
                                </button>
                            </div>
                        </div>
                    )}

                    <div className="rounded-2xl border border-white/10 bg-gray-900/70 p-7 shadow-lg shadow-black/20 backdrop-blur mb-6">
                        <div className="flex items-center justify-between mb-5">
                            <h2 className="text-2xl font-semibold">Members</h2>
                            <span className="text-sm text-gray-400">
                                {group.members?.length ?? 0} / 8
                            </span>
                        </div>

                        <div className="space-y-4">
                            {group.members?.map((member: ApiGroupMember) => (
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
                                                member.role?.toLowerCase() === "leader"
                                                    ? "bg-blue-500/20 text-blue-400"
                                                    : "bg-white/10 text-gray-300",
                                            ].join(" ")}
                                        >
                                            {member.role}
                                        </span>

                                        {(isLeader || isCoordinator) && member.role?.toLowerCase() !== "leader" && (
                                            <button
                                                onClick={() => handleRemoveMember(member.studentId)}
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
                            ))}

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
            </main>
        </div>
    );
}