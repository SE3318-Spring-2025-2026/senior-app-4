"use client";

import { useEffect, useMemo, useState } from "react";
import { toast } from "sonner";
import ConfirmDialog from "@/components/ConfirmDialog";
import { decodeToken, getToken, getUser } from "@/lib/auth";
import {
    AdvisorRequestStatus,
    createAdvisorRequest,
    fetchAdvisorRequestStatus,
    fetchProfessors,
    ProfessorDirectoryItem,
    withdrawAdvisorRequest,
} from "@/lib/advisor-api";

type Props = {
    groupId: number;
    leaderId: number;
    advisorId: number | null;
};

const mockProfessors: ProfessorDirectoryItem[] = [
    {
        userId: 11,
        fullName: "Dr. Ayse Yilmaz",
        email: "ayse.yilmaz@yasar.edu.tr",
        githubUsername: "drayseyilmaz",
        role: "professor",
        createdAt: null,
    },
    {
        userId: 12,
        fullName: "Dr. Mehmet Kaya",
        email: "mehmet.kaya@yasar.edu.tr",
        githubUsername: "drmehmetkaya",
        role: "professor",
        createdAt: null,
    },
    {
        userId: 17,
        fullName: "Dr. Selin Aydin",
        email: "selin.aydin@yasar.edu.tr",
        githubUsername: "drselinaydin",
        role: "professor",
        createdAt: null,
    },
];

function getCurrentUserId() {
    const token = getToken();
    const storedUser = getUser();

    if (!token) {
        return storedUser?.userId ?? null;
    }

    const claims = decodeToken(token);
    const claim = claims?.userId;

    if (typeof claim === "number") return claim;
    if (typeof claim === "string") return Number(claim);
    return storedUser?.userId ?? null;
}

function getProfessorAccentClass(active: boolean) {
    if (active) {
        return "border-blue-400/40 bg-blue-500/10 shadow-blue-950/20";
    }

    return "border-white/10 bg-gray-900/70 shadow-black/20";
}

