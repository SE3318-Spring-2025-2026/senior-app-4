"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { getToken, getUser } from "@/lib/auth";
import Sidebar from "@/components/Sidebar";
import { fetchCommittees, fetchCommitteeById } from "@/lib/committees-api";

interface CommitteeGroup {
    committeeId: number;
    committeeName: string;
    groupId: number;
    groupName: string;
    status: string;
}

export default function CommitteeGroupsPage() {
    const router = useRouter();
    const [role, setRole] = useState<string | null>(null);

    useEffect(() => {
        const token = getToken();
        const user = getUser();

        if (!token || !user) {
            router.replace("/auth/login");
            return;
        }

        if (user.requiresPasswordChange) {
            router.replace("/auth/change-password");
            return;
        }

        if (user.role !== "professor") {
            queueMicrotask(() => setRole("denied"));
            return;
        }

        queueMicrotask(() => setRole(user.role));
    }, [router]);

    if (role === null) return <Spinner />;
    if (role === "denied") return <AccessDenied />;
    return <CommitteeGroupsLayout />;
}

function Spinner() {
    return (
        <div className="min-h-screen bg-gray-950 flex items-center justify-center">
            <svg className="w-6 h-6 animate-spin text-blue-500" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
            </svg>
        </div>
    );
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
                <p className="text-sm text-gray-500">Only Professors can access this page.</p>
            </div>
        </div>
    );
}

function CommitteeGroupsLayout() {
    return (
        <div className="min-h-screen bg-gray-950 flex">
            <Sidebar activePage="committee-groups" />
            <main className="flex-1 flex flex-col min-w-0">
                <div className="border-b border-white/5 px-8 py-4 flex items-center justify-between">
                    <div>
                        <h1 className="text-base font-semibold text-white">Committee Groups</h1>
                        <p className="text-xs text-gray-500 mt-0.5">Groups assigned to the committees you belong to</p>
                    </div>
                    <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-white/5 border border-white/10">
                        <div className="w-1.5 h-1.5 rounded-full bg-green-400 animate-pulse" />
                        <span className="text-xs text-gray-400">System Online</span>
                    </div>
                </div>
                <div className="flex-1 p-8">
                    <CommitteeGroupsTable />
                </div>
            </main>
        </div>
    );
}

