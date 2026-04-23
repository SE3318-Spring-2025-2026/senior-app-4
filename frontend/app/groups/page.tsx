"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter, useSearchParams, usePathname } from "next/navigation";
import GroupCard from "@/components/GroupCard";
import GroupCardSkeleton from "@/components/GroupCardSkeleton";
import AppTopbar from "@/components/AppTopbar";
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
    const router = useRouter();
    const searchParams = useSearchParams();
    const pathname = usePathname();

    const [groups, setGroups] = useState<Group[]>([]);
    const [loading, setLoading] = useState(true);
    
    const page = parseInt(searchParams.get("page") || "0");
    const statusFilter = searchParams.get("status") || "all";
    const advisorFilter = searchParams.get("advisorAssigned") || "all";
    const searchQuery = searchParams.get("groupName") || "";

    const [searchInput, setSearchInput] = useState(searchQuery);
    const [totalPages, setTotalPages] = useState(1);
    const [error, setError] = useState("");

    const [groupName, setGroupName] = useState("");
    const [creating, setCreating] = useState(false);
    const [createError, setCreateError] = useState("");
    const [createSuccess, setCreateSuccess] = useState("");
    const [showCreateForm, setShowCreateForm] = useState(false);

    const pageSize = 6;
    const currentUser = getUser();

    useEffect(() => {
        const handler = setTimeout(() => {
            if (searchInput !== searchQuery) {
                updateParams({ groupName: searchInput, page: 0 });
            }
        }, 300);
        return () => clearTimeout(handler);
    }, [searchInput, searchQuery]);

    const updateParams = (updates: Record<string, string | number>) => {
        const params = new URLSearchParams(searchParams.toString());
        Object.entries(updates).forEach(([key, value]) => {
            if (value === "" || value === "all" || value === 0) {
                if (key !== "page") params.delete(key);
                else params.set(key, "0");
            } else {
                params.set(key, String(value));
            }
        });
        router.push(`${pathname}?${params.toString()}`);
    };

    useEffect(() => {
        let cancelled = false;

        async function loadGroups() {
            try {
                setLoading(true);
                setError("");

                const response = await fetchGroups(0, 1000);

                if (cancelled) return;

                const mappedGroups = response.content.map(mapApiGroupToUiGroup);
                setGroups(mappedGroups);
            } catch (err) {
                if (cancelled) return;

                const message =
                    err instanceof Error ? err.message : "Failed to load groups.";
                setError(message);
                setGroups([]);
            } finally {
                if (!cancelled) setLoading(false);
            }
        }

        loadGroups();

        return () => {
            cancelled = true;
        };
    }, []);

    const filteredGroups = useMemo(() => {
        let filtered = [...groups];

        if (statusFilter !== "all") {
            filtered = filtered.filter(g => g.status === statusFilter);
        }

        if (advisorFilter !== "all") {
            filtered = filtered.filter(g => 
                advisorFilter === "has_advisor" ? g.advisorId !== null : g.advisorId === null
            );
        }

        if (searchQuery) {
            filtered = filtered.filter(g => g.groupName.toLowerCase().includes(searchQuery.toLowerCase()));
        }

        filtered.sort((a, b) => {
            if (currentUser && a.leaderId === currentUser.userId) return -1;
            if (currentUser && b.leaderId === currentUser.userId) return 1;
            return a.groupName.localeCompare(b.groupName);
        });

        return filtered;
    }, [groups, statusFilter, advisorFilter, searchQuery, currentUser]);

    useEffect(() => {
        setTotalPages(Math.max(Math.ceil(filteredGroups.length / pageSize), 1));
        
        if (page >= Math.max(Math.ceil(filteredGroups.length / pageSize), 1)) {
            updateParams({ page: 0 });
        }
    }, [filteredGroups.length, pageSize]);

    const paginatedGroups = useMemo(() => {
        const start = page * pageSize;
        return filteredGroups.slice(start, start + pageSize);
    }, [filteredGroups, page, pageSize]);

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
            updateParams({ page: 0 });

            const response = await fetchGroups(0, 1000);
            const mappedGroups = response.content.map(mapApiGroupToUiGroup);
            setGroups(mappedGroups);
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
                <AppTopbar />

                <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
                    <div>
                        <h1 className="text-3xl font-bold text-white">Project Groups</h1>
                        <p className="mt-2 text-gray-400">
                            Browse all groups and view their current project status.
                        </p>
                    </div>

                    <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
                        <div className="rounded-2xl border border-white/10 bg-gray-900 px-4 py-3 text-sm text-gray-400 shadow-lg shadow-black/20">
                            <span className="font-semibold text-white">{filteredGroups.length}</span>{" "}
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

                <div className="mb-6 flex flex-col md:flex-row gap-4">
                    <input
                        type="text"
                        placeholder="Search groups..."
                        value={searchInput}
                        onChange={(e) => setSearchInput(e.target.value)}
                        className="flex-1 bg-gray-900 border border-white/10 text-sm text-white rounded-xl px-4 py-3 focus:ring-1 focus:ring-blue-500 outline-none shadow-lg shadow-black/20"
                    />
                    <select
                        value={statusFilter}
                        onChange={(e) => updateParams({ status: e.target.value, page: 0 })}
                        className="bg-gray-900 border border-white/10 text-sm text-white rounded-xl px-4 py-3 focus:ring-1 focus:ring-blue-500 outline-none shadow-lg shadow-black/20"
                    >
                        <option value="all">All Statuses</option>
                        <option value="forming">Forming</option>
                        <option value="formed">Formed</option>
                        <option value="advised">Advised</option>
                        <option value="disbanded">Disbanded</option>
                    </select>
                    <select
                        value={advisorFilter}
                        onChange={(e) => updateParams({ advisorAssigned: e.target.value, page: 0 })}
                        className="bg-gray-900 border border-white/10 text-sm text-white rounded-xl px-4 py-3 focus:ring-1 focus:ring-blue-500 outline-none shadow-lg shadow-black/20"
                    >
                        <option value="all">All Advisor Status</option>
                        <option value="has_advisor">Has Advisor</option>
                        <option value="no_advisor">No Advisor</option>
                    </select>
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
                ) : paginatedGroups.length === 0 ? (
                    <div className="rounded-2xl border border-dashed border-white/10 bg-gray-900 px-6 py-16 text-center shadow-lg shadow-black/20">
                        <div className="mx-auto max-w-md">
                            <h2 className="text-xl font-semibold text-white">No groups found</h2>
                            <p className="mt-2 text-gray-400">
                                There are currently no project groups matching your filters.
                            </p>
                        </div>
                    </div>
                ) : (
                    <>
                        <div className="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-3">
                            {paginatedGroups.map((group) => (
                                <GroupCard
                                    key={group.groupId}
                                    group={group}
                                    isOwnGroup={currentUser?.userId === group.leaderId}
                                />
                            ))}
                        </div>

                        {totalPages > 1 && (
                            <div className="mt-8 flex items-center justify-center gap-3">
                                <button
                                    onClick={() => updateParams({ page: Math.max(page - 1, 0) })}
                                    disabled={page === 0}
                                    className="rounded-xl border border-white/10 bg-gray-900 px-4 py-2 text-sm text-gray-300 transition-colors disabled:cursor-not-allowed disabled:opacity-40 hover:bg-white/5"
                                >
                                    Previous
                                </button>

                                <span className="text-sm text-gray-400">
                                    Page <span className="text-white">{page + 1}</span> /{" "}
                                    <span className="text-white">{totalPages}</span>
                                </span>

                                <button
                                    onClick={() => updateParams({ page: Math.min(page + 1, totalPages - 1) })}
                                    disabled={page >= totalPages - 1}
                                    className="rounded-xl border border-white/10 bg-gray-900 px-4 py-2 text-sm text-gray-300 transition-colors disabled:cursor-not-allowed disabled:opacity-40 hover:bg-white/5"
                                >
                                    Next
                                </button>
                            </div>
                        )}
                    </>
                )}
            </div>
        </main>
    );
}