export default function AdvisorRequestPanel({
    groupId,
    leaderId,
    advisorId,
}: Props) {
    const [professors, setProfessors] = useState<ProfessorDirectoryItem[]>([]);
    const [activeRequest, setActiveRequest] = useState<AdvisorRequestStatus | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [requestingProfessorId, setRequestingProfessorId] = useState<number | null>(null);
    const [withdrawing, setWithdrawing] = useState(false);
    const [confirmOpen, setConfirmOpen] = useState(false);
    const [usingMockData, setUsingMockData] = useState(false);

    const user = getUser();
    const currentUserId = getCurrentUserId();
    const isLeader =
        user?.role === "student" &&
        currentUserId !== null &&
        currentUserId === leaderId;

    useEffect(() => {
        let cancelled = false;

        async function loadPanel() {
            try {
                setLoading(true);
                setError("");

                const [professorData, requestStatus] = await Promise.all([
                    fetchProfessors(),
                    fetchAdvisorRequestStatus(groupId),
                ]);

                if (cancelled) return;

                setProfessors(professorData);
                setActiveRequest(requestStatus);
                setUsingMockData(false);
            } catch {
                if (cancelled) return;

                setProfessors(mockProfessors);
                setActiveRequest(null);
                setUsingMockData(true);
                setError("");
            } finally {
                if (!cancelled) {
                    setLoading(false);
                }
            }
        }

        loadPanel();

        return () => {
            cancelled = true;
        };
    }, [groupId]);

    const requestedProfessorName = activeRequest?.professorName ?? null;
    const requestedProfessor = useMemo(
        () => professors.find((professor) => professor.fullName === requestedProfessorName) ?? null,
        [professors, requestedProfessorName]
    );

    const requestButtonsDisabled = Boolean(activeRequest) || Boolean(advisorId) || !isLeader;

    async function refreshRequestStatus() {
        if (usingMockData) {
            return;
        }

        const requestStatus = await fetchAdvisorRequestStatus(groupId);
        setActiveRequest(requestStatus);
    }

    async function handleRequestAdvisor(professor: ProfessorDirectoryItem) {
        if (usingMockData) {
            setActiveRequest({
                requestId: Date.now(),
                professorName: professor.fullName,
                status: "PENDING",
            });
            toast.success(`Mock preview: advisor request sent to ${professor.fullName}.`);
            return;
        }

        try {
            setRequestingProfessorId(professor.userId);
            await createAdvisorRequest(groupId, professor.userId);
            await refreshRequestStatus();
            toast.success(`Advisor request sent to ${professor.fullName}.`);
        } catch (err) {
            toast.error(
                err instanceof Error ? err.message : "Failed to send advisor request."
            );
        } finally {
            setRequestingProfessorId(null);
        }
    }

    async function handleWithdrawRequest() {
        if (usingMockData) {
            setWithdrawing(true);
            setActiveRequest(null);
            setConfirmOpen(false);
            setWithdrawing(false);
            toast.success("Mock preview: advisor request withdrawn successfully.");
            return;
        }

        try {
            setWithdrawing(true);
            await withdrawAdvisorRequest(groupId);
            await refreshRequestStatus();
            toast.success("Advisor request withdrawn successfully.");
            setConfirmOpen(false);
        } catch (err) {
            toast.error(
                err instanceof Error ? err.message : "Failed to withdraw advisor request."
            );
        } finally {
            setWithdrawing(false);
        }
    }

    return (
        <>
            <section className="mb-8 rounded-2xl border border-white/10 bg-gray-900/70 p-7 shadow-lg shadow-black/20 backdrop-blur">
                <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                    <div>
                        <h2 className="text-2xl font-semibold text-white">
                            Professor Selection & Advisor Request
                        </h2>
                        <p className="mt-2 max-w-2xl text-sm text-gray-400">
                            Browse available professors, send an advisor request, and manage
                            your current pending request from one place.
                        </p>
                        <p className="mt-2 max-w-2xl text-xs text-gray-500">
                            TODO: switch this directory to backend-provided availability data
                            when issue #80 exposes professor availability/filtering support.
                        </p>
                    </div>

                    <div className="rounded-xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-gray-300">
                        {advisorId ? (
                            <span>Advisor assigned: Advisor #{advisorId}</span>
                        ) : activeRequest ? (
                            <span>Pending request: {activeRequest.professorName}</span>
                        ) : (
                            <span>No active advisor request</span>
                        )}
                    </div>
                </div>

                {usingMockData && (
                    <div className="mt-4 rounded-2xl border border-amber-500/20 bg-amber-500/10 p-4 text-sm text-amber-100">
                        Preview mode is active because advisor APIs are unavailable from the browser.
                        Request and withdraw actions are currently simulated with mock data.
                    </div>
                )}

                {advisorId ? (
                    <div className="mt-6 rounded-2xl border border-green-500/20 bg-green-500/10 p-5 text-sm text-green-100">
                        This group already has an assigned advisor. New requests are unavailable
                        until the current advisor assignment is released.
                    </div>
                ) : activeRequest ? (
                    <div className="mt-6 rounded-2xl border border-blue-500/20 bg-blue-500/10 p-5">
                        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                            <div>
                                <p className="text-sm text-blue-200">Active Request</p>
                                <p className="mt-1 text-lg font-semibold text-white">
                                    {activeRequest.professorName}
                                </p>
                                <p className="mt-1 text-sm text-blue-100/80">
                                    Status: {activeRequest.status}
                                </p>
                            </div>

                            {isLeader ? (
                                <button
                                    type="button"
                                    onClick={() => setConfirmOpen(true)}
                                    disabled={withdrawing}
                                    className="rounded-xl border border-red-500/20 bg-red-500/10 px-4 py-2.5 text-sm font-medium text-red-100 transition hover:bg-red-500/20 disabled:opacity-50"
                                >
                                    {withdrawing ? "Withdrawing..." : "Withdraw Request"}
                                </button>
                            ) : (
                                <p className="text-sm text-blue-100/80">
                                    Only the team leader can withdraw this request.
                                </p>
                            )}
                        </div>
                    </div>
                ) : !isLeader ? (
                    <div className="mt-6 rounded-2xl border border-amber-500/20 bg-amber-500/10 p-5 text-sm text-amber-100">
                        Only the team leader can submit or withdraw advisor requests.
                    </div>
                ) : null}

                {loading ? (
                    <div className="mt-6 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                        {Array.from({ length: 3 }).map((_, index) => (
                            <div
                                key={index}
                                className="h-48 animate-pulse rounded-2xl border border-white/10 bg-white/5"
                            />
                        ))}
                    </div>
                ) : error ? (
                    <div className="mt-6 rounded-2xl border border-red-500/20 bg-red-500/10 p-5 text-sm text-red-100">
                        {error}
                    </div>
                ) : professors.length === 0 ? (
                    <div className="mt-6 rounded-2xl border border-dashed border-white/10 bg-white/5 p-8 text-center text-sm text-gray-400">
                        No professors are available to request right now.
                    </div>
                ) : (
                    <div className="mt-6 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                        {professors.map((professor) => {
                            const isRequestedProfessor =
                                requestedProfessor?.userId === professor.userId ||
                                requestedProfessorName === professor.fullName;
                            const buttonDisabled =
                                requestButtonsDisabled || requestingProfessorId !== null;

                            return (
                                <article
                                    key={professor.userId}
                                    className={[
                                        "rounded-2xl border p-5 shadow-lg backdrop-blur transition-all",
                                        getProfessorAccentClass(isRequestedProfessor),
                                    ].join(" ")}
                                >
                                    <div className="flex items-start justify-between gap-3">
                                        <div>
                                            <h3 className="text-lg font-semibold text-white">
                                                {professor.fullName}
                                            </h3>
                                            <p className="mt-1 text-sm text-gray-400">
                                                {professor.email}
                                            </p>
                                        </div>

                                        {isRequestedProfessor && (
                                            <span className="rounded-full border border-blue-400/20 bg-blue-500/15 px-3 py-1 text-xs font-medium text-blue-200">
                                                Pending
                                            </span>
                                        )}
                                    </div>

                                    <div className="mt-4 space-y-2 text-sm text-gray-300">
                                        <p>
                                            <span className="text-gray-500">GitHub:</span>{" "}
                                            {professor.githubUsername || "Not linked"}
                                        </p>
                                        <p>
                                            <span className="text-gray-500">Role:</span>{" "}
                                            {professor.role}
                                        </p>
                                    </div>

                                    <button
                                        type="button"
                                        onClick={() => handleRequestAdvisor(professor)}
                                        disabled={buttonDisabled}
                                        className={[
                                            "mt-5 w-full rounded-xl px-4 py-2.5 text-sm font-medium transition",
                                            isRequestedProfessor
                                                ? "border border-blue-400/20 bg-blue-500/10 text-blue-100"
                                                : "bg-white text-gray-900 hover:bg-gray-100",
                                            buttonDisabled && !isRequestedProfessor
                                                ? "cursor-not-allowed border border-white/10 bg-white/5 text-gray-500"
                                                : "",
                                        ].join(" ")}
                                    >
                                        {requestingProfessorId === professor.userId
                                            ? "Requesting..."
                                            : isRequestedProfessor
                                              ? "Request Pending"
                                              : "Request Advisor"}
                                    </button>
                                </article>
                            );
                        })}
                    </div>
                )}
            </section>

            <ConfirmDialog
                open={confirmOpen}
                title="Withdraw advisor request?"
                message="This will cancel your current pending advisor request. You can send a new request after withdrawing it."
                confirmText="Withdraw Request"
                cancelText="Keep Request"
                loading={withdrawing}
                onConfirm={handleWithdrawRequest}
                onCancel={() => {
                    if (!withdrawing) {
                        setConfirmOpen(false);
                    }
                }}
            />
        </>
    );
}
