"use client";

import { useEffect, useMemo, useState } from "react";
import GroupCard from "@/components/GroupCard";
import GroupCardSkeleton from "@/components/GroupCardSkeleton";
import AppTopbar from "@/components/AppTopbar";
import { useNotifications } from "@/components/NotificationProvider";
import { fetchGroups, createGroupApi, ApiGroupListItem } from "@/lib/groups-api";
import { Group } from "@/lib/group-types";
import { getUser } from "@/lib/auth";
import { showToast } from "@/components/toast/ToastContext";

function mapApiGroupToUiGroup(apiGroup: ApiGroupListItem): Group {
    return {
        groupId: apiGroup.id,
        groupName: apiGroup.groupName,
        status: apiGroup.status.toLowerCase() as Group["status"],
        leaderId: apiGroup.leaderId,
        leaderName: `Leader #${apiGroup.leaderId}`,
        advisorId: apiGroup.advisorId ?? null,
        advisorName: apiGroup.advisorId ? `Advisor #${apiGroup.advisorId}` : "Not Assigned",
        memberCount: apiGroup.memberCount,
        members: [],
        githubBound: false,
        jiraBound: false,
        createdAt: apiGroup.createdAt,
        updatedAt: apiGroup.createdAt,
    };
}

export default function GroupsPage() {
    const [groups, setGroups] = useState<Group[]>([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(1);
    const [error, setError] = useState("");

    const [groupName, setGroupName] = useState("");
    const [creating, setCreating] = useState(false);
    const [createError, setCreateError] = useState("");
    const [createSuccess, setCreateSuccess] = useState("");
    const [showCreateForm, setShowCreateForm] = useState(false);

    const pageSize = 6;
    const { unreadOrPendingCount } = useNotifications();
    const currentUser = getUser();

    useEffect(() => {
        let cancelled = false;

        async function loadGroups() {
            try {
                setLoading(true);
                setError("");

                const response = await fetchGroups(page, pageSize);

                if (cancelled) return;

                const mappedGroups = response.content.map(mapApiGroupToUiGroup);
                setGroups(mappedGroups);
                setTotalPages(Math.max(response.totalPages, 1));
            } catch (err) {
                if (cancelled) return;

                const message =
                    err instanceof Error ? err.message : "Failed to load groups.";
                setError(message);
                setGroups([]);
                setTotalPages(1);
            } finally {
                if (!cancelled) setLoading(false);
            }
        }

        loadGroups();

        return () => {
            cancelled = true;
        };
    }, [page]);

    const sortedGroups = useMemo(() => {
        return [...groups].sort((a, b) => a.groupName.localeCompare(b.groupName));
    }, [groups]);

    async function handleCreateGroup(e: React.FormEvent) {
        e.preventDefault();
        setCreateError("");
        setCreateSuccess("");

        const trimmed = groupName.trim();

        if (trimmed.length < 3 || trimmed.length > 100) {
            const message = "Group name must be between 3 and 100 characters.";
            setCreateError(message);
            showToast(message, "warning");
            return;
        }

        setCreating(true);
        try {
            await createGroupApi(trimmed);
            setCreateSuccess("Group created successfully!");
            showToast("Group created successfully!", "success");
            setGroupName("");
            setShowCreateForm(false);
            setPage(0);

            const response = await fetchGroups(0, pageSize);
            const mappedGroups = response.content.map(mapApiGroupToUiGroup);
            setGroups(mappedGroups);
            setTotalPages(Math.max(response.totalPages, 1));
        } catch (err) {
            const message =
                err instanceof Error ? err.message : "Failed to create group.";
            setCreateError(message);
            showToast(message, "error");
        } finally {
            setCreating(false);
        }
    }

    return (
        <main className="min-h-screen bg-gray-950 px-6 py-10">
            <div className="mx-auto max-w-6xl">
                <AppTopbar title="Groups" notificationCount={unreadOrPendingCount} />

                <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
                    <div>
                        <h1 className="text-3xl font-bold text-white">Project Groups</h1>
                        <p className="mt-2 text-gray-400">
                            Browse all groups and view their current project status.
                        </p>
                    </div>

                    <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
                        <div className="rounded-2xl border border-white/10 bg-gray-900 px-4 py-3 text-sm text-gray-400 shadow-lg shadow-black/20">
                            <span className="font-semibold text-white">{sortedGroups.length}</span>{" "}
                            groups listed
                        </div>

                        <button
                            onClick={() => {
                                setShowCreateForm((prev) => !prev);
                                setCreateError("");
                                setCreateSuccess("");
                            }}
                            className="rounded-2xl bg-green-600 px-5 py-3 text-sm font-semibold text-white hover:bg-green-500 transition-colors"
                        >
                            {showCreateForm ? "Close" : "+ Create Group"}
                        </button>
                    </div>
                </div>

                {showCreateForm && (
                    <div className="mb-8 rounded-2xl border border-green-500/20 bg-green-500/5 p-6 shadow-lg shadow-black/20">
                        <h2 className="text-xl font-semibold text-white mb-2">
                            Create New Group
                        </h2>
                        <p className="text-sm text-gray-400 mb-4">
                            Enter a group name to create a new project group.
                        </p>

                        <form
                            onSubmit={handleCreateGroup}
                            className="flex flex-col gap-4 sm:flex-row sm:items-end"
                        >
                            <div className="flex-1">
                                <input
                                    type="text"
                                    placeholder="Group name"
                                    value={groupName}
                                    onChange={(e) => setGroupName(e.target.value)}
                                    className="w-full rounded-xl border border-white/10 bg-gray-900 px-4 py-3 text-white placeholder-gray-500 focus:border-green-500 focus:outline-none focus:ring-1 focus:ring-green-500 transition"
                                    maxLength={100}
                                />
                            </div>

                            <div className="flex gap-3">
                                <button
                                    type="submit"
                                    disabled={creating}
                                    className="rounded-xl bg-green-600 px-6 py-3 text-sm font-semibold text-white hover:bg-green-500 disabled:opacity-50"
                                >
                                    {creating ? "Creating..." : "Create"}
                                </button>

                                <button
                                    type="button"
                                    onClick={() => {
                                        setShowCreateForm(false);
                                        setCreateError("");
                                        setCreateSuccess("");
                                        setGroupName("");
                                    }}
                                    className="rounded-xl border border-white/10 bg-gray-900 px-6 py-3 text-sm font-semibold text-gray-300 hover:bg-white/5"
                                >
                                    Cancel
                                </button>
                            </div>
                        </form>

                        {createSuccess && (
                            <p className="mt-3 text-green-400 text-sm">
                                ✓ {createSuccess}
                            </p>
                        )}

                        {createError && (
                            <p className="mt-3 text-red-400 text-sm">
                                ✗ {createError}
                            </p>
                        )}
                    </div>
                )}

                {loading ? (
                    <div className="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-3">
                        {Array.from({ length: 4 }).map((_, i) => (
                            <GroupCardSkeleton key={i} />
                        ))}
                    </div>
                ) : error ? (
                    <div className="rounded-2xl border border-red-500/20 bg-red-500/10 px-6 py-16 text-center shadow-lg shadow-black/20">
                        <div className="mx-auto max-w-md">
                            <h2 className="text-xl font-semibold text-red-300">
                                Could not load groups
                            </h2>
                            <p className="mt-2 text-red-200/80">{error}</p>
                        </div>
                    </div>
                ) : sortedGroups.length === 0 ? (
                    <div className="rounded-2xl border border-dashed border-white/10 bg-gray-900 px-6 py-16 text-center shadow-lg shadow-black/20">
                        <div className="mx-auto max-w-md">
                            <h2 className="text-xl font-semibold text-white">No groups found</h2>
                            <p className="mt-2 text-gray-400">
                                There are currently no project groups to display.
                            </p>
                        </div>
                    </div>
                ) : (
                    <>
                        <div className="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-3">
                            {sortedGroups.map((group) => (
                                <GroupCard
                                    key={group.groupId}
                                    group={group}
                                    isOwnGroup={currentUser?.userId === group.leaderId}
                                />
                            ))}
                        </div>

                        <div className="mt-8 flex items-center justify-center gap-3">
                            <button
                                onClick={() => setPage((prev) => Math.max(prev - 1, 0))}
                                disabled={page === 0}
                                className="rounded-xl border border-white/10 bg-gray-900 px-4 py-2 text-sm text-gray-300 transition-colors disabled:cursor-not-allowed disabled:opacity-40 hover:bg-white/5"
                            >
                                Previous
                            </button>

                            <span className="text-sm text-gray-400">
                                Page <span className="text-white">{page + 1}</span> /{" "}
                                <span className="text-white">{Math.max(totalPages, 1)}</span>
                            </span>

                            <button
                                onClick={() =>
                                    setPage((prev) => Math.min(prev + 1, totalPages - 1))
                                }
                                disabled={page >= totalPages - 1}
                                className="rounded-xl border border-white/10 bg-gray-900 px-4 py-2 text-sm text-gray-300 transition-colors disabled:cursor-not-allowed disabled:opacity-40 hover:bg-white/5"
                            >
                                Next
                            </button>
                        </div>
                    </>
                )}
            </div>
        </main>
    );
}