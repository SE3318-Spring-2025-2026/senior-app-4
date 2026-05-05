"use client";

import { useState, useEffect, useCallback } from "react";
import { useParams, useRouter } from "next/navigation";
import Sidebar from "@/components/Sidebar";
import { useAuthGuard } from "@/hooks/useAuthGuard";
import {
    fetchCommitteeById,
    removeAdvisor,
    assignAdvisor,
    removeJury,
    fetchValidationRules,
    ValidationRules,
} from "@/lib/committees-api";
import { CommitteeDetail } from "@/lib/committee-types";
import AssignAdvisorForm from "@/components/committees/AssignAdvisorForm";
import AssignJuryForm from "@/components/committees/AssignJuryForm";
import { toast } from "sonner";
import Link from "next/link";

// ─── Auth shell ────────────────────────────────────────────────────────────

export default function CoordinatorCommitteeDetailsPage() {
    const authStatus = useAuthGuard("coordinator");

    if (authStatus === "loading")
        return (
            <div className="min-h-screen bg-gray-950 flex items-center justify-center">
                <svg className="w-6 h-6 animate-spin text-blue-500" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z" />
                </svg>
            </div>
        );

    if (authStatus === "denied")
        return (
            <div className="min-h-screen flex items-center justify-center bg-gray-950">
                <h1 className="text-lg font-semibold text-white">Access Restricted</h1>
            </div>
        );

    return <CommitteeDetailDashboard />;
}

// ─── Helpers ───────────────────────────────────────────────────────────────

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

// ─── Main dashboard ─────────────────────────────────────────────────────────

