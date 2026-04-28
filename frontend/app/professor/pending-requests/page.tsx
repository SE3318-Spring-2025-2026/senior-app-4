"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { getToken, getUser } from "@/lib/auth";
import Sidebar from "@/components/Sidebar";
import {
    fetchPendingRequests,
    submitAdvisorDecision,
    type AdvisorRequestSummary,
} from "@/lib/advisor-requests-api";

export default function PendingRequestsPage() {
    const router = useRouter();
    const [role, setRole] = useState<string | null>(null);

    useEffect(() => {
        const token = getToken();
        const user = getUser();
        if (!token || !user) { router.replace("/auth/login"); return; }
        if (user.requiresPasswordChange) { router.replace("/auth/change-password"); return; }
        if (user.role !== "professor") { setRole("denied"); return; }
        setRole(user.role);
    }, [router]);

    if (role === null) return <Spinner />;
    if (role === "denied") return <AccessDenied />;
    return <PendingRequestsLayout />;
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

function PendingRequestsLayout() {
    return (
        <div className="min-h-screen bg-gray-950 flex">
            <Sidebar activePage="pending-requests" />
            <main className="flex-1 flex flex-col min-w-0">
                <div className="border-b border-white/5 px-8 py-4 flex items-center justify-between">
                    <div>
                        <h1 className="text-base font-semibold text-white">Pending Requests</h1>
                        <p className="text-xs text-gray-500 mt-0.5">Review and respond to advisor requests from student groups</p>
                    </div>
                    <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-white/5 border border-white/10">
                        <div className="w-1.5 h-1.5 rounded-full bg-green-400 animate-pulse" />
                        <span className="text-xs text-gray-400">Live</span>
                    </div>
                </div>
                <div className="flex-1 p-8">
                    <PendingRequestsTable />
                </div>
            </main>
        </div>
    );
}

function PendingRequestsTable() {
    const [requests, setRequests] = useState<AdvisorRequestSummary[]>([]);
    const [loading, setLoading] = useState(true);
    const [rejectingId, setRejectingId] = useState<number | null>(null);
    const [rejectReason, setRejectReason] = useState("");
    const [processingId, setProcessingId] = useState<number | null>(null);

    useEffect(() => {
        (async () => {
            try {
                const data = await fetchPendingRequests();
                setRequests(data);
            } catch (err: unknown) {
                toast.error(err instanceof Error ? err.message : "Failed to load requests.");
            } finally {
                setLoading(false);
            }
        })();
    }, []);

    const handleApprove = async (requestId: number) => {
        setProcessingId(requestId);
        try {
            await submitAdvisorDecision(requestId, { decision: "APPROVE" });
            toast.success("Request approved. The group has been notified.");
            setRequests((prev) => prev.filter((r) => r.id !== requestId));
        } catch (err: unknown) {
            toast.error(err instanceof Error ? err.message : "Failed to approve request.");
        } finally {
            setProcessingId(null);
        }
    };

    const handleRejectConfirm = async (requestId: number) => {
        setProcessingId(requestId);
        try {
            await submitAdvisorDecision(requestId, {
                decision: "REJECT",
                reason: rejectReason.trim() || undefined,
            });
            toast.success("Request rejected. The group has been notified.");
            setRequests((prev) => prev.filter((r) => r.id !== requestId));
            setRejectingId(null);
            setRejectReason("");
        } catch (err: unknown) {
            toast.error(err instanceof Error ? err.message : "Failed to reject request.");
        } finally {
            setProcessingId(null);
        }
    };

    const openReject = (requestId: number) => {
        setRejectingId(requestId);
        setRejectReason("");
    };

    const cancelReject = () => {
        setRejectingId(null);
        setRejectReason("");
    };

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

    if (requests.length === 0) {
        return (
            <div className="bg-gray-900 border border-white/8 rounded-2xl p-12 flex flex-col items-center justify-center gap-3">
                <div className="w-14 h-14 rounded-2xl bg-gray-800 border border-white/5 flex items-center justify-center">
                    <svg className="w-6 h-6 text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12h3.75M9 15h3.75M9 18h3.75m3 .75H18a2.25 2.25 0 002.25-2.25V6.108c0-1.135-.845-2.098-1.976-2.192a48.424 48.424 0 00-1.123-.08m-5.801 0c-.065.21-.1.433-.1.664 0 .414.336.75.75.75h4.5a.75.75 0 00.75-.75 2.25 2.25 0 00-.1-.664m-5.8 0A2.251 2.251 0 0113.5 2.25H15c1.012 0 1.867.668 2.15 1.586m-5.8 0c-.376.023-.75.05-1.124.08M6.75 12h.008v.008H6.75V12zm0 3h.008v.008H6.75V15zm0 3h.008v.008H6.75V18z" />
                    </svg>
                </div>
                <p className="text-sm font-medium text-white">No pending requests</p>
                <p className="text-xs text-gray-500">Student groups that request you as their advisor will appear here.</p>
            </div>
        );
    }

    return (
        <div className="bg-gray-900 border border-white/8 rounded-2xl overflow-hidden">
            <div className="px-6 py-4 border-b border-white/5 flex items-center justify-between">
                <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-lg bg-yellow-500/10 border border-yellow-500/20 flex items-center justify-center">
                        <svg className="w-4 h-4 text-yellow-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 6v6h4.5m4.5 0a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                    </div>
                    <div>
                        <p className="text-sm font-semibold text-white">Pending Advisor Requests</p>
                        <p className="text-xs text-gray-500">{requests.length} request{requests.length !== 1 ? "s" : ""} awaiting your decision</p>
                    </div>
                </div>
                <span className="text-xs font-medium text-yellow-400 bg-yellow-400/10 border border-yellow-400/20 px-2.5 py-1 rounded-full">
                    {requests.length} Pending
                </span>
            </div>

            <div className="overflow-x-auto">
                <table className="w-full">
                    <thead>
                        <tr className="border-b border-white/5">
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Team</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Members</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Message</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Requested</th>
                            <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-white/5">
                        {requests.map((req) => (
                            <RequestRow
                                key={req.id}
                                request={req}
                                isRejecting={rejectingId === req.id}
                                rejectReason={rejectReason}
                                isProcessing={processingId === req.id}
                                onApprove={() => handleApprove(req.id)}
                                onRejectOpen={() => openReject(req.id)}
                                onRejectCancel={cancelReject}
                                onRejectConfirm={() => handleRejectConfirm(req.id)}
                                onRejectReasonChange={setRejectReason}
                            />
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
}

type RequestRowProps = {
    request: AdvisorRequestSummary;
    isRejecting: boolean;
    rejectReason: string;
    isProcessing: boolean;
    onApprove: () => void;
    onRejectOpen: () => void;
    onRejectCancel: () => void;
    onRejectConfirm: () => void;
    onRejectReasonChange: (v: string) => void;
};

function RequestRow({
    request,
    isRejecting,
    rejectReason,
    isProcessing,
    onApprove,
    onRejectOpen,
    onRejectCancel,
    onRejectConfirm,
    onRejectReasonChange,
}: RequestRowProps) {
    return (
        <>
            <tr className={`transition-colors ${isRejecting ? "bg-red-500/5" : "hover:bg-white/[0.02]"}`}>
                <td className="px-6 py-4">
                    <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-lg bg-blue-600/10 border border-blue-500/20 flex items-center justify-center shrink-0">
                            <span className="text-xs font-bold text-blue-400 uppercase">
                                {request.teamName.slice(0, 2)}
                            </span>
                        </div>
                        <p className="text-sm font-medium text-white">{request.teamName}</p>
                    </div>
                </td>
                <td className="px-6 py-4">
                    <GroupMembers teamId={request.teamId} />
                </td>
                <td className="px-6 py-4 max-w-xs">
                    {request.message ? (
                        <p className="text-sm text-gray-400 truncate" title={request.message}>
                            {request.message}
                        </p>
                    ) : (
                        <span className="text-xs text-gray-600 italic">No message</span>
                    )}
                </td>
                <td className="px-6 py-4">
                    <span className="text-sm text-gray-400">{formatRelativeDate(request.requestedAt)}</span>
                </td>
                <td className="px-6 py-4">
                    {!isRejecting ? (
                        <div className="flex items-center justify-end gap-2">
                            <button
                                onClick={onApprove}
                                disabled={isProcessing}
                                className="flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg text-xs font-medium
                                           bg-green-600/10 border border-green-500/30 text-green-400
                                           hover:bg-green-600/20 hover:border-green-500/50 active:scale-95
                                           transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                {isProcessing ? (
                                    <svg className="w-3.5 h-3.5 animate-spin" fill="none" viewBox="0 0 24 24">
                                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                                    </svg>
                                ) : (
                                    <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                                    </svg>
                                )}
                                Approve
                            </button>
                            <button
                                onClick={onRejectOpen}
                                disabled={isProcessing}
                                className="flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg text-xs font-medium
                                           bg-red-600/10 border border-red-500/30 text-red-400
                                           hover:bg-red-600/20 hover:border-red-500/50 active:scale-95
                                           transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                </svg>
                                Reject
                            </button>
                        </div>
                    ) : (
                        <div className="flex items-center justify-end gap-2">
                            <button
                                onClick={onRejectCancel}
                                disabled={isProcessing}
                                className="px-3 py-1.5 rounded-lg text-xs font-medium text-gray-400
                                           hover:text-white hover:bg-white/5 transition-colors
                                           disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                Cancel
                            </button>
                        </div>
                    )}
                </td>
            </tr>

            {isRejecting && (
                <tr className="bg-red-500/5 border-b border-white/5">
                    <td colSpan={5} className="px-6 py-4">
                        <div className="flex items-start gap-4">
                            <div className="flex-1 space-y-2">
                                <label className="text-xs font-medium text-gray-400 uppercase tracking-wider">
                                    Rejection Reason
                                    <span className="ml-1 text-gray-600 normal-case font-normal">(optional)</span>
                                </label>
                                <textarea
                                    value={rejectReason}
                                    onChange={(e) => onRejectReasonChange(e.target.value)}
                                    placeholder="Explain why you are rejecting this request..."
                                    rows={2}
                                    className="w-full px-4 py-3 bg-gray-800 border border-red-500/20 rounded-xl text-sm text-white
                                               placeholder:text-gray-600 focus:outline-none focus:border-red-500/50
                                               focus:ring-1 focus:ring-red-500/20 transition-all resize-none"
                                    autoFocus
                                />
                            </div>
                            <div className="pt-6 shrink-0">
                                <button
                                    onClick={onRejectConfirm}
                                    disabled={isProcessing}
                                    className="flex items-center gap-1.5 px-4 py-2 rounded-lg text-sm font-medium
                                               bg-red-600 text-white hover:bg-red-500 active:scale-95
                                               transition-all shadow-lg shadow-red-600/20
                                               disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none"
                                >
                                    {isProcessing ? (
                                        <>
                                            <svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                                                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                                                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                                            </svg>
                                            Rejecting...
                                        </>
                                    ) : (
                                        <>
                                            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                            </svg>
                                            Confirm Reject
                                        </>
                                    )}
                                </button>
                            </div>
                        </div>
                    </td>
                </tr>
            )}
        </>
    );
}

function GroupMembers({ teamId }: { teamId: number }) {
    const [members, setMembers] = useState<{ fullName: string; role: string }[] | null>(null);

    useEffect(() => {
        const token = getToken();
        const API_BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api/v1";
        (async () => {
            try {
                const res = await fetch(`${API_BASE}/groups/${teamId}`, {
                    headers: { Authorization: `Bearer ${token ?? ""}` },
                    cache: "no-store",
                });
                if (!res.ok) return;
                const data = await res.json();
                setMembers(data.members ?? []);
            } catch {
                // silently fail — not critical
            }
        })();
    }, [teamId]);

    if (members === null) {
        return <span className="text-xs text-gray-600">Loading...</span>;
    }

    const leader = members.find((m) => m.role === "LEADER" || m.role === "leader");
    const count = members.length;

    return (
        <div className="space-y-1">
            <p className="text-sm text-white">{count} member{count !== 1 ? "s" : ""}</p>
            {leader && (
                <p className="text-xs text-gray-500">
                    Leader: <span className="text-gray-300">{leader.fullName}</span>
                </p>
            )}
            {count > 0 && (
                <div className="flex flex-wrap gap-1 mt-1">
                    {members.slice(0, 3).map((m, i) => (
                        <span
                            key={i}
                            className="text-xs text-gray-500 bg-white/5 border border-white/8 px-1.5 py-0.5 rounded"
                        >
                            {m.fullName.split(" ")[0]}
                        </span>
                    ))}
                    {count > 3 && (
                        <span className="text-xs text-gray-600 bg-white/5 border border-white/8 px-1.5 py-0.5 rounded">
                            +{count - 3} more
                        </span>
                    )}
                </div>
            )}
        </div>
    );
}

function formatRelativeDate(isoString: string): string {
    const diff = Date.now() - new Date(isoString).getTime();
    const minutes = Math.floor(diff / 60000);
    if (minutes < 60) return `${minutes}m ago`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours}h ago`;
    const days = Math.floor(hours / 24);
    if (days < 7) return `${days}d ago`;
    return new Date(isoString).toLocaleDateString("tr-TR", { day: "2-digit", month: "short" });
}
