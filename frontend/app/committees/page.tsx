"use client";

import Sidebar from "@/components/Sidebar";
import Link from "next/link";
import { Suspense, useEffect, useState, useRef } from "react";
import { fetchCommittees } from "@/lib/committees-api";
import { CommitteeListItem } from "@/lib/committee-types";
import { showToast } from "@/components/toast/ToastContext";
import { useRouter, useSearchParams, usePathname } from "next/navigation";
import { getUser } from "@/lib/auth";

export default function CommitteesPage() {
    return (
        <Suspense fallback={<div className="min-h-screen bg-gray-950" />}>
            <CommitteesPageContent />
        </Suspense>
    );
}

function CommitteesPageContent() {
    const [committees, setCommittees] = useState<CommitteeListItem[]>([]);
    const [loading, setLoading] = useState(true);

    const router = useRouter();
    const searchParams = useSearchParams();
    const pathname = usePathname();

    const currentUser = getUser();
    const currentUserId = currentUser?.userId ? Number(currentUser.userId) : null;

    const initialStatus = searchParams.get("status") || "ALL";
    const initialSearch = searchParams.get("search") || "";
    const initialSort = searchParams.get("sort") || "created_desc";
    const initialPage = Number(searchParams.get("page") || "1");
    const initialPageSize = Number(searchParams.get("size") || "10");

    const [statusFilter, setStatusFilter] = useState(initialStatus);
    const [searchInput, setSearchInput] = useState(initialSearch);
    const [searchQuery, setSearchQuery] = useState(initialSearch);
    const [sortOption, setSortOption] = useState(initialSort);
    const [page, setPage] = useState(initialPage);
    const [pageSize, setPageSize] = useState(initialPageSize);
    const [totalPages, setTotalPages] = useState(1);

    const fetchIdRef = useRef(0);

    function getStatusColor(status: string | undefined) {
        switch (status) {
            case "ACTIVE":
                return "border-cyan-400/40 bg-cyan-400/15 text-cyan-300 font-semibold";
            case "INACTIVE":
                return "border-pink-500/40 bg-pink-500/15 text-pink-400 font-semibold";
            case "COMPLETED":
                return "border-orange-400/40 bg-orange-400/15 text-orange-300 font-semibold";
            default:
                return "border-gray-500/20 bg-gray-500/10 text-gray-400";
        }
    }

    function isMyCommittee(committee: CommitteeListItem): boolean {
        if (!currentUserId) return false;
        const inAdvisors = committee.advisors?.some((a) => a.userId === currentUserId) ?? false;
        const inJury = committee.jury?.some((j) => j.userId === currentUserId) ?? false;
        return inAdvisors || inJury;
    }

    function updateUrlParams(updates: Record<string, string | number>) {
        const params = new URLSearchParams(searchParams.toString());
        Object.entries(updates).forEach(([key, value]) => {
            if (value === "" || value === "ALL" || value === "created_desc" || value === 1 || value === 10) {
                params.delete(key);
            } else {
                params.set(key, String(value));
            }
        });
        const query = params.toString();
        router.replace(query ? `${pathname}?${query}` : pathname);
    }

    async function loadCommittees(
        nextPage = page,
        nextStatus = statusFilter,
        nextSearch = searchQuery,
        nextSort = sortOption,
        nextSize = pageSize
    ) {
        setLoading(true);
        const currentFetchId = ++fetchIdRef.current;

        try {
            const response = await fetchCommittees(nextPage - 1, nextSize, nextStatus, nextSearch, nextSort);
            if (currentFetchId !== fetchIdRef.current) return;
            setCommittees(response.content);
            setTotalPages(response.totalPages);
        } catch (err) {
            if (currentFetchId === fetchIdRef.current) {
                showToast(err instanceof Error ? err.message : "Failed to load committees.", "error");
            }
        } finally {
            if (currentFetchId === fetchIdRef.current) setLoading(false);
        }
    }

    useEffect(() => {
        setPage(1);
        setSearchQuery(searchInput);
        updateUrlParams({ search: searchInput, page: 1 });
    }, [searchInput]);

    useEffect(() => {
        loadCommittees(1, statusFilter, searchQuery, sortOption, pageSize);
    }, [statusFilter, searchQuery, sortOption, pageSize]);

    function handlePageChange(nextPage: number) {
        if (nextPage < 1 || nextPage > totalPages) return;
        setPage(nextPage);
        updateUrlParams({ page: nextPage });
        loadCommittees(nextPage, statusFilter, searchQuery, sortOption, pageSize);
    }

    // Sort: my committees first
    const displayCommittees = [...committees].sort((a, b) => {
        const aScore = isMyCommittee(a) ? 0 : 1;
        const bScore = isMyCommittee(b) ? 0 : 1;
        return aScore - bScore;
    });

    return (
        <div className="min-h-screen bg-gray-950 flex">
            <Sidebar activePage="committees" />

            <main className="flex-1 min-w-0 px-6 py-10 text-white">
                <div className="mx-auto max-w-6xl space-y-8">
                    <div>
                        <h1 className="text-3xl font-bold">Committees</h1>
                        <p className="mt-2 text-gray-400">
                            Browse committees and view their current assignments.
                        </p>
                    </div>

                    <div className="rounded-2xl border border-white/10 bg-gray-900/70 p-6">
                        <div className="mb-6 grid grid-cols-1 gap-4 md:grid-cols-4">
                            <input
                                type="text"
                                placeholder="Search committees..."
                                value={searchInput}
                                onChange={(e) => setSearchInput(e.target.value)}
                                className="rounded-xl border border-white/10 bg-gray-950 px-4 py-3 text-sm text-white placeholder:text-gray-500 outline-none focus:border-blue-500"
                            />

                            <select
                                value={statusFilter}
                                onChange={(e) => {
                                    setPage(1);
                                    setStatusFilter(e.target.value);
                                    updateUrlParams({ status: e.target.value, page: 1 });
                                }}
                                className="rounded-xl border border-white/10 bg-gray-950 px-4 py-3 text-sm text-white outline-none focus:border-blue-500"
                            >
                                <option value="ALL">All Statuses</option>
                                <option value="ACTIVE">ACTIVE</option>
                                <option value="INACTIVE">INACTIVE</option>
                                <option value="COMPLETED">COMPLETED</option>
                            </select>

                            <select
                                value={sortOption}
                                onChange={(e) => {
                                    setPage(1);
                                    setSortOption(e.target.value);
                                    updateUrlParams({ sort: e.target.value, page: 1 });
                                }}
                                className="rounded-xl border border-white/10 bg-gray-950 px-4 py-3 text-sm text-white outline-none focus:border-blue-500"
                            >
                                <option value="created_desc">Created Date ↓</option>
                                <option value="created_asc">Created Date ↑</option>
                                <option value="name_asc">Name A-Z</option>
                                <option value="name_desc">Name Z-A</option>
                            </select>

                            <select
                                value={pageSize}
                                onChange={(e) => {
                                    const nextSize = Number(e.target.value);
                                    setPage(1);
                                    setPageSize(nextSize);
                                    updateUrlParams({ size: nextSize, page: 1 });
                                }}
                                className="rounded-xl border border-white/10 bg-gray-950 px-4 py-3 text-sm text-white outline-none focus:border-blue-500"
                            >
                                <option value={10}>10 / page</option>
                                <option value={20}>20 / page</option>
                                <option value={50}>50 / page</option>
                            </select>
                        </div>

                        {loading ? (
                            <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
                                {Array.from({ length: 6 }).map((_, index) => (
                                    <div
                                        key={index}
                                        className="h-44 animate-pulse rounded-2xl border border-white/10 bg-white/5"
                                    />
                                ))}
                            </div>
                        ) : committees.length === 0 ? (
                            <div className="rounded-2xl border border-dashed border-white/10 bg-white/5 px-6 py-16 text-center">
                                <p className="text-gray-400">
                                    No committees found. Try adjusting your filters.
                                </p>
                            </div>
                        ) : (
                            <>
                                <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
                                    {displayCommittees.map((committee) => {
                                        const mine = isMyCommittee(committee);
                                        return (
                                            <Link
                                                key={committee.committeeId}
                                                href={`/committees/${committee.committeeId}`}
                                            >
                                                <div
                                                    className={`h-full cursor-pointer rounded-2xl border p-5 transition ${
                                                        mine
                                                            ? "border-blue-500/60 bg-blue-950/40 shadow-lg shadow-blue-500/20 ring-1 ring-blue-500/30 hover:border-blue-400/80 hover:shadow-blue-400/30"
                                                            : "border-white/10 bg-gray-950 hover:border-blue-500/40 hover:bg-white/5"
                                                    }`}
                                                >
                                                    <div className="flex items-start justify-between gap-3">
                                                        <div className="flex-1 min-w-0">
                                                            <div className="flex items-center gap-2 flex-wrap">
                                                                <h3 className="font-semibold text-white truncate">
                                                                    {committee.committeeName}
                                                                </h3>
                                                                {mine && (
                                                                    <span className="shrink-0 rounded-full bg-blue-500/20 border border-blue-400/40 px-2 py-0.5 text-xs font-semibold text-blue-300">
                                                                        Your Committee
                                                                    </span>
                                                                )}
                                                            </div>
                                                            <p className="mt-1 text-sm text-gray-400 truncate">
                                                                {committee.description || "No description"}
                                                            </p>
                                                        </div>

                                                        <span className={`shrink-0 rounded-full border px-3 py-1 text-xs ${getStatusColor(committee.status)}`}>
                                                            {committee.status}
                                                        </span>
                                                    </div>

                                                    <div className="mt-4 grid grid-cols-3 gap-2 text-center text-xs text-gray-400">
                                                        <div className={`rounded-lg p-2 ${mine ? "bg-blue-500/10" : "bg-white/5"}`}>
                                                            <p className="text-white">{committee.advisorCount ?? 0}</p>
                                                            Advisors
                                                        </div>
                                                        <div className={`rounded-lg p-2 ${mine ? "bg-blue-500/10" : "bg-white/5"}`}>
                                                            <p className="text-white">{committee.juryCount ?? 0}</p>
                                                            Jury
                                                        </div>
                                                        <div className={`rounded-lg p-2 ${mine ? "bg-blue-500/10" : "bg-white/5"}`}>
                                                            <p className="text-white">{committee.groupCount ?? 0}</p>
                                                            Groups
                                                        </div>
                                                    </div>
                                                </div>
                                            </Link>
                                        );
                                    })}
                                </div>

                                <div className="mt-8 flex items-center justify-center gap-3">
                                    <button
                                        onClick={() => handlePageChange(page - 1)}
                                        disabled={page <= 1}
                                        className="rounded-xl border border-white/10 bg-gray-900 px-4 py-2 text-sm text-gray-300 transition-colors disabled:cursor-not-allowed disabled:opacity-40 hover:bg-white/5"
                                    >
                                        Previous
                                    </button>

                                    <span className="text-sm text-gray-400">
                                        Page <span className="text-white">{page}</span> /{" "}
                                        <span className="text-white">{totalPages}</span>
                                    </span>

                                    <button
                                        onClick={() => handlePageChange(page + 1)}
                                        disabled={page >= totalPages}
                                        className="rounded-xl border border-white/10 bg-gray-900 px-4 py-2 text-sm text-gray-300 transition-colors disabled:cursor-not-allowed disabled:opacity-40 hover:bg-white/5"
                                    >
                                        Next
                                    </button>
                                </div>
                            </>
                        )}
                    </div>
                </div>
            </main>
        </div>
    );
}
