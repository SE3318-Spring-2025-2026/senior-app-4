"use client";

import { useParams } from "next/navigation";
import Link from "next/link";
import { mockGroups } from "@/lib/mock-groups";
import StatusBadge from "@/components/StatusBadge";
import AppTopbar from "@/components/AppTopbar";
import { useNotifications } from "@/components/NotificationProvider";

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

    const { unreadOrPendingCount } = useNotifications();

    const group = mockGroups.find((g) => g.groupId === groupId);

    if (!group) {
        return (
            <main className="min-h-screen flex items-center justify-center bg-gray-950 text-white">
                Group not found
            </main>
        );
    }

    return (
        <main className="min-h-screen bg-gray-950 px-6 py-10 text-white">
            <div className="mx-auto max-w-5xl">
                <AppTopbar title="Group Details" notificationCount={unreadOrPendingCount} />

                <Link href="/groups" className="text-sm text-blue-400 hover:underline">
                    ← Back to groups
                </Link>

                <div className="mb-6 mt-6 flex items-start justify-between gap-4">
                    <div>
                        <h1 className="text-4xl font-bold tracking-tight">{group.groupName}</h1>
                        <p className="mt-2 text-lg text-gray-400">
                            Advisor: {group.advisorName || "Not Assigned"}
                        </p>
                    </div>

                    <StatusBadge status={group.status} />
                </div>

                <div className="mb-8 grid gap-6 md:grid-cols-2">
                    <Link href={`/groups/${group.groupId}/integrations/github`}>
                        <div className="cursor-pointer rounded-2xl border border-white/10 bg-gray-900/70 p-6 shadow-lg shadow-black/20 backdrop-blur transition-all hover:border-blue-500/40 hover:shadow-blue-500/10">
                            <p className="mb-2 text-sm text-gray-400">GitHub</p>
                            <p
                                className={
                                    group.githubBound
                                        ? "font-medium text-green-400"
                                        : "font-medium text-gray-500"
                                }
                            >
                                {group.githubBound ? "Bound ✔" : "Not Connected ✖"}
                            </p>
                        </div>
                    </Link>

                    <Link href={`/groups/${group.groupId}/integrations/jira`}>
                        <div className="cursor-pointer rounded-2xl border border-white/10 bg-gray-900/70 p-6 shadow-lg shadow-black/20 backdrop-blur transition-all hover:border-blue-500/40 hover:shadow-blue-500/10">
                            <p className="mb-2 text-sm text-gray-400">JIRA</p>
                            <p
                                className={
                                    group.jiraBound
                                        ? "font-medium text-green-400"
                                        : "font-medium text-gray-500"
                                }
                            >
                                {group.jiraBound ? "Bound ✔" : "Not Connected ✖"}
                            </p>
                        </div>
                    </Link>

                    <div className="rounded-2xl border border-white/10 bg-gray-900/70 p-6 shadow-lg shadow-black/20 backdrop-blur">
                        <p className="mb-2 text-sm text-gray-400">Created At</p>
                        <p className="font-medium text-white">{formatDate(group.createdAt)}</p>
                    </div>

                    <div className="rounded-2xl border border-white/10 bg-gray-900/70 p-6 shadow-lg shadow-black/20 backdrop-blur">
                        <p className="mb-2 text-sm text-gray-400">Updated At</p>
                        <p className="font-medium text-white">{formatDate(group.updatedAt)}</p>
                    </div>
                </div>

                <div className="rounded-2xl border border-white/10 bg-gray-900/70 p-7 shadow-lg shadow-black/20 backdrop-blur">
                    <h2 className="mb-5 text-2xl font-semibold">Members</h2>

                    <div className="space-y-4">
                        {group.members.map((member) => (
                            <div
                                key={member.studentId}
                                className="flex items-center justify-between rounded-xl bg-white/5 px-5 py-4"
                            >
                                <div>
                                    <p className="text-lg font-medium">{member.fullName}</p>
                                    <p className="mt-1 text-sm text-gray-400">{member.studentId}</p>
                                </div>

                                <span
                                    className={[
                                        "rounded-full px-3 py-1 text-xs font-medium capitalize",
                                        member.role === "leader"
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