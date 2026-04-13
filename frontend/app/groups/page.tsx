"use client";

import { useEffect, useState } from "react";
import GroupCard from "@/components/GroupCard";
import GroupCardSkeleton from "@/components/GroupCardSkeleton";
import { mockGroups } from "@/lib/mock-groups";
import { Group } from "@/lib/group-types";

export default function GroupsPage() {
    const [groups, setGroups] = useState<Group[]>([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(0);
    const pageSize = 6;

    useEffect(() => {
        const timer = setTimeout(() => {
            setGroups(mockGroups);
            setLoading(false);
        }, 1200);

        return () => clearTimeout(timer);
    }, []);

    const sortedGroups = [...groups].sort((a, b) => {
        const aOwn = a.leaderName === "Miray Yıldırım";
        const bOwn = b.leaderName === "Miray Yıldırım";

        if (aOwn) return -1;
        if (bOwn) return 1;
        return a.groupName.localeCompare(b.groupName);
    });

    const start = page * pageSize;
    const end = start + pageSize;
    const paginatedGroups = sortedGroups.slice(start, end);
    const totalPages = Math.ceil(sortedGroups.length / pageSize);

    return (
        <main className="min-h-screen bg-gray-950 px-6 py-10">
            <div className="mx-auto max-w-6xl">
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

                {loading ? (
                    <div className="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-3">
                        {Array.from({ length: 4 }).map((_, i) => (
                            <GroupCardSkeleton key={i} />
                        ))}
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
                            {paginatedGroups.map((group) => (
                                <GroupCard
                                    key={group.groupId}
                                    group={group}
                                    isOwnGroup={group.leaderName === "Miray Yıldırım"}
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
                                onClick={() => setPage((prev) => Math.min(prev + 1, totalPages - 1))}
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