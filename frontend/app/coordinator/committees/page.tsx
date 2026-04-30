"use client";

<<<<<<< HEAD
import { useState, useEffect } from "react";
import Link from "next/link";
import Sidebar from "@/components/Sidebar";
import { useAuthGuard } from "@/hooks/useAuthGuard";
import { fetchCommittees, Committee } from "@/lib/committees-api";
import CreateCommitteeForm from "@/components/committees/CreateCommitteeForm";

export default function CoordinatorCommitteesPage() {
    const authStatus = useAuthGuard("coordinator");

    if (authStatus === "loading") return (
        <div className="min-h-screen bg-gray-950 flex items-center justify-center">
            <svg className="w-6 h-6 animate-spin text-blue-500" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
            </svg>
        </div>
    );

    if (authStatus === "denied") return <AccessDenied />;
    return <DashboardLayout />;
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
                <p className="text-sm text-gray-500">Only Coordinators and Admins can access this page.</p>
            </div>
        </div>
    );
}

function DashboardLayout() {
    const [committees, setCommittees] = useState<Committee[]>([]);
    const [loading, setLoading] = useState(true);
    const [showCreateModal, setShowCreateModal] = useState(false);

    const loadCommittees = async () => {
        setLoading(true);
        try {
            const data = await fetchCommittees();
            console.log("Fetched committees data:", data);
            setCommittees(Array.isArray(data) ? data : (data as any)?.data || []);
        } catch (error) {
            console.error("Failed to fetch committees", error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadCommittees();
    }, []);

    const handleCreateSuccess = () => {
        setShowCreateModal(false);
        loadCommittees();
    };

    return (
        <div className="min-h-screen bg-gray-950 flex">
            {/* Sidebar with activePage="system-alerts" just to keep it selected or we can add a new one, but let's assume we can pass "committees" */}
            <Sidebar activePage="committees" />

            <main className="flex-1 flex flex-col min-w-0 relative">
                <div className="border-b border-white/5 px-8 py-4 flex items-center justify-between">
                    <div>
                        <h1 className="text-base font-semibold text-white">Committees</h1>
                        <p className="text-xs text-gray-500 mt-0.5">Manage evaluation committees and assignments</p>
                    </div>
                    <button
                        onClick={() => setShowCreateModal(true)}
                        className="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-500 transition-colors"
                    >
                        Create Committee
                    </button>
                </div>

                <div className="flex-1 p-8">
                    <div className="max-w-5xl mx-auto space-y-6">
                        {loading ? (
                            <div className="text-center py-10">
                                <div className="inline-block w-8 h-8 border-4 border-white/20 border-t-blue-500 rounded-full animate-spin"></div>
                                <p className="text-gray-400 mt-3">Loading committees...</p>
                            </div>
                        ) : committees.length === 0 ? (
                            <div className="text-center py-10 bg-gray-900 border border-dashed border-white/10 rounded-2xl">
                                <p className="text-gray-400 mb-4">No committees have been created yet.</p>
                                <button
                                    onClick={() => setShowCreateModal(true)}
                                    className="bg-blue-600/10 text-blue-400 border border-blue-500/20 px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-500/20 transition-colors"
                                >
                                    Create First Committee
                                </button>
                            </div>
                        ) : (
                            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                                {Array.isArray(committees) && committees.map((committee) => (
                                    <Link 
                                        href={`/coordinator/committees/${committee.committeeId}`} 
                                        key={committee.committeeId}
                                        className="bg-gray-900 border border-white/10 p-6 rounded-2xl hover:border-blue-500/50 transition-colors group relative overflow-hidden"
                                    >
                                        <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-blue-500 to-purple-500 opacity-0 group-hover:opacity-100 transition-opacity"></div>
                                        <div className="flex justify-between items-start mb-4">
                                            <h3 className="text-lg font-semibold text-white">{committee.committeeName}</h3>
                                            <span className={`px-2 py-1 rounded-md text-xs font-medium border ${
                                                committee.status === 'ACTIVE' 
                                                ? 'bg-green-500/10 text-green-400 border-green-500/20' 
                                                : 'bg-gray-500/10 text-gray-400 border-gray-500/20'
                                            }`}>
                                                {committee.status}
                                            </span>
                                        </div>
                                        <p className="text-sm text-gray-400 line-clamp-2 mb-4 h-10">
                                            {committee.description || "No description provided."}
                                        </p>
                                        <div className="flex justify-between items-center text-xs text-gray-500 border-t border-white/5 pt-4">
                                            <span>{committee.advisors?.length || 0} Members Assigned</span>
                                            <span className="text-blue-400 group-hover:underline">View Details &rarr;</span>
                                        </div>
                                    </Link>
                                ))}
                            </div>
                        )}
                    </div>
                </div>

                {/* Create Modal */}
                {showCreateModal && (
                    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
                        <div className="bg-gray-900 border border-white/10 w-full max-w-md rounded-2xl shadow-2xl p-6">
                            <h2 className="text-xl font-bold text-white mb-6">Create New Committee</h2>
                            <CreateCommitteeForm 
                                onSuccess={handleCreateSuccess} 
                                onCancel={() => setShowCreateModal(false)} 
                            />
                        </div>
                    </div>
                )}
            </main>
        </div>
    );
}
=======
import Sidebar from "@/components/Sidebar";
import CommitteeForm from "@/components/committee/CommitteeForm";
import { showToast } from "@/components/toast/ToastContext";
import {
    createCommittee,
    deleteCommittee,
    fetchCoordinatorCommittees,
    updateCommittee,
} from "@/lib/committees-api";
import { Committee, CommitteeFormValues } from "@/lib/committee-types";
import { getUser } from "@/lib/auth";
import { useEffect, useState } from "react";
import { useRouter, useSearchParams, usePathname } from "next/navigation";

export default function CoordinatorCommitteesPage() {
    const [committees, setCommittees] = useState<Committee[]>([]);
    const [loading, setLoading] = useState(true);
    const [showCreate, setShowCreate] = useState(false);
    const [editingCommittee, setEditingCommittee] = useState<Committee | null>(null);
    const [submitting, setSubmitting] = useState(false);
    const [apiError, setApiError] = useState("");
    const [totalPages, setTotalPages] = useState(1);

    function updateUrlParams(updates: Record<string, string | number>) {
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
    }

    const router = useRouter();
    const searchParams = useSearchParams();
    const pathname = usePathname();

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
            showToast(
                err instanceof Error ? err.message : "Failed to load committees.",
                "error"
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
    }, [searchInput]);

    async function handleCreate(values: CommitteeFormValues) {
        setSubmitting(true);
        setApiError("");

        try {
            await createCommittee(values);
            showToast("Committee created successfully.", "success");
            setShowCreate(false);
            await loadCommittees(1, statusFilter, searchQuery, sortOption, pageSize);
        } catch (err) {
            const message =
                err instanceof Error ? err.message : "Failed to create committee.";
            setApiError(message);
            showToast(message, "error");
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
            showToast("Committee updated successfully.", "success");
            setEditingCommittee(null);
            await loadCommittees(page, statusFilter, searchQuery, sortOption, pageSize);
        } catch (err) {
            const message =
                err instanceof Error ? err.message : "Failed to update committee.";
            setApiError(message);
            showToast(message, "error");
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
            showToast("Committee deleted successfully.", "success");
            await loadCommittees(page, statusFilter, searchQuery, sortOption, pageSize);
        } catch (err) {
            showToast(
                err instanceof Error ? err.message : "Failed to delete committee.",
                "error"
            );
        }
    }

    function handlePageChange(nextPage: number) {
        if (nextPage < 1 || nextPage > totalPages) return;

        setPage(nextPage);
        updateUrlParams({ page: nextPage });
        loadCommittees(nextPage, statusFilter, searchQuery, sortOption, pageSize);
    }

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

                    {showCreate && (
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
                                <option value="FORMING">FORMING</option>
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

                                                <span className="rounded-full border border-blue-500/20 bg-blue-500/10 px-3 py-1 text-xs text-blue-300">
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
                                                    href={`/committees/${committee.committeeId}`}
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
>>>>>>> 534d052 (feat: committee management frontend with filters, search, sort, pagination)
