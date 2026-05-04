"use client";

import Sidebar from "@/components/Sidebar";
import Link from "next/link";
import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { deleteCommittee, fetchCommitteeById } from "@/lib/committees-api";
import { getUser } from "@/lib/auth";
import { CommitteeDetail } from "@/lib/committee-types";
import { showToast } from "@/components/toast/ToastContext";
import CommitteeAssignmentManager from "@/components/committees/CommitteeAssignmentManager";

export default function CommitteeDetailPage() {
    const params = useParams();
    const committeeId = Number(params.committeeId);

    const currentUser = getUser();
    const isCoordinator = currentUser?.role === "coordinator";

    const [committee, setCommittee] = useState<CommitteeDetail | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        let cancelled = false;

        async function loadCommittee() {
            try {
                setLoading(true);
                const data = await fetchCommitteeById(committeeId);

                if (!cancelled) {
                    setCommittee(data);
                }
            } catch (err) {
                showToast(
                    err instanceof Error ? err.message : "Failed to load committee.",
                    "error"
                );
            } finally {
                if (!cancelled) {
                    setLoading(false);
                }
            }
        }

        if (!Number.isNaN(committeeId)) {
            loadCommittee();
        }

        return () => {
            cancelled = true;
        };
    }, [committeeId]);

    return (
        <div className="min-h-screen bg-gray-950 flex">
            <Sidebar activePage="committees" />

            <main className="flex-1 min-w-0 px-6 py-10 text-white">
                <div className="mx-auto max-w-5xl space-y-8">
                    <div className="flex items-center gap-3 text-sm text-gray-400">
                        
                    <Link 
                        href={isCoordinator ? "/coordinator/committees" : "/committees"} 
                        className="text-blue-400 hover:text-blue-300 transition-colors"
                        >
                         ← Back to committees
                    </Link>

                        <span className="text-gray-600">/</span>

                        <span className="text-white">{committee?.committeeName}</span>
                    </div>

                    {loading ? (
                        <div className="rounded-2xl border border-white/10 bg-gray-900/70 p-8">
                            <div className="h-8 w-64 animate-pulse rounded bg-white/10" />
                            <div className="mt-4 h-4 w-96 animate-pulse rounded bg-white/10" />
                            <div className="mt-8 grid grid-cols-1 gap-4 md:grid-cols-3">
                                {Array.from({ length: 3 }).map((_, index) => (
                                    <div
                                        key={index}
                                        className="h-24 animate-pulse rounded-xl bg-white/5"
                                    />
                                ))}
                            </div>
                        </div>
                    ) : !committee ? (
                        <div className="rounded-2xl border border-red-500/20 bg-red-500/10 p-8 text-red-200">
                            Committee not found.
                        </div>
                    ) : (
                        <>
                            <div className="rounded-2xl border border-white/10 bg-gray-900/70 p-8">
                                <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                                    <div>
                                        <h1 className="text-3xl font-bold">
                                            {committee.committeeName}
                                        </h1>
                                        <p className="mt-3 max-w-2xl text-gray-400">
                                            {committee.description || "No description provided."}
                                        </p>
                                    </div>

                                    <div className="flex items-center gap-2">
                                        <span className="rounded-full border border-blue-500/20 bg-blue-500/10 px-4 py-2 text-sm text-blue-300">
                                            {committee.status}
                                        </span>

                                        {isCoordinator && (
                                            <>
                                                <Link
                                                    href={`/coordinator/committees?edit=${committee.committeeId}`}
                                                    className="px-3 py-1 text-sm bg-white/10 rounded hover:bg-white/20"
                                                >
                                                    Edit
                                                </Link>

                                                <button
                                                    type="button"
                                                    onClick={async () => {
                                                        const ok = window.confirm("Delete this committee?");
                                                        if (!ok) return;

                                                        try {
                                                            await deleteCommittee(committee.committeeId);
                                                            showToast("Committee deleted successfully.", "success");
                                                            window.location.href = "/coordinator/committees";
                                                        } catch (err) {
                                                            showToast(
                                                                err instanceof Error ? err.message : "Failed to delete committee.",
                                                                "error"
                                                            );
                                                        }
                                                    }}
                                                    className="px-3 py-1 text-sm bg-red-600 rounded hover:bg-red-500"
                                                >
                                                    Delete
                                                </button>
                                            </>
                                        )}
                                    </div>
                                </div>

                                <div className="mt-8 grid grid-cols-1 gap-4 md:grid-cols-3">
                                    <div className="rounded-xl border border-white/10 bg-white/5 p-5">
                                        <p className="text-sm text-gray-400">Advisor Count</p>
                                        <p className="mt-2 text-2xl font-bold">
                                            {committee.advisorCount ?? 0}
                                        </p>
                                    </div>

                                    <div className="rounded-xl border border-white/10 bg-white/5 p-5">
                                        <p className="text-sm text-gray-400">Jury Count</p>
                                        <p className="mt-2 text-2xl font-bold">
                                            {committee.juryCount ?? 0}
                                        </p>
                                    </div>

                                    <div className="rounded-xl border border-white/10 bg-white/5 p-5">
                                        <p className="text-sm text-gray-400">Group Count</p>
                                        <p className="mt-2 text-2xl font-bold">
                                            {committee.groupCount ?? 0}
                                        </p>
                                    </div>
                                </div>
                            </div>

                            <div className="rounded-2xl border border-white/10 bg-gray-900/70 p-6">
                                <h2 className="text-xl font-semibold">Committee Details</h2>
                                <div className="mt-5 grid grid-cols-1 gap-4 md:grid-cols-2">
                                    <Info label="Committee ID" value={String(committee.committeeId)} />
                                    <Info label="Created By" value={`User #${committee.createdBy}`} />
                                    <Info
                                        label="Created At"
                                        value={
                                            committee.createdAt
                                                ? new Date(committee.createdAt).toLocaleString()
                                                : "-"
                                        }
                                    />
                                    <Info
                                        label="Updated At"
                                        value={
                                            committee.updatedAt
                                                ? new Date(committee.updatedAt).toLocaleString()
                                                : "-"
                                        }
                                    />
                                </div>
                            </div>

                            <div className="mt-2">
                                <CommitteeAssignmentManager committeeId={committeeId} />
                            </div>

                            <div className="rounded-2xl border border-white/10 bg-gray-900/70 p-6">
                                <h2 className="text-xl font-semibold">Assigned Groups</h2>
                                <div className="mt-4 space-y-3">
                                    {committee.groups.length === 0 ? (
                                        <p className="text-gray-400 text-sm">No groups assigned.</p>
                                    ) : (
                                        committee.groups.map((group) => (
                                            <div
                                                key={group.groupId}
                                                className="flex justify-between items-center border border-white/10 rounded-lg p-3 bg-white/5"
                                            >
                                                <div>
                                                    <p className="font-medium">{group.groupName}</p>
                                                    <p className="text-sm text-gray-400">
                                                        Members: {group.membersCount}
                                                    </p>
                                                </div>
                                                <div className="text-right text-sm text-gray-400">
                                                    <p>{group.status}</p>
                                                    <p>
                                                        {group.examDate
                                                            ? new Date(group.examDate).toLocaleString()
                                                            : "No exam date"}
                                                    </p>
                                                </div>
                                            </div>
                                        ))
                                    )}
                                </div>
                            </div>
                            <div className="rounded-2xl border border-white/10 bg-gray-900/70 p-6">
                                <h2 className="text-xl font-semibold">Recent Activity</h2>

                                <div className="mt-4 space-y-3">
                                    {committee.recentAuditLogs.length === 0 ? (
                                        <p className="text-gray-400 text-sm">No activity yet.</p>
                                    ) : (
                                        committee.recentAuditLogs.map((log) => (
                                            <div
                                                key={log.id}
                                                className="border border-white/10 rounded-lg p-3 bg-white/5"
                                            >
                                                <p className="text-sm">
                                                    <span className="font-medium">{log.userName}</span>{" "}
                                                    {log.description}
                                                </p>

                                                <p className="text-xs text-gray-400 mt-1">
                                                    {new Date(log.timestamp).toLocaleString()}
                                                </p>
                                            </div>
                                        ))
                                    )}
                                </div>
                            </div>
                        </>
                    )}
                </div>
            </main>
        </div>
    );
}

function Info({ label, value }: { label: string; value: string }) {
    return (
        <div className="rounded-xl border border-white/10 bg-white/5 p-4">
            <p className="text-sm text-gray-500">{label}</p>
            <p className="mt-1 text-sm font-medium text-white">{value}</p>
        </div>


    );
}