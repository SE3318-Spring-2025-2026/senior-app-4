"use client";

import Sidebar from "@/components/Sidebar";
import Link from "next/link";
import { useRouter, useParams } from "next/navigation";
import { useEffect, useState, useCallback } from "react";
import {
    deleteCommittee,
    fetchCommitteeById,
    removeAdvisor,
    removeJury,
    fetchValidationRules,
    ValidationRules,
} from "@/lib/committees-api";
import { getUser } from "@/lib/auth";
import { CommitteeDetail } from "@/lib/committee-types";
import { showToast } from "@/components/toast/ToastContext";
import { toast } from "sonner";
import AssignAdvisorForm from "@/components/committees/AssignAdvisorForm";
import AssignJuryForm from "@/components/committees/AssignJuryForm";

type ModalType = "advisor" | "jury" | null;

function fmtDate(iso?: string | null) {
    if (!iso) return "—";
    return new Date(iso).toLocaleDateString("en-GB", {
        day: "2-digit",
        month: "short",
        year: "numeric",
    });
}

function RoleBadge({ role }: { role: string }) {
    const colors: Record<string, string> = {
        PRESIDENT: "bg-yellow-500/10 text-yellow-400 border-yellow-500/20",
        VICE_PRESIDENT: "bg-orange-500/10 text-orange-400 border-orange-500/20",
        MEMBER: "bg-blue-500/10 text-blue-400 border-blue-500/20",
        ADVISOR: "bg-purple-500/10 text-purple-400 border-purple-500/20",
    };
    return (
        <span className={`inline-flex items-center px-2 py-0.5 rounded-md border text-xs font-medium ${colors[role] ?? "bg-gray-500/10 text-gray-400 border-gray-500/20"}`}>
            {role.replace(/_/g, " ")}
        </span>
    );
}

function JuryTypeBadge({ type }: { type: string }) {
    const colors: Record<string, string> = {
        INTERNAL: "bg-teal-500/10 text-teal-400 border-teal-500/20",
        EXTERNAL: "bg-violet-500/10 text-violet-400 border-violet-500/20",
        ADDITIONAL: "bg-sky-500/10 text-sky-400 border-sky-500/20",
        JURY: "bg-violet-500/10 text-violet-400 border-violet-500/20",
    };
    return (
        <span className={`inline-flex items-center px-2 py-0.5 rounded-md border text-xs font-medium ${colors[type] ?? "bg-gray-500/10 text-gray-400 border-gray-500/20"}`}>
            {type}
        </span>
    );
}

