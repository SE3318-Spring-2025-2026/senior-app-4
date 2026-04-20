"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import StatusBadge from "@/components/StatusBadge";
import GithubStatusCard from "@/components/GithubStatusCard";
import JiraStatusCard from "@/components/JiraStatusCard";
import { fetchGroupDetail } from "@/lib/groups-api";
import { inviteMemberApi } from "@/lib/notifications-api";
import { getUser } from "@/lib/auth";
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

    const [group, setGroup] = useState<any>(null);
    const [githubIntegration, setGithubIntegration] = useState<GithubIntegrationApiResponse | null>(null);
    const [jiraIntegration, setJiraIntegration] = useState<JiraIntegrationApiResponse | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    // Invite UI state
    const [inviteStudentId, setInviteStudentId] = useState("");
    const [inviting, setInviting] = useState(false);
    const [inviteSuccess, setInviteSuccess] = useState("");
    const [inviteError, setInviteError] = useState("");

    const currentUser = getUser();
    const isLeader =
        currentUser?.userId != null && group?.leaderId === currentUser.userId;

    useEffect(() => {
        let cancelled = false;

        async function loadGroup() {
            try {
                setLoading(true);
                setError("");

                const [groupData, githubData, jiraData] = await Promise.all([
                    fetchGroupDetail(groupId),
                    fetchGithubIntegration(groupId),
                    fetchJiraIntegration(groupId),
                ]);

                if (cancelled) return;
                setGroup(groupData);
                setGithubIntegration(githubData);
                setJiraIntegration(jiraData);
            } catch (err) {
                if (cancelled) return;
                const message =
                    err instanceof Error ? err.message : "Failed to load group details.";
                setError(message);
            } finally {
                if (!cancelled) setLoading(false);
            }
        }

        if (!Number.isNaN(groupId)) {
            loadGroup();
        }

        return () => { cancelled = true; };
    }, [groupId]);

    async function handleInvite(e: React.FormEvent) {
        e.preventDefault();
        setInviteError("");
        setInviteSuccess("");

        const studentId = parseInt(inviteStudentId.trim(), 10);
        if (Number.isNaN(studentId) || studentId <= 0) {
            setInviteError("Please enter a valid numeric student ID.");
            return;
        }

        setInviting(true);
        try {
            await inviteMemberApi(groupId, studentId);
            setInviteSuccess("Invitation sent successfully!");
            setInviteStudentId("");
        } catch (err) {
            setInviteError(
                err instanceof Error ? err.message : "Failed to send invitation."
            );
        } finally {
            setInviting(false);
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
        <main className="min-h-screen bg-gray-950 px-6 py-10 text-white">
            <div className="mx-auto max-w-5xl">
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
                    <StatusBadge status={group.status.toLowerCase()} />
                </div>

                <div className="grid gap-6 md:grid-cols-2 mb-8">
                    <GithubStatusCard
                        integration={
                            githubIntegration?.data?.status === "inactive"
                                ? undefined
                                : githubIntegration?.data
                        }
                    />

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

                {/* Members list */}
                <div className="rounded-2xl border border-white/10 bg-gray-900/70 p-7 shadow-lg shadow-black/20 backdrop-blur mb-6">
                    <div className="flex items-center justify-between mb-5">
                        <h2 className="text-2xl font-semibold">Members</h2>
                        <span className="text-sm text-gray-400">
                            {group.members?.length ?? 0} / 8
                        </span>
                    </div>

                    <div className="space-y-4">
                        {group.members?.map((member: any) => (
                            <div
                                key={member.userId}
                                className="flex items-center justify-between rounded-xl bg-white/5 px-5 py-4"
                            >
                                <div>
                                    <p className="text-lg font-medium">{member.fullName}</p>
                                    <p className="text-sm text-gray-400 mt-1">ID: {member.userId}</p>
                                </div>

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
                            </div>
                        ))}

                        {(!group.members || group.members.length === 0) && (
                            <p className="text-gray-500 text-sm">No members yet.</p>
                        )}
                    </div>
                </div>

                {/* Invite Member — only visible to the group leader */}
                {isLeader && (
                    <div className="rounded-2xl border border-blue-500/20 bg-blue-500/5 p-7 shadow-lg shadow-black/20 backdrop-blur">
                        <h2 className="text-xl font-semibold mb-1">Invite a Member</h2>
                        <p className="text-sm text-gray-400 mb-5">
                            Enter the student&apos;s user ID to send them a group membership invite.
                        </p>

                        <form onSubmit={handleInvite} className="flex items-end gap-4">
                            <div className="flex-1">
                                <label
                                    htmlFor="invite-student-id"
                                    className="block text-sm text-gray-300 mb-2 font-medium"
                                >
                                    Student User ID
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
                                {inviting ? "Sending…" : "Send Invite"}
                            </button>
                        </form>

                        {/* Feedback messages */}
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
            </div>
        </main>
    );
}