function CommitteeGroupsTable() {
    const router = useRouter();
    const [groups, setGroups] = useState<CommitteeGroup[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const loadGroups = useCallback(async () => {
        setLoading(true);
        setError(null);

        try {
            const user = getUser();
            if (!user?.userId) throw new Error("No user found");
            const currentUserId = Number(user.userId);

            // Fetch all committees and filter by my committees
            const response = await fetchCommittees(0, 100);
            const myCommittees = response.content.filter((c) => {
                const inAdvisors = c.advisors?.some((a) => a.userId === currentUserId) ?? false;
                const inJury = c.jury?.some((j) => j.userId === currentUserId) ?? false;
                return inAdvisors || inJury;
            });

            // Fetch details for each committee to get the assigned groups
            const groupList: CommitteeGroup[] = [];
            for (const c of myCommittees) {
                const details = await fetchCommitteeById(c.committeeId);
                for (const g of (details.groups || [])) {
                    groupList.push({
                        committeeId: c.committeeId,
                        committeeName: c.committeeName,
                        groupId: g.groupId,
                        groupName: g.groupName,
                        status: g.status,
                    });
                }
            }

            setGroups(groupList);
        } catch (err: unknown) {
            const message = err instanceof Error ? err.message : "Failed to load committee groups.";
            setError(message);
            toast.error(message);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        loadGroups();
    }, [loadGroups]);

    if (loading) {
        return (
            <div className="bg-gray-900 border border-white/8 rounded-2xl p-12 flex items-center justify-center">
                <svg className="w-5 h-5 animate-spin text-blue-500" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                </svg>
            </div>
        );
    }

    if (error) {
        return (
            <div className="bg-gray-900 border border-red-500/20 rounded-2xl p-8 flex items-start justify-between gap-6">
                <div className="flex items-start gap-4">
                    <div className="w-10 h-10 rounded-xl bg-red-500/10 border border-red-500/20 flex items-center justify-center shrink-0">
                        <svg className="w-5 h-5 text-red-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 9v3.75m0 3.75h.008v.008H12v-.008zm8.25-3.75A8.25 8.25 0 113.75 12a8.25 8.25 0 0116.5 0z" />
                        </svg>
                    </div>
                    <div>
                        <p className="text-sm font-semibold text-white">Committee groups could not be loaded</p>
                        <p className="text-sm text-gray-400 mt-1">{error}</p>
                    </div>
                </div>
                <button
                    onClick={loadGroups}
                    className="px-4 py-2 rounded-lg text-sm font-medium bg-white/5 border border-white/10 text-gray-300 hover:text-white hover:bg-white/10 transition-colors"
                >
                    Retry
                </button>
            </div>
        );
    }

    if (groups.length === 0) {
        return (
            <div className="bg-gray-900 border border-white/8 rounded-2xl p-12 flex flex-col items-center justify-center gap-3">
                <div className="w-14 h-14 rounded-2xl bg-gray-800 border border-white/5 flex items-center justify-center">
                    <svg className="w-6 h-6 text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                    </svg>
                </div>
                <p className="text-sm font-medium text-white">No committee groups</p>
                <p className="text-xs text-gray-500">Groups assigned to your committees will appear here.</p>
            </div>
        );
    }

    return (
        <div className="bg-gray-900 border border-white/8 rounded-2xl overflow-hidden">
            <div className="px-6 py-4 border-b border-white/5 flex items-center justify-between gap-4">
                <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-lg bg-blue-500/10 border border-blue-500/20 flex items-center justify-center">
                        <svg className="w-4 h-4 text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                        </svg>
                    </div>
                    <div>
                        <p className="text-sm font-semibold text-white">Committee Groups</p>
                        <p className="text-xs text-gray-500">
                            {groups.length} group{groups.length !== 1 ? "s" : ""} in your committees
                        </p>
                    </div>
                </div>
                <button
                    onClick={loadGroups}
                    className="px-3 py-1.5 rounded-lg text-xs font-medium bg-white/5 border border-white/10 text-gray-300 hover:text-white hover:bg-white/10 transition-colors"
                >
                    Refresh
                </button>
            </div>

            <div className="overflow-x-auto">
                <table className="w-full">
                    <thead>
                        <tr className="border-b border-white/5">
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Group</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Committee</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                            <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-white/5">
                        {groups.map((group, index) => (
                            <tr key={`${group.committeeId}-${group.groupId}-${index}`} className="hover:bg-white/[0.02] transition-colors">
                                <td className="px-6 py-4">
                                    <button
                                        onClick={() => router.push(`/professor/submissions?groupId=${group.groupId}`)}
                                        className="flex items-center gap-3 text-left hover:opacity-80 transition-opacity"
                                    >
                                        <div className="w-8 h-8 rounded-lg bg-blue-600/10 border border-blue-500/20 flex items-center justify-center shrink-0">
                                            <span className="text-xs font-bold text-blue-400 uppercase">
                                                {group.groupName ? group.groupName.slice(0, 2) : "G"}
                                            </span>
                                        </div>
                                        <div className="min-w-0">
                                            <p className="text-sm font-medium text-white truncate hover:underline">{group.groupName}</p>
                                            <p className="text-xs text-gray-600">Group #{group.groupId}</p>
                                        </div>
                                    </button>
                                </td>
                                <td className="px-6 py-4 text-sm text-gray-300">
                                    {group.committeeName}
                                </td>
                                <td className="px-6 py-4">
                                    <span className="text-xs font-medium px-2 py-0.5 rounded-full border text-gray-400 bg-white/5 border-white/10">
                                        {group.status}
                                    </span>
                                </td>
                                <td className="px-6 py-4 text-right">
                                    <button
                                        onClick={() => router.push(`/professor/submissions?groupId=${group.groupId}`)}
                                        className="inline-flex items-center gap-1.5 rounded-lg border border-white/10 px-3 py-1.5 text-xs font-medium text-gray-300 transition hover:bg-white/5 hover:text-white"
                                    >
                                        View Submissions
                                    </button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
}