export default function CommitteeDetailPage() {
    const params = useParams();
    const committeeId = Number(params.committeeId);
    const router = useRouter();

    const [isCoordinator, setIsCoordinator] = useState(false);
    const [committee, setCommittee] = useState<CommitteeDetail | null>(null);
    const [rules, setRules] = useState<ValidationRules | null>(null);
    const [rulesOpen, setRulesOpen] = useState(false);
    const [loading, setLoading] = useState(true);
    const [modal, setModal] = useState<ModalType>(null);
    const [removingAdvisorId, setRemovingAdvisorId] = useState<number | null>(null);
    const [removingJuryId, setRemovingJuryId] = useState<number | null>(null);

    useEffect(() => {
        const currentUser = getUser();
        setIsCoordinator(currentUser?.role === "coordinator");
    }, []);

    const loadCommittee = useCallback(async () => {
        try {
            setLoading(true);
            const data = await fetchCommitteeById(committeeId);
            setCommittee(data);
        } catch (err) {
            showToast(
                err instanceof Error ? err.message : "Failed to load committee.",
                "error"
            );
        } finally {
            setLoading(false);
        }
    }, [committeeId]);

    const loadRules = useCallback(async () => {
        try {
            const data = await fetchValidationRules(committeeId);
            setRules(data);
        } catch {
            // endpoint may not be implemented yet — silent fail
        }
    }, [committeeId]);

    useEffect(() => {
        if (!Number.isNaN(committeeId)) {
            loadCommittee();
            loadRules();
        }
    }, [committeeId, loadCommittee, loadRules]);

    // ── Advisor remove ────────────────────────────────────────────────────
    const handleRemoveAdvisor = async (advisorId: number) => {
        if (!confirm("Remove this advisor from the committee?")) return;
        setRemovingAdvisorId(advisorId);
        try {
            await removeAdvisor(committeeId, advisorId);
            toast.success("Advisor removed");
            loadCommittee();
        } catch (err: any) {
            toast.error(err?.message ?? "Failed to remove advisor");
        } finally {
            setRemovingAdvisorId(null);
        }
    };

    // ── Jury remove ───────────────────────────────────────────────────────
    const handleRemoveJury = async (juryId: number) => {
        if (!confirm("Remove this jury member from the committee?")) return;
        setRemovingJuryId(juryId);
        try {
            await removeJury(committeeId, juryId);
            toast.success("Jury member removed");
            loadCommittee();
        } catch (err: any) {
            toast.error(err?.message ?? "Failed to remove jury member");
        } finally {
            setRemovingJuryId(null);
        }
    };

    const handleModalSuccess = () => {
        setModal(null);
        loadCommittee();
    };

    const assignedJuryIds: number[] = (committee?.jury ?? []).map(
        (j: any) => j.id ?? j.userId ?? j.professorId
    );

    return (
        <div className="min-h-screen bg-gray-950 flex">
            <Sidebar activePage="committees" />

            <main className="flex-1 min-w-0 px-6 py-10 text-white">
                <div className="mx-auto max-w-5xl space-y-8">

                    {/* ── Breadcrumb ── */}
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
                                {Array.from({ length: 3 }).map((_, i) => (
                                    <div key={i} className="h-24 animate-pulse rounded-xl bg-white/5" />
                                ))}
                            </div>
                        </div>
                    ) : !committee ? (
                        <div className="rounded-2xl border border-red-500/20 bg-red-500/10 p-8 text-red-200">
                            Committee not found.
                        </div>
                    ) : (
                        <>
                            {/* ── Header card ── */}
                            <div className="rounded-2xl border border-white/10 bg-gray-900/70 p-8">
                                <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                                    <div>
                                        <h1 className="text-3xl font-bold">{committee.committeeName}</h1>
                                        <p className="mt-3 max-w-2xl text-gray-400">
                                            {committee.description || "No description provided."}
                                        </p>
                                    </div>
                                    <div className="flex items-center gap-2">
                                        <span className={`rounded-full border px-4 py-2 text-sm font-semibold ${
                                            committee.status === "ACTIVE"
                                                ? "border-cyan-400/40 bg-cyan-400/15 text-cyan-300"
                                                : committee.status === "INACTIVE"
                                                ? "border-pink-500/40 bg-pink-500/15 text-pink-400"
                                                : committee.status === "COMPLETED"
                                                ? "border-orange-400/40 bg-orange-400/15 text-orange-300"
                                                : "border-gray-500/20 bg-gray-500/10 text-gray-400"
                                        }`}>
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

                                {/* Stats row */}
                                <div className="mt-8 grid grid-cols-1 gap-4 md:grid-cols-3">
                                    <StatCard label="Advisor Count" value={committee.advisorCount ?? 0} />
                                    <StatCard label="Jury Count" value={committee.juryCount ?? 0} />
                                    <StatCard label="Group Count" value={committee.groupCount ?? 0} />
                                </div>
                            </div>

                            {/* ── Details ── */}
                            <div className="rounded-2xl border border-white/10 bg-gray-900/70 p-6">
                                <h2 className="text-xl font-semibold">Committee Details</h2>
                                <div className="mt-5 grid grid-cols-1 gap-4 md:grid-cols-2">
                                    <Info label="Committee ID" value={String(committee.committeeId)} />
                                    <Info label="Created By" value={committee.createdByName ?? `User #${committee.createdBy}`} />
                                    <Info label="Created At" value={committee.createdAt ? new Date(committee.createdAt).toLocaleString() : "-"} />
                                    <Info label="Updated At" value={committee.updatedAt ? new Date(committee.updatedAt).toLocaleString() : "-"} />
                                </div>
                            </div>

                            {/* ── Validation Rules (visible to all) ── */}
                            <div className="rounded-2xl border border-white/10 bg-gray-900/70 overflow-hidden">
                                    <button
                                        onClick={() => setRulesOpen((v) => !v)}
                                        className="w-full flex items-center justify-between px-6 py-4 hover:bg-white/5 transition-colors"
                                    >
                                        <span className="text-base font-semibold text-white flex items-center gap-2">
                                            <span className="text-amber-400">⚙</span>
                                            Validation Rules
                                        </span>
                                        <span className="text-gray-400 text-xs">{rulesOpen ? "▲ Hide" : "▼ Show"}</span>
                                    </button>
                                    {rulesOpen && (
                                        <div className="px-6 pb-6 border-t border-white/5">
                                            {rules ? (
                                                <div className="mt-4 grid grid-cols-1 sm:grid-cols-2 gap-4">
                                                    <RuleSection title="Committee Size">
                                                        <RuleItem label="Min Advisors" value={rules.minAdvisors} />
                                                        <RuleItem label="Max Advisors" value={rules.maxAdvisors} />
                                                        <RuleItem label="Min Jury Members" value={rules.minJury} />
                                                        <RuleItem label="Max Jury Members" value={rules.maxJury} />
                                                        {rules.scheduleWindow && (
                                                            <RuleItem label="Schedule Window" value={rules.scheduleWindow} />
                                                        )}
                                                    </RuleSection>
                                                    {rules.explicitRules && rules.explicitRules.length > 0 && (
                                                        <RuleSection title="Assignment Rules">
                                                            {rules.explicitRules.map((r: string, i: number) => (
                                                                <li key={i} className="text-sm text-gray-300 list-disc ml-4">{r}</li>
                                                            ))}
                                                        </RuleSection>
                                                    )}
                                                </div>
                                            ) : (
                                                <p className="mt-4 text-sm text-gray-500">
                                                    Validation rules are not available for this committee yet.
                                                </p>
                                            )}
                                        </div>
                                    )}
                                </div>

                            {/* ── Advisors ── */}
                            <div className="rounded-2xl border border-white/10 bg-gray-900/70 overflow-hidden">
                                <div className="flex items-center justify-between px-6 py-5 border-b border-white/5">
                                    <h2 className="text-xl font-semibold flex items-center gap-2">
                                        Advisors
                                        <span className="text-xs font-normal text-gray-500 bg-white/5 px-2 py-0.5 rounded-full">
                                            {committee.advisors.length}
                                        </span>
                                    </h2>
                                    {isCoordinator && (
                                        <button
                                            onClick={() => setModal("advisor")}
                                            className="bg-blue-600/20 text-blue-400 border border-blue-500/30 px-3 py-1.5 rounded-lg text-sm font-medium hover:bg-blue-600/30 transition-colors"
                                        >
                                            + Assign Advisor
                                        </button>
                                    )}
                                </div>
                                <div className="p-6">
                                    {committee.advisors.length === 0 ? (
                                        <p className="text-gray-400 text-sm">No advisors assigned.</p>
                                    ) : (
                                        <table className="w-full text-left">
                                            <thead>
                                                <tr className="border-b border-white/10">
                                                    <Th>Name</Th>
                                                    <Th>Role</Th>
                                                    <Th>Assigned</Th>
                                                    {isCoordinator && <Th right>Actions</Th>}
                                                </tr>
                                            </thead>
                                            <tbody className="divide-y divide-white/5">
                                                {committee.advisors.map((a: any) => {
                                                    const aId = a.id ?? a.userId ?? a.advisorId;
                                                    return (
                                                        <tr key={aId} className="hover:bg-white/5 transition-colors">
                                                            <Td><span className="font-medium">{a.name ?? a.fullName ?? "—"}</span></Td>
                                                            <Td><RoleBadge role={a.role ?? "ADVISOR"} /></Td>
                                                            <Td><span className="text-xs text-gray-500">{fmtDate(a.assignedAt)}</span></Td>
                                                            {isCoordinator && (
                                                                <Td right>
                                                                    <button
                                                                        onClick={() => handleRemoveAdvisor(aId)}
                                                                        disabled={removingAdvisorId === aId}
                                                                        className="text-sm bg-red-500/10 text-red-400 border border-red-500/20 px-3 py-1 rounded-lg hover:bg-red-500/20 transition-colors disabled:opacity-50"
                                                                    >
                                                                        {removingAdvisorId === aId ? "Removing…" : "Remove"}
                                                                    </button>
                                                                </Td>
                                                            )}
                                                        </tr>
                                                    );
                                                })}
                                            </tbody>
                                        </table>
                                    )}
                                </div>
                            </div>

                            {/* ── Jury ── */}
                            <div className="rounded-2xl border border-white/10 bg-gray-900/70 overflow-hidden">
                                <div className="flex items-center justify-between px-6 py-5 border-b border-white/5">
                                    <h2 className="text-xl font-semibold flex items-center gap-2">
                                        Jury
                                        <span className="text-xs font-normal text-gray-500 bg-white/5 px-2 py-0.5 rounded-full">
                                            {committee.jury.length}
                                        </span>
                                    </h2>
                                    {isCoordinator && (
                                        <button
                                            onClick={() => setModal("jury")}
                                            className="bg-violet-600/20 text-violet-400 border border-violet-500/30 px-3 py-1.5 rounded-lg text-sm font-medium hover:bg-violet-600/30 transition-colors"
                                        >
                                            + Assign Jury Member
                                        </button>
                                    )}
                                </div>
                                <div className="p-6">
                                    {committee.jury.length === 0 ? (
                                        <p className="text-gray-400 text-sm">No jury members assigned.</p>
                                    ) : (
                                        <table className="w-full text-left">
                                            <thead>
                                                <tr className="border-b border-white/10">
                                                    <Th>Name</Th>
                                                    <Th>Type</Th>
                                                    <Th>Assigned</Th>
                                                    {isCoordinator && <Th right>Actions</Th>}
                                                </tr>
                                            </thead>
                                            <tbody className="divide-y divide-white/5">
                                                {committee.jury.map((j: any) => {
                                                    const jId = j.id ?? j.committeeJuryId ?? j.juryMemberId;
                                                    const jUserId = j.userId ?? jId;
                                                    return (
                                                        <tr key={jId} className="hover:bg-white/5 transition-colors">
                                                            <Td><span className="font-medium">{j.name ?? j.fullName ?? "—"}</span></Td>
                                                            <Td><JuryTypeBadge type={j.role ?? j.juryType ?? "JURY"} /></Td>
                                                            <Td><span className="text-xs text-gray-500">{fmtDate(j.assignedAt)}</span></Td>
                                                            {isCoordinator && (
                                                                <Td right>
                                                                    <button
                                                                        onClick={() => handleRemoveJury(jUserId)}
                                                                        disabled={removingJuryId === jUserId}
                                                                        className="text-sm bg-red-500/10 text-red-400 border border-red-500/20 px-3 py-1 rounded-lg hover:bg-red-500/20 transition-colors disabled:opacity-50"
                                                                    >
                                                                        {removingJuryId === jUserId ? "Removing…" : "Remove"}
                                                                    </button>
                                                                </Td>
                                                            )}
                                                        </tr>
                                                    );
                                                })}
                                            </tbody>
                                        </table>
                                    )}
                                </div>
                            </div>

                            {/* ── Assigned Groups ── */}
                            <div className="rounded-2xl border border-white/10 bg-gray-900/70 p-6">
                                <h2 className="text-xl font-semibold flex items-center gap-2">
                                    Assigned Groups
                                    <span className="text-xs font-normal text-gray-500 bg-white/5 px-2 py-0.5 rounded-full">
                                        {committee.groups.length}
                                    </span>
                                </h2>
                                <div className="mt-4 space-y-3">
                                    {committee.groups.length === 0 ? (
                                        <p className="text-gray-400 text-sm">No groups assigned yet. Groups will appear here automatically when an advisor with groups is added to this committee.</p>
                                    ) : (
                                        committee.groups.map((group) => (
                                            <button
                                                key={group.groupId}
                                                onClick={() => router.push(`/groups/${group.groupId}`)}
                                                className="w-full flex justify-between items-center border border-white/10 rounded-xl p-4 bg-white/5 hover:bg-white/10 hover:border-blue-500/30 transition-all text-left cursor-pointer group"
                                            >
                                                <div className="flex items-center gap-3">
                                                    <div className="w-8 h-8 rounded-lg bg-blue-500/15 flex items-center justify-center text-blue-400 text-sm font-bold">
                                                        {group.groupName.charAt(0).toUpperCase()}
                                                    </div>
                                                    <div>
                                                        <p className="font-medium text-white group-hover:text-blue-300 transition-colors">{group.groupName}</p>
                                                        <p className="text-xs text-gray-400">Members: {group.membersCount}</p>
                                                    </div>
                                                </div>
                                                <div className="flex items-center gap-3">
                                                    <div className="text-right">
                                                        <span className={`inline-flex items-center px-2 py-0.5 rounded-md border text-xs font-medium ${
                                                            group.status === "ASSIGNED" ? "bg-blue-500/10 text-blue-400 border-blue-500/20" :
                                                            group.status === "SCHEDULED" ? "bg-amber-500/10 text-amber-400 border-amber-500/20" :
                                                            group.status === "COMPLETED" ? "bg-green-500/10 text-green-400 border-green-500/20" :
                                                            "bg-gray-500/10 text-gray-400 border-gray-500/20"
                                                        }`}>
                                                            {group.status}
                                                        </span>
                                                        {group.examDate && (
                                                            <p className="text-xs text-gray-500 mt-1">{new Date(group.examDate).toLocaleDateString()}</p>
                                                        )}
                                                    </div>
                                                    <span className="text-gray-500 group-hover:text-blue-400 transition-colors text-lg">→</span>
                                                </div>
                                            </button>
                                        ))
                                    )}
                                </div>
                            </div>
                        </>
                    )}
                </div>
            </main>

            {/* ── Modals ── */}
            {modal && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
                    <div className="bg-gray-900 border border-white/10 w-full max-w-md rounded-2xl shadow-2xl p-6">
                        {modal === "advisor" ? (
                            <>
                                <h2 className="text-xl font-bold text-white mb-6">Assign Advisor</h2>
                                <AssignAdvisorForm
                                    committeeId={committeeId}
                                    onSuccess={handleModalSuccess}
                                    onCancel={() => setModal(null)}
                                />
                            </>
                        ) : (
                            <>
                                <h2 className="text-xl font-bold text-white mb-6">Assign Jury Member</h2>
                                <AssignJuryForm
                                    committeeId={committeeId}
                                    assignedProfessorIds={assignedJuryIds}
                                    onSuccess={handleModalSuccess}
                                    onCancel={() => setModal(null)}
                                />
                            </>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}

// ─── Reusable atoms ─────────────────────────────────────────────────────────

function StatCard({ label, value }: { label: string; value: number }) {
    return (
        <div className="rounded-xl border border-white/10 bg-white/5 p-5">
            <p className="text-sm text-gray-400">{label}</p>
            <p className="mt-2 text-2xl font-bold">{value}</p>
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

function Th({ children, right }: { children: React.ReactNode; right?: boolean }) {
    return (
        <th className={`py-3 px-2 text-xs font-semibold text-gray-400 uppercase tracking-wider ${right ? "text-right" : ""}`}>
            {children}
        </th>
    );
}

function Td({ children, right }: { children: React.ReactNode; right?: boolean }) {
    return (
        <td className={`py-3 px-2 ${right ? "text-right" : ""}`}>{children}</td>
    );
}

function RuleSection({ title, children }: { title: string; children: React.ReactNode }) {
    return (
        <div className="bg-white/5 rounded-xl p-4">
            <div className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-3">{title}</div>
            <div className="space-y-2">{children}</div>
        </div>
    );
}

function RuleItem({ label, value }: { label: string; value?: string | number | boolean | null }) {
    if (value === undefined || value === null) return null;
    return (
        <div className="flex justify-between items-center text-sm">
            <span className="text-gray-400">{label}</span>
            <span className="text-white font-medium">{String(value)}</span>
        </div>
    );
}