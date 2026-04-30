"use client";

import Sidebar from "@/components/Sidebar";
import Link from "next/link";
import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { fetchCommitteeById } from "@/lib/committees-api";
import { Committee } from "@/lib/committee-types";
import { showToast } from "@/components/toast/ToastContext";

export default function CommitteeDetailPage() {
    const params = useParams();
    const committeeId = Number(params.committeeId);

    const [committee, setCommittee] = useState<Committee | null>(null);
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
                    <Link href="/committees" className="text-sm text-blue-400 hover:underline">
                        ← Back to committees
                    </Link>

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

                                    <span className="rounded-full border border-blue-500/20 bg-blue-500/10 px-4 py-2 text-sm text-blue-300">
                                        {committee.status}
                                    </span>
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