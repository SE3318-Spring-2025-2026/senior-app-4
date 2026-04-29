"use client";

import { useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import GithubBindForm from "@/components/GithubBindForm";
import GithubStatusCard from "@/components/GithubStatusCard";
import IntegrationTestCard from "@/components/IntegrationTestCard";
import ConfirmDialog from "@/components/ConfirmDialog";
import AppTopbar from "@/components/AppTopbar";
import {
    mockGithubIntegrations,
    mockIntegrationTests,
} from "@/lib/mock-github";
import { mockGroups } from "@/lib/mock-groups";
import { useNotifications } from "@/components/NotificationProvider";

export default function GithubIntegrationPage() {
    const params = useParams();
    const groupId = Number(params.groupId);

    const { unreadOrPendingCount } = useNotifications();

    const group = mockGroups.find((g) => g.groupId === groupId);
    const integration = mockGithubIntegrations.find((i) => i.groupId === groupId);

    const [showConfirm, setShowConfirm] = useState(false);
    const [unbindLoading, setUnbindLoading] = useState(false);
    const [statusMessage, setStatusMessage] = useState("");

    if (!group) {
        return (
            <main className="min-h-screen bg-gray-950 flex items-center justify-center text-white">
                Group not found
            </main>
        );
    }

    const isLeader = group.members.some(
        (member) => member.role === "leader" && member.fullName === "Miray Yıldırım"
    );

    async function handleBind(githubPat: string, organizationName: string) {
        console.log("Bind request:", { githubPat, organizationName });

        await new Promise((resolve) => setTimeout(resolve, 1000));

        setStatusMessage(`GitHub organization "${organizationName}" bound successfully.`);
    }

    async function handleUnbind() {
        setUnbindLoading(true);

        try {
            await new Promise((resolve) => setTimeout(resolve, 1000));
            setStatusMessage("GitHub organization unbound successfully.");
            setShowConfirm(false);
        } finally {
            setUnbindLoading(false);
        }
    }

    return (
        <>
            <main className="min-h-screen bg-gray-950 px-6 py-10 text-white">
                <div className="mx-auto max-w-4xl space-y-8">
                    <AppTopbar
                        title="GitHub Integration"
                        notificationCount={unreadOrPendingCount}
                    />

                    <Link
                        href={`/groups/${groupId}`}
                        className="text-sm text-blue-400 hover:underline"
                    >
                        ← Back to group
                    </Link>

                    <div>
                        <h1 className="text-3xl font-bold">GitHub Integration</h1>
                        <p className="mt-2 text-gray-400">
                            Connect your group with a GitHub organization.
                        </p>
                    </div>

                    {statusMessage && (
                        <div className="rounded-2xl border border-green-500/20 bg-green-500/10 px-4 py-3 text-sm text-green-300">
                            {statusMessage}
                        </div>
                    )}

                    <GithubStatusCard integration={integration} />

                    {isLeader ? (
                        <>
                            <GithubBindForm onBind={handleBind} />

                            {integration && (
                                <div className="rounded-2xl border border-white/10 bg-gray-900/70 p-6 shadow-lg shadow-black/20 backdrop-blur">
                                    <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                                        <div>
                                            <h3 className="text-lg font-semibold text-white">
                                                Unbind GitHub Organization
                                            </h3>
                                            <p className="mt-1 text-sm text-gray-400">
                                                Remove the current GitHub organization binding from this group.
                                            </p>
                                        </div>

                                        <button
                                            onClick={() => setShowConfirm(true)}
                                            className="rounded-xl bg-red-600 px-4 py-3 text-sm font-medium text-white transition hover:bg-red-500"
                                        >
                                            Unbind
                                        </button>
                                    </div>
                                </div>
                            )}

                            <IntegrationTestCard
                                groupId={groupId}
                                mockResult={mockIntegrationTests[groupId]}
                            />
                        </>
                    ) : (
                        <div className="rounded-2xl border border-white/10 bg-gray-900/70 p-6 text-sm text-gray-400 shadow-lg shadow-black/20 backdrop-blur">
                            Only the group leader can manage GitHub integration settings.
                        </div>
                    )}
                </div>
            </main>

            <ConfirmDialog
                open={showConfirm}
                title="Unbind GitHub organization?"
                message="This will remove the current GitHub organization binding from your group. Are you sure you want to continue?"
                confirmText="Yes, unbind"
                cancelText="Cancel"
                loading={unbindLoading}
                onConfirm={handleUnbind}
                onCancel={() => setShowConfirm(false)}
            />
        </>
    );
}