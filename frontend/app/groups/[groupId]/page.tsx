"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import StatusBadge from "@/components/StatusBadge";
import {
    ApiGroupDetail,
    ApiGroupMember,
    fetchGroupDetail,
} from "@/lib/groups-api";

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

    const [group, setGroup] = useState<ApiGroupDetail | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        let cancelled = false;

        async function loadGroup() {
            try {
                setLoading(true);
                setError("");

                const data = await fetchGroupDetail(groupId);

                if (cancelled) return;
                setGroup(data);
            } catch (err) {
                if (cancelled) return;
                const message =
                    err instanceof Error ? err.message : "Failed to load group details.";
                setError(message);
            } finally {
                if (!cancelled) {
                    setLoading(false);
                }
            }
        }

        if (!Number.isNaN(groupId)) {
            loadGroup();
        }

        return () => {
            cancelled = true;
        };
    }, [groupId]);

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
                    {"<- Back to groups"}
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
                    <Link href={`/groups/${group.id}/committee-grading`}>
                        <div className="cursor-pointer rounded-2xl border border-blue-500/20 bg-blue-500/10 p-6 shadow-lg shadow-blue-950/20 backdrop-blur transition-all hover:border-blue-400/40 hover:bg-blue-500/15">
                            <p className="mb-2 text-sm text-blue-200">Committee Grading</p>
                            <p className="font-medium text-white">Open grading drawer</p>
                            <p className="mt-2 text-sm text-blue-100/70">
                                Review submission details, add comments, and submit the final score.
                            </p>
                        </div>
                    </Link>

                    <div className="rounded-2xl border border-white/10 bg-gray-900/70 p-6 shadow-lg shadow-black/20 backdrop-blur">
                        <p className="text-sm text-gray-400 mb-2">GitHub</p>
                        <p className="text-gray-500 font-medium">Unknown</p>
                    </div>

                    <div className="rounded-2xl border border-white/10 bg-gray-900/70 p-6 shadow-lg shadow-black/20 backdrop-blur">
                        <p className="text-sm text-gray-400 mb-2">JIRA</p>
                        <p className="text-gray-500 font-medium">Unknown</p>
                    </div>

                    <div className="rounded-2xl border border-white/10 bg-gray-900/70 p-6 shadow-lg shadow-black/20 backdrop-blur">
                        <p className="text-sm text-gray-400 mb-2">Created At</p>
                        <p className="text-white font-medium">{formatDate(group.createdAt)}</p>
                    </div>

                    <div className="rounded-2xl border border-white/10 bg-gray-900/70 p-6 shadow-lg shadow-black/20 backdrop-blur">
                        <p className="text-sm text-gray-400 mb-2">Updated At</p>
                        <p className="text-white font-medium">{formatDate(group.updatedAt)}</p>
                    </div>
                </div>

                <div className="rounded-2xl border border-white/10 bg-gray-900/70 p-7 shadow-lg shadow-black/20 backdrop-blur">
                    <h2 className="text-2xl font-semibold mb-5">Members</h2>

                    <div className="space-y-4">
                        {group.members?.map((member: ApiGroupMember) => (
                            <div
                                key={member.userId}
                                className="flex items-center justify-between rounded-xl bg-white/5 px-5 py-4"
                            >
                                <div>
                                    <p className="text-lg font-medium">{member.fullName}</p>
                                    <p className="text-sm text-gray-400 mt-1">{member.userId}</p>
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
                    </div>
                </div>
            </div>
        </main>
    );
}
