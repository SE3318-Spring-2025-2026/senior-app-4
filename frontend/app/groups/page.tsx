"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter, useSearchParams, usePathname } from "next/navigation";
import GroupCard from "@/components/GroupCard";
import GroupCardSkeleton from "@/components/GroupCardSkeleton";
import AppTopbar from "@/components/AppTopbar";
import { useNotifications } from "@/components/NotificationProvider";
import { fetchGroups, ApiGroupListItem } from "@/lib/groups-api";
import { Group } from "@/lib/group-types";

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

    const pageSize = 6;
    const { unreadOrPendingCount } = useNotifications();

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

                const response = await fetchGroups(page, pageSize, statusFilter, searchQuery, advisorFilter);

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
                if (!cancelled) {
                    setLoading(false);
                }
            }
        }

        loadGroups();

        return () => {
            cancelled = true;
        };
    }, [page, statusFilter, searchQuery, advisorFilter]);

    const sortedGroups = useMemo(() => {
        return [...groups].sort((a, b) => a.groupName.localeCompare(b.groupName));
    }, [groups]);

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

                    <div className="rounded-2xl border border-white/10 bg-gray-900 px-4 py-3 text-sm text-gray-400 shadow-lg shadow-black/20">
                        <span className="font-semibold text-white">{sortedGroups.length}</span>{" "}
                        groups listed
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
                                    isOwnGroup={false}
                                />
                            ))}
                        </div>

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
                                <span className="text-white">{Math.max(totalPages, 1)}</span>
                            </span>

                            <button
                                onClick={() => updateParams({ page: Math.min(page + 1, totalPages - 1) })}
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