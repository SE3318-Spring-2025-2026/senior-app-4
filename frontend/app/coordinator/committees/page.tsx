"use client";

import Sidebar from "@/components/Sidebar";
import CommitteeForm from "@/components/committee/CommitteeForm";
import { toast } from "sonner";
import {
    createCommittee,
    deleteCommittee,
    fetchCoordinatorCommittees,
    updateCommittee,
} from "@/lib/committees-api";
import { Committee, CommitteeFormValues } from "@/lib/committee-types";
import { getUser } from "@/lib/auth";
import { Suspense, useCallback, useEffect, useState } from "react";
import { useRouter, useSearchParams, usePathname } from "next/navigation";
import { useAuthGuard } from "@/hooks/useAuthGuard";

export default function CoordinatorCommitteesPage() {
    return (
        <Suspense fallback={<div className="min-h-screen bg-gray-950" />}>
            <CoordinatorCommitteesPageContent />
        </Suspense>
    );
}

function CoordinatorCommitteesPageContent() {
    const authStatus = useAuthGuard("coordinator");
    const router = useRouter();
    const searchParams = useSearchParams();
    const pathname = usePathname();

    const [committees, setCommittees] = useState<Committee[]>([]);
    const [loading, setLoading] = useState(true);
    const [showCreate, setShowCreate] = useState(false);
    const [editingCommittee, setEditingCommittee] = useState<Committee | null>(null);
    const [submitting, setSubmitting] = useState(false);
    const [apiError, setApiError] = useState("");
    const [totalPages, setTotalPages] = useState(1);

    const updateUrlParams = useCallback((updates: Record<string, string | number>) => {
        const params = new URLSearchParams(searchParams.toString());

        Object.entries(updates).forEach(([key, value]) => {
            if (
                value === "" ||
                value === "ALL" ||
                value === "created_desc" ||
                value === 1 ||
                value === 10
            ) {
                params.delete(key);
            } else {
                params.set(key, String(value));
            }
        });

        const query = params.toString();
        router.replace(query ? `${pathname}?${query}` : pathname);
    }, [searchParams, router, pathname]);
    const editId = searchParams.get("edit");

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

    const currentUser = getUser();
    const coordinatorId = currentUser?.userId ?? 1;

    async function loadCommittees(
        nextPage = page,
        nextStatus = statusFilter,
        nextSearch = searchQuery,
        nextSort = sortOption,
        nextSize = pageSize
    ) {
        setLoading(true);
        try {
            const response = await fetchCoordinatorCommittees(
                coordinatorId,
                nextPage - 1,
                nextSize,
                nextStatus,
                nextSearch,
                nextSort
            );

            setCommittees(response.content);
            setTotalPages(response.totalPages);
        } catch (err) {
            toast.error(
                err instanceof Error ? err.message : "Failed to load committees."
            );
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        loadCommittees(1, statusFilter, searchQuery, sortOption, pageSize);
    }, [statusFilter, searchQuery, sortOption, pageSize]);

    useEffect(() => {
        const timeout = setTimeout(() => {
            setPage(1);
            setSearchQuery(searchInput);
            updateUrlParams({ search: searchInput, page: 1 });
        }, 300);

        return () => clearTimeout(timeout);
    }, [searchInput, updateUrlParams]);

    useEffect(() => {
        if (!editId) return;

        const committeeToEdit = committees.find(
            (c) => c.committeeId === Number(editId)
        );

        if (committeeToEdit) {
            setEditingCommittee(committeeToEdit);
            setShowCreate(false);
            setApiError("");

            router.replace(pathname);
        }
    }, [editId, committees, router, pathname]);

    async function handleCreate(values: CommitteeFormValues) {
        setSubmitting(true);
        setApiError("");

        try {
            await createCommittee(values);
            toast.success("Committee created successfully.");
            setShowCreate(false);
            await loadCommittees(1, statusFilter, searchQuery, sortOption, pageSize);
        } catch (err) {
            const message =
                err instanceof Error ? err.message : "Failed to create committee.";
            setApiError(message);
            toast.error(message);
        } finally {
            setSubmitting(false);
        }
    }

    async function handleUpdate(values: CommitteeFormValues) {
        if (!editingCommittee) return;

        setSubmitting(true);
        setApiError("");


        try {
            await updateCommittee(editingCommittee.committeeId, values);
            toast.success("Committee updated successfully.");
            setEditingCommittee(null);
            await loadCommittees(page, statusFilter, searchQuery, sortOption, pageSize);
        } catch (err) {
            const message =
                err instanceof Error ? err.message : "Failed to update committee.";
            setApiError(message);
            toast.error(message);
        } finally {
            setSubmitting(false);
        }
    }

    async function handleDelete(committeeId: number) {
        const confirmed = window.confirm(
            "Are you sure you want to delete this committee?"
        );

        if (!confirmed) return;

        try {
            await deleteCommittee(committeeId);
            toast.success("Committee deleted successfully.");
            await loadCommittees(page, statusFilter, searchQuery, sortOption, pageSize);
        } catch (err) {
            toast.error(err instanceof Error ? err.message : "Failed to delete committee.");
        }
    }

    function handlePageChange(nextPage: number) {
        if (nextPage < 1 || nextPage > totalPages) return;

        setPage(nextPage);
        updateUrlParams({ page: nextPage });
        loadCommittees(nextPage, statusFilter, searchQuery, sortOption, pageSize);
    }

    if (authStatus === "loading") return <div>Loading...</div>;
    if (authStatus === "denied") return <div>Access Denied</div>;

    return (
        <div className="min-h-screen bg-gray-950 flex">
            <Sidebar activePage="committees" />

            <main className="flex-1 min-w-0 px-6 py-10 text-white">
                <div className="mx-auto max-w-6xl space-y-8">
                    <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
                        <div>
                            <h1 className="text-3xl font-bold">Committee Management</h1>
                            <p className="mt-2 text-gray-400">
                                Create, edit, delete, and review committees.
                            </p>
                        </div>

                        <button
                            onClick={() => {
                                setShowCreate((prev) => !prev);
                                setEditingCommittee(null);
                                setApiError("");
                            }}
                            className="rounded-xl bg-blue-600 px-5 py-3 text-sm font-semibold text-white hover:bg-blue-500"
                        >
                            {showCreate ? "Close" : "+ Create Committee"}
                        </button>
                    </div>

                    {showCreate && !editingCommittee && (
                        <div className="rounded-2xl border border-white/10 bg-gray-900/70 p-6">
                            <h2 className="mb-4 text-xl font-semibold">Create Committee</h2>
                            <CommitteeForm
                                onSubmit={handleCreate}
                                isSubmitting={submitting}
                                apiError={apiError}
                            />
                        </div>
                    )}

                    {editingCommittee && (
                        <div className="rounded-2xl border border-yellow-500/20 bg-yellow-500/5 p-6">
                            <h2 className="mb-4 text-xl font-semibold">Edit Committee</h2>
                            <CommitteeForm
                                initialValues={{
                                    committeeName: editingCommittee.committeeName,
                                    description: editingCommittee.description ?? "",
                                    status: editingCommittee.status,
                                }}
                                onSubmit={handleUpdate}
                                isSubmitting={submitting}
                                apiError={apiError}
                                submitLabel="Update"
                            />
                        </div>
                    )}

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


                                <div className="mt-6 grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">

                                    {committees.map((committee) => (
                                        <div
                                            key={committee.committeeId}
                                            className="rounded-2xl border border-white/10 bg-gray-950 p-5"
                                        >
                                            <div className="flex items-start justify-between gap-3">
                                                <div>
                                                    <h3 className="font-semibold text-white">
                                                        {committee.committeeName}
                                                    </h3>
                                                    <p className="mt-1 text-sm text-gray-400">
                                                        {committee.description || "No description"}
                                                    </p>
                                                </div>

                                                <span
                                    className={`px-3 py-1 rounded-full text-xs font-semibold border ${
                                        committee.status === "ACTIVE"
                                            ? "border-cyan-400/40 bg-cyan-400/15 text-cyan-300"
                                            : committee.status === "INACTIVE"
                                            ? "border-pink-500/40 bg-pink-500/15 text-pink-400"
                                            : committee.status === "COMPLETED"
                                            ? "border-orange-400/40 bg-orange-400/15 text-orange-300"
                                            : "border-gray-500/20 bg-gray-500/10 text-gray-400"
                                    }`}
                                >
                                    {committee.status}
                                </span>
                                            </div>

                                            <div className="mt-4 grid grid-cols-3 gap-2 text-center text-xs text-gray-400">
                                                <div className="rounded-lg bg-white/5 p-2">
                                                    <p className="text-white">{committee.advisorCount ?? 0}</p>
                                                    Advisors
                                                </div>
                                                <div className="rounded-lg bg-white/5 p-2">
                                                    <p className="text-white">{committee.juryCount ?? 0}</p>
                                                    Jury
                                                </div>
                                                <div className="rounded-lg bg-white/5 p-2">
                                                    <p className="text-white">{committee.groupCount ?? 0}</p>
                                                    Groups
                                                </div>
                                            </div>

                                            <div className="mt-5 grid grid-cols-3 gap-2">
                                                <button
                                                    onClick={() => {
                                                        setEditingCommittee(committee);
                                                        setShowCreate(false);
                                                        setApiError("");
                                                    }}
                                                    className="rounded-xl bg-white/10 px-4 py-2 text-sm text-white hover:bg-white/15"
                                                >
                                                    Edit
                                                </button>

                                                <button
                                                    onClick={() => handleDelete(committee.committeeId)}
                                                    className="rounded-xl bg-red-600 px-4 py-2 text-sm text-white hover:bg-red-500"
                                                >
                                                    Delete
                                                </button>

                                                <a
                                                    href={`/committees/${committee.committeeId}?from=coordinator`}
                                                    className="rounded-xl bg-blue-600 px-4 py-2 text-center text-sm text-white hover:bg-blue-500"
                                                >
                                                    View
                                                </a>
                                            </div>
                                        </div>
                                    ))}
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