function CommitteeDetailDashboard() {
    const { id } = useParams();
    const router = useRouter();
    const committeeId = Number(id);

    const [committee, setCommittee] = useState<CommitteeDetail | null>(null);
    const [rules, setRules] = useState<ValidationRules | null>(null);
    const [rulesOpen, setRulesOpen] = useState(false);
    const [loading, setLoading] = useState(true);
    const [modal, setModal] = useState<ModalType>(null);
    const [removingAdvisorId, setRemovingAdvisorId] = useState<number | null>(null);
    const [removingJuryId, setRemovingJuryId] = useState<number | null>(null);

    const loadCommittee = useCallback(async () => {
        setLoading(true);
        try {
            const data = await fetchCommitteeById(committeeId);
            setCommittee(data);
        } catch {
            toast.error("Committee not found");
            router.push("/coordinator/committees");
        } finally {
            setLoading(false);
        }
    }, [committeeId, router]);

    const loadRules = useCallback(async () => {
        try {
            const data = await fetchValidationRules(committeeId);
            setRules(data);
        } catch {
            // validation-rules endpoint may not be implemented yet — fail silently
        }
    }, [committeeId]);

    useEffect(() => {
        if (committeeId) {
            loadCommittee();
            loadRules();
        }
    }, [committeeId, loadCommittee, loadRules]);

    // ── Advisor actions ──────────────────────────────────────────────────

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

    // ── Jury actions ─────────────────────────────────────────────────────

    const handleRemoveJury = async (professorId: number) => {
        if (!confirm("Remove this jury member from the committee?")) return;
        setRemovingJuryId(professorId);
        try {
            await removeJury(committeeId, professorId);
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

    // ── Derive already-assigned IDs ──────────────────────────────────────

    const assignedAdvisorIds: number[] = (committee?.advisors ?? []).map(
        (a: any) => a.id ?? a.userId ?? a.advisorId
    );
    const assignedJuryIds: number[] = (committee?.jury ?? []).map(
        (j: any) => j.id ?? j.userId ?? j.professorId
    );

    // ── Render ────────────────────────────────────────────────────────────

    return (
        <div className="min-h-screen bg-gray-950 flex">
            <Sidebar activePage="committees" />

            <main className="flex-1 flex flex-col min-w-0">
                {/* ─── Header ─── */}
                <div className="border-b border-white/5 px-8 py-4 flex items-center justify-between">
                    <div>
                        <div className="flex items-center gap-2 text-sm text-gray-500 mb-1">
                            <Link href="/coordinator/committees" className="hover:text-white transition-colors">
                                Committees
                            </Link>
                            <span>/</span>
                            <span className="text-gray-300">Details</span>
                        </div>
                        <h1 className="text-base font-semibold text-white flex items-center gap-3">
                            {committee?.committeeName ?? "Loading…"}
                            {committee && (
                                <span
                                    className={`px-2 py-0.5 rounded text-xs font-medium border ${
                                        committee.status === "ACTIVE"
                                            ? "bg-green-500/10 text-green-400 border-green-500/20"
                                            : "bg-gray-500/10 text-gray-400 border-gray-500/20"
                                    }`}
                                >
                                    {committee.status}
                                </span>
                            )}
                        </h1>
                    </div>
                </div>

                {/* ─── Body ─── */}
                <div className="flex-1 p-8">
                    {loading && !committee ? (
                        <div className="flex justify-center py-20">
                            <div className="w-8 h-8 border-4 border-white/20 border-t-blue-500 rounded-full animate-spin" />
                        </div>
                    ) : committee ? (
                        <div className="max-w-5xl mx-auto space-y-6">

                            {/* ── Description ── */}
                            <div className="bg-gray-900 border border-white/10 rounded-2xl p-6">
                                <h2 className="text-sm font-semibold text-gray-400 uppercase tracking-wider mb-2">
                                    Description
                                </h2>
                                <p className="text-gray-300 text-sm">
                                    {committee.description ?? "No description provided."}
                                </p>
                            </div>

                            {/* ── Validation Rules ── */}
                            <div className="bg-gray-900 border border-white/10 rounded-2xl overflow-hidden">
                                <button
                                    onClick={() => setRulesOpen((v) => !v)}
                                    className="w-full flex items-center justify-between px-6 py-4 hover:bg-white/5 transition-colors"
                                >
                                    <span className="text-sm font-semibold text-white flex items-center gap-2">
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
                                                    <RuleItem label="Min Jury Members" value={rules.minJuryMembers} />
                                                    <RuleItem label="Max Jury Members" value={rules.maxJuryMembers} />
                                                </RuleSection>
                                                <RuleSection title="Advisor Qualifications">
                                                    <RuleItem label="Requires President" value={rules.requiresPresident ? "Yes" : "No"} />
                                                    <RuleItem label="Requires Vice President" value={rules.requiresVicePresident ? "Yes" : "No"} />
                                                    <RuleItem label="Advisor Group Limit" value={rules.advisorGroupLimit} />
                                                </RuleSection>
                                                {rules.scheduleRules && rules.scheduleRules.length > 0 && (
                                                    <RuleSection title="Schedule Rules">
                                                        {rules.scheduleRules.map((r, i) => (
                                                            <li key={i} className="text-sm text-gray-300 list-disc ml-4">{r}</li>
                                                        ))}
                                                    </RuleSection>
                                                )}
                                                {rules.groupAssignmentRules && rules.groupAssignmentRules.length > 0 && (
                                                    <RuleSection title="Group Assignment Rules">
                                                        {rules.groupAssignmentRules.map((r, i) => (
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
                            <SectionCard
                                title="Assigned Advisors"
                                count={committee.advisors?.length ?? 0}
                                action={
                                    <button
                                        onClick={() => setModal("advisor")}
                                        className="bg-blue-600/20 text-blue-400 border border-blue-500/30 px-3 py-1.5 rounded-lg text-sm font-medium hover:bg-blue-600/30 transition-colors"
                                    >
                                        + Assign Advisor
                                    </button>
                                }
                            >
                                {committee.advisors && committee.advisors.length > 0 ? (
                                    <table className="w-full text-left">
                                        <thead>
                                            <tr className="bg-white/5 border-b border-white/10">
                                                <Th>Name</Th>
                                                <Th>Email</Th>
                                                <Th>Role</Th>
                                                <Th>Assigned</Th>
                                                <Th right>Actions</Th>
                                            </tr>
                                        </thead>
                                        <tbody className="divide-y divide-white/5">
                                            {committee.advisors.map((a: any) => {
                                                const advisorId = a.id ?? a.userId ?? a.advisorId;
                                                return (
                                                    <tr key={advisorId} className="hover:bg-white/5 transition-colors">
                                                        <Td>
                                                            <div className="text-sm font-medium text-white">
                                                                {a.name ?? a.fullName ?? a.advisorName ?? "—"}
                                                            </div>
                                                        </Td>
                                                        <Td>
                                                            <span className="text-xs text-gray-400">
                                                                {a.email ?? a.advisorEmail ?? "—"}
                                                            </span>
                                                        </Td>
                                                        <Td>
                                                            <RoleBadge role={a.role ?? "MEMBER"} />
                                                        </Td>
                                                        <Td>
                                                            <span className="text-xs text-gray-500">
                                                                {fmtDate(a.assignedAt)}
                                                            </span>
                                                        </Td>
                                                        <Td right>
                                                            <button
                                                                onClick={() => handleRemoveAdvisor(advisorId)}
                                                                disabled={removingAdvisorId === advisorId}
                                                                className="text-sm bg-red-500/10 text-red-400 border border-red-500/20 px-3 py-1 rounded-lg hover:bg-red-500/20 transition-colors disabled:opacity-50"
                                                            >
                                                                {removingAdvisorId === advisorId ? "Removing…" : "Remove"}
                                                            </button>
                                                        </Td>
                                                    </tr>
                                                );
                                            })}
                                        </tbody>
                                    </table>
                                ) : (
                                    <EmptyState text="No advisors assigned yet." />
                                )}
                            </SectionCard>

                            {/* ── Jury ── */}
                            <SectionCard
                                title="Jury Members"
                                count={committee.jury?.length ?? 0}
                                action={
                                    <button
                                        onClick={() => setModal("jury")}
                                        className="bg-violet-600/20 text-violet-400 border border-violet-500/30 px-3 py-1.5 rounded-lg text-sm font-medium hover:bg-violet-600/30 transition-colors"
                                    >
                                        + Assign Jury Member
                                    </button>
                                }
                            >
                                {committee.jury && committee.jury.length > 0 ? (
                                    <table className="w-full text-left">
                                        <thead>
                                            <tr className="bg-white/5 border-b border-white/10">
                                                <Th>Name</Th>
                                                <Th>Email</Th>
                                                <Th>Type</Th>
                                                <Th>Assigned</Th>
                                                <Th right>Actions</Th>
                                            </tr>
                                        </thead>
                                        <tbody className="divide-y divide-white/5">
                                            {committee.jury.map((j: any) => {
                                                const juryId = j.id ?? j.userId ?? j.professorId ?? j.juryMemberId;
                                                return (
                                                    <tr key={juryId} className="hover:bg-white/5 transition-colors">
                                                        <Td>
                                                            <div className="text-sm font-medium text-white">
                                                                {j.name ?? j.fullName ?? j.juryMemberName ?? "—"}
                                                            </div>
                                                        </Td>
                                                        <Td>
                                                            <span className="text-xs text-gray-400">
                                                                {j.email ?? j.juryMemberEmail ?? "—"}
                                                            </span>
                                                        </Td>
                                                        <Td>
                                                            <JuryTypeBadge type={j.role ?? j.juryType ?? "JURY"} />
                                                        </Td>
                                                        <Td>
                                                            <span className="text-xs text-gray-500">
                                                                {fmtDate(j.assignedAt)}
                                                            </span>
                                                        </Td>
                                                        <Td right>
                                                            <button
                                                                onClick={() => handleRemoveJury(juryId)}
                                                                disabled={removingJuryId === juryId}
                                                                className="text-sm bg-red-500/10 text-red-400 border border-red-500/20 px-3 py-1 rounded-lg hover:bg-red-500/20 transition-colors disabled:opacity-50"
                                                            >
                                                                {removingJuryId === juryId ? "Removing…" : "Remove"}
                                                            </button>
                                                        </Td>
                                                    </tr>
                                                );
                                            })}
                                        </tbody>
                                    </table>
                                ) : (
                                    <EmptyState text="No jury members assigned yet." />
                                )}
                            </SectionCard>

                            {/* ── Assigned Groups ── */}
                            {committee.groups && committee.groups.length > 0 && (
                                <SectionCard title="Assigned Groups" count={committee.groups.length}>
                                    <table className="w-full text-left">
                                        <thead>
                                            <tr className="bg-white/5 border-b border-white/10">
                                                <Th>Group Name</Th>
                                                <Th>Members</Th>
                                                <Th>Status</Th>
                                                <Th>Exam Date</Th>
                                            </tr>
                                        </thead>
                                        <tbody className="divide-y divide-white/5">
                                            {committee.groups.map((g) => (
                                                <tr key={g.groupId} className="hover:bg-white/5 transition-colors">
                                                    <Td>
                                                        <span className="text-sm font-medium text-white">{g.groupName}</span>
                                                    </Td>
                                                    <Td>
                                                        <span className="text-sm text-gray-400">{g.membersCount}</span>
                                                    </Td>
                                                    <Td>
                                                        <span className="text-xs text-gray-400">{g.status}</span>
                                                    </Td>
                                                    <Td>
                                                        <span className="text-xs text-gray-500">{fmtDate(g.examDate)}</span>
                                                    </Td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </SectionCard>
                            )}
                        </div>
                    ) : null}
                </div>
            </main>

            {/* ─── Modals ─── */}
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

// ─── Small UI helpers ───────────────────────────────────────────────────────

function SectionCard({
    title,
    count,
    action,
    children,
}: {
    title: string;
    count?: number;
    action?: React.ReactNode;
    children: React.ReactNode;
}) {
    return (
        <div className="bg-gray-900 border border-white/10 rounded-2xl overflow-hidden shadow-xl shadow-black/20">
            <div className="p-6 border-b border-white/5 flex justify-between items-center">
                <h2 className="text-base font-semibold text-white flex items-center gap-2">
                    {title}
                    {count !== undefined && (
                        <span className="text-xs font-normal text-gray-500 bg-white/5 px-2 py-0.5 rounded-full">
                            {count}
                        </span>
                    )}
                </h2>
                {action}
            </div>
            {children}
        </div>
    );
}

function EmptyState({ text }: { text: string }) {
    return (
        <div className="text-center py-10">
            <p className="text-gray-500 text-sm">{text}</p>
        </div>
    );
}

function Th({ children, right }: { children: React.ReactNode; right?: boolean }) {
    return (
        <th
            className={`px-6 py-4 text-xs font-semibold text-gray-400 uppercase tracking-wider ${right ? "text-right" : ""}`}
        >
            {children}
        </th>
    );
}

function Td({ children, right }: { children: React.ReactNode; right?: boolean }) {
    return (
        <td className={`px-6 py-4 ${right ? "text-right" : ""}`}>{children}</td>
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
