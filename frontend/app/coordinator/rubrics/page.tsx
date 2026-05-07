"use client";

import { useState, useEffect } from "react";
import { toast } from "sonner";
import Sidebar from "@/components/Sidebar";
import { buildHeaders, API_BASE } from "@/lib/api-utils";
import { useAuthGuard } from "@/hooks/useAuthGuard";

type DeliverableType = "PROPOSAL" | "REVISED_PROPOSAL" | "STATEMENT_OF_WORK" | "DEMONSTRATION";
type GradingType = "SOFT" | "BINARY";

interface GradingCriterion {
    id?: string;
    deliverableType: DeliverableType;
    gradingType?: GradingType | null;
    name: string;
    description: string;
    weight: number;
}

interface CriterionRow {
    key: number;
    name: string;
    description: string;
    gradingType: GradingType;
    weight: string;
}

// ─── Page shell (auth guard) ─────────────────────────────────────────────────

export default function RubricsPage() {
    const authStatus = useAuthGuard("coordinator");
    if (authStatus === "loading") return <Spinner />;
    if (authStatus === "denied") return <AccessDenied />;
    return <RubricsLayout />;
}

// ─── Layout ──────────────────────────────────────────────────────────────────

function RubricsLayout() {
    const [activeTab, setActiveTab] = useState<DeliverableType>("PROPOSAL");

    const tabs: { value: DeliverableType; label: string }[] = [
        { value: "PROPOSAL", label: "Proposal" },
        { value: "REVISED_PROPOSAL", label: "Revised Proposal" },
        { value: "STATEMENT_OF_WORK", label: "Statement of Work" },
        { value: "DEMONSTRATION", label: "Demonstration" },
    ];

    return (
        <div className="min-h-screen bg-gray-950 flex">
            <Sidebar activePage="rubrics" />
            <main className="flex-1 flex flex-col min-w-0">
                {/* Header */}
                <div className="border-b border-white/5 px-8 py-4 flex items-center justify-between">
                    <div>
                        <h1 className="text-base font-semibold text-white">Grading Criteria</h1>
                        <p className="text-xs text-gray-500 mt-0.5">
                            Define weighted rubric criteria for each deliverable type
                        </p>
                    </div>
                </div>

                {/* Tabs */}
                <div className="px-8 pt-6">
                    <div className="flex gap-1 bg-gray-900 border border-white/8 rounded-xl p-1 w-fit">
                        {tabs.map((tab) => (
                            <button
                                key={tab.value}
                                onClick={() => setActiveTab(tab.value)}
                                className={`px-4 py-1.5 rounded-lg text-sm font-medium transition-all ${
                                    activeTab === tab.value
                                        ? "bg-white/10 text-white"
                                        : "text-gray-500 hover:text-gray-300"
                                }`}
                            >
                                {tab.label}
                            </button>
                        ))}
                    </div>
                </div>

                {/* Content */}
                <div className="flex-1 p-8 pt-6">
                    <div className="max-w-3xl mx-auto space-y-5">
                        <CriteriaPanel key={activeTab} deliverableType={activeTab} />
                    </div>
                </div>
            </main>
        </div>
    );
}

// ─── Criteria panel (per deliverable type) ───────────────────────────────────

let rowCounter = 0;

function CriteriaPanel({ deliverableType }: { deliverableType: DeliverableType }) {
    const [existing, setExisting] = useState<GradingCriterion[]>([]);
    const [loadingExisting, setLoadingExisting] = useState(true);
    const [rows, setRows] = useState<CriterionRow[]>([newRow()]);
    const [saving, setSaving] = useState(false);
    const [editingId, setEditingId] = useState<string | null>(null);
    const [editingValues, setEditingValues] = useState<{ name: string; description: string; gradingType: GradingType; weight: string }>({ name: "", description: "", gradingType: "SOFT", weight: "" });
    const [editSaving, setEditSaving] = useState(false);

    function newRow(): CriterionRow {
        return { key: ++rowCounter, name: "", description: "", gradingType: "SOFT", weight: "" };
    }

    useEffect(() => {
        (async () => {
            setLoadingExisting(true);
            try {
                const res = await fetch(
                    `${API_BASE}/grading-criteria?deliverableType=${deliverableType}`,
                    { headers: buildHeaders() }
                );
                if (!res.ok) throw new Error();
                const body = await res.json();
                setExisting(body.data ?? []);
            } catch {
                // backend not yet implemented — start empty
                setExisting([]);
            } finally {
                setLoadingExisting(false);
            }
        })();
    }, [deliverableType]);

    const totalWeight = rows.reduce((sum, r) => sum + (parseFloat(r.weight) || 0), 0);
    const existingWeight = existing.reduce((sum, c) => sum + c.weight, 0);
    const combinedWeight = existingWeight + totalWeight;
    const overLimit = combinedWeight > 100;

    function updateRow(key: number, field: keyof Omit<CriterionRow, "key">, value: string) {
        setRows((prev) => prev.map((r) => (r.key === key ? { ...r, [field]: value } : r)));
    }

    function addRow() {
        setRows((prev) => [...prev, newRow()]);
    }

    function removeRow(key: number) {
        setRows((prev) => prev.filter((r) => r.key !== key));
    }

    function startEdit(c: GradingCriterion) {
        setEditingId(c.id!);
        setEditingValues({ name: c.name, description: c.description, gradingType: c.gradingType ?? "SOFT", weight: String(c.weight) });
    }

    function cancelEdit() {
        setEditingId(null);
    }

    async function saveEdit() {
        const weight = parseFloat(editingValues.weight);
        if (!editingValues.name.trim() || isNaN(weight) || weight <= 0) {
            toast.error("Name and a positive weight are required.");
            return;
        }
        setEditSaving(true);
        try {
            const res = await fetch(`${API_BASE}/grading-criteria/${editingId}`, {
                method: "PUT",
                headers: buildHeaders(),
                body: JSON.stringify({
                    deliverableType,
                    gradingType: editingValues.gradingType,
                    name: editingValues.name.trim(),
                    description: editingValues.description.trim(),
                    weight,
                }),
            });
            if (!res.ok) throw new Error("Failed to update criterion.");
            setExisting((prev) =>
                prev.map((c) =>
                    c.id === editingId
                        ? { ...c, name: editingValues.name.trim(), description: editingValues.description.trim(), gradingType: editingValues.gradingType, weight }
                        : c
                )
            );
            setEditingId(null);
            toast.success("Criterion updated.");
        } catch (err: unknown) {
            toast.error(err instanceof Error ? err.message : "Failed to update criterion.");
        } finally {
            setEditSaving(false);
        }
    }

    async function handleSubmit() {
        const valid = rows.filter((r) => r.name.trim() && parseFloat(r.weight) > 0);
        if (valid.length === 0) {
            toast.error("Add at least one criterion with a name and weight.");
            return;
        }
        if (overLimit) {
            toast.error("Total weight cannot exceed 100%.");
            return;
        }

        setSaving(true);
        try {
            await Promise.all(
                valid.map((r) =>
                    fetch(`${API_BASE}/grading-criteria`, {
                        method: "POST",
                        headers: buildHeaders(),
                        body: JSON.stringify({
                            deliverableType,
                            gradingType: r.gradingType,
                            name: r.name.trim(),
                            description: r.description.trim(),
                            weight: parseFloat(r.weight),
                        }),
                    }).then((res) => {
                        if (!res.ok) throw new Error(`Failed to save "${r.name}"`);
                    })
                )
            );
            toast.success(`${valid.length} criteria saved successfully.`);
            // Reload existing
            const res = await fetch(
                `${API_BASE}/grading-criteria?deliverableType=${deliverableType}`,
                { headers: buildHeaders() }
            );
            if (res.ok) {
                const body = await res.json();
                setExisting(body.data ?? []);
            }
            setRows([newRow()]);
        } catch (err: unknown) {
            toast.error(err instanceof Error ? err.message : "Failed to save criteria.");
        } finally {
            setSaving(false);
        }
    }

    return (
        <>
            {/* Existing criteria */}
            {loadingExisting ? (
                <div className="bg-gray-900 border border-white/8 rounded-2xl p-6 flex items-center justify-center">
                    <svg className="w-5 h-5 animate-spin text-blue-500" fill="none" viewBox="0 0 24 24">
                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                    </svg>
                </div>
            ) : existing.length > 0 ? (
                <div className="bg-gray-900 border border-white/8 rounded-2xl overflow-hidden">
                    <div className="px-6 py-4 border-b border-white/5 flex items-center gap-3">
                        <div className="w-8 h-8 rounded-lg bg-green-500/10 border border-green-500/20 flex items-center justify-center">
                            <svg className="w-4 h-4 text-green-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                        </div>
                        <div>
                            <p className="text-sm font-semibold text-white">Existing Criteria</p>
                            <p className="text-xs text-gray-500">Total weight: {existingWeight}%</p>
                        </div>
                    </div>
                    <div className="divide-y divide-white/5">
                        {existing.map((c) =>
                            editingId === c.id ? (
                                <div key={c.id} className="px-6 py-4 space-y-3">
                                    <div className="grid grid-cols-[1fr_1fr_2fr_80px] gap-3">
                                        <input
                                            type="text"
                                            value={editingValues.name}
                                            onChange={(e) => setEditingValues((v) => ({ ...v, name: e.target.value }))}
                                            className="rounded-lg border border-white/10 bg-gray-800 px-3 py-2 text-sm text-white placeholder-gray-600 focus:border-blue-500/50 focus:outline-none"
                                        />
                                        <select
                                            value={editingValues.gradingType}
                                            onChange={(e) => setEditingValues((v) => ({ ...v, gradingType: e.target.value as GradingType }))}
                                            className="rounded-lg border border-white/10 bg-gray-800 px-3 py-2 text-sm text-white focus:border-blue-500/50 focus:outline-none"
                                        >
                                            <option value="SOFT">Soft</option>
                                            <option value="BINARY">Binary</option>
                                        </select>
                                        <input
                                            type="text"
                                            value={editingValues.description}
                                            onChange={(e) => setEditingValues((v) => ({ ...v, description: e.target.value }))}
                                            className="rounded-lg border border-white/10 bg-gray-800 px-3 py-2 text-sm text-white placeholder-gray-600 focus:border-blue-500/50 focus:outline-none"
                                        />
                                        <input
                                            type="number"
                                            min={0}
                                            max={100}
                                            value={editingValues.weight}
                                            onChange={(e) => setEditingValues((v) => ({ ...v, weight: e.target.value }))}
                                            className="rounded-lg border border-white/10 bg-gray-800 px-3 py-2 text-sm text-white placeholder-gray-600 focus:border-blue-500/50 focus:outline-none"
                                        />
                                    </div>
                                    <div className="flex gap-2 justify-end">
                                        <button
                                            onClick={cancelEdit}
                                            disabled={editSaving}
                                            className="px-3 py-1.5 rounded-lg text-xs text-gray-400 hover:text-white border border-white/10 hover:bg-white/5 transition-colors disabled:opacity-50"
                                        >
                                            Cancel
                                        </button>
                                        <button
                                            onClick={saveEdit}
                                            disabled={editSaving}
                                            className="px-3 py-1.5 rounded-lg text-xs font-medium bg-blue-600 text-white hover:bg-blue-500 transition-colors disabled:opacity-50"
                                        >
                                            {editSaving ? "Saving…" : "Save"}
                                        </button>
                                    </div>
                                </div>
                            ) : (
                                <div key={c.id} className="px-6 py-3 flex items-center justify-between gap-4">
                                    <div className="min-w-0">
                                        <p className="text-sm font-medium text-white truncate">{c.name}</p>
                                        {c.description && (
                                            <p className="text-xs text-gray-500 mt-0.5 truncate">{c.description}</p>
                                        )}
                                    </div>
                                    <div className="flex items-center gap-3 flex-shrink-0">
                                        <span className="rounded-full border border-white/10 bg-white/5 px-2 py-0.5 text-xs text-gray-400">
                                            {(c.gradingType ?? "SOFT") === "BINARY" ? "Binary" : "Soft"}
                                        </span>
                                        <span className="text-sm font-semibold text-blue-400">{c.weight}%</span>
                                        <button
                                            onClick={() => startEdit(c)}
                                            className="px-2.5 py-1 rounded-lg text-xs text-gray-400 hover:text-white border border-white/10 hover:bg-white/5 transition-colors"
                                        >
                                            Edit
                                        </button>
                                    </div>
                                </div>
                            )
                        )}
                    </div>
                </div>
            ) : null}

            {/* Add new criteria form */}
            <div className="bg-gray-900 border border-white/8 rounded-2xl overflow-hidden">
                <div className="px-6 py-4 border-b border-white/5 flex items-center gap-3">
                    <div className="w-8 h-8 rounded-lg bg-blue-500/10 border border-blue-500/20 flex items-center justify-center">
                        <svg className="w-4 h-4 text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 4.5v15m7.5-7.5h-15" />
                        </svg>
                    </div>
                    <div>
                        <p className="text-sm font-semibold text-white">Add Criteria</p>
                        <p className="text-xs text-gray-500">Each row is one grading criterion</p>
                    </div>
                </div>

                <div className="p-6 space-y-3">
                    {/* Column headers */}
                    <div className="grid grid-cols-[1fr_110px_2fr_80px_32px] gap-3 px-1">
                        <p className="text-xs font-medium text-gray-500 uppercase tracking-wider">Name</p>
                        <p className="text-xs font-medium text-gray-500 uppercase tracking-wider">Type</p>
                        <p className="text-xs font-medium text-gray-500 uppercase tracking-wider">Description</p>
                        <p className="text-xs font-medium text-gray-500 uppercase tracking-wider">Weight %</p>
                        <span />
                    </div>

                    {/* Rows */}
                    {rows.map((row) => (
                        <div key={row.key} className="grid grid-cols-[1fr_110px_2fr_80px_32px] gap-3 items-center">
                            <input
                                type="text"
                                placeholder="e.g. Technical Feasibility"
                                value={row.name}
                                onChange={(e) => updateRow(row.key, "name", e.target.value)}
                                className="w-full rounded-lg border border-white/10 bg-gray-800 px-3 py-2 text-sm text-white placeholder-gray-600 focus:border-blue-500/50 focus:outline-none"
                            />
                            <select
                                value={row.gradingType}
                                onChange={(e) => updateRow(row.key, "gradingType", e.target.value)}
                                className="w-full rounded-lg border border-white/10 bg-gray-800 px-3 py-2 text-sm text-white focus:border-blue-500/50 focus:outline-none"
                            >
                                <option value="SOFT">Soft</option>
                                <option value="BINARY">Binary</option>
                            </select>
                            <input
                                type="text"
                                placeholder="Optional description"
                                value={row.description}
                                onChange={(e) => updateRow(row.key, "description", e.target.value)}
                                className="w-full rounded-lg border border-white/10 bg-gray-800 px-3 py-2 text-sm text-white placeholder-gray-600 focus:border-blue-500/50 focus:outline-none"
                            />
                            <input
                                type="number"
                                placeholder="0"
                                min={0}
                                max={100}
                                value={row.weight}
                                onChange={(e) => updateRow(row.key, "weight", e.target.value)}
                                className="w-full rounded-lg border border-white/10 bg-gray-800 px-3 py-2 text-sm text-white placeholder-gray-600 focus:border-blue-500/50 focus:outline-none"
                            />
                            <button
                                onClick={() => removeRow(row.key)}
                                disabled={rows.length === 1}
                                className="flex items-center justify-center w-8 h-8 rounded-lg text-gray-600 hover:text-red-400 hover:bg-red-500/10 transition-colors disabled:opacity-30 disabled:cursor-not-allowed"
                            >
                                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M6 18L18 6M6 6l12 12" />
                                </svg>
                            </button>
                        </div>
                    ))}

                    {/* Add row button */}
                    <button
                        onClick={addRow}
                        className="flex items-center gap-2 text-sm text-blue-400 hover:text-blue-300 transition-colors mt-1"
                    >
                        <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 4.5v15m7.5-7.5h-15" />
                        </svg>
                        Add row
                    </button>
                </div>

                {/* Weight summary + submit */}
                <div className="px-6 pb-6 space-y-4">
                    {/* Weight bar */}
                    <div className="space-y-1.5">
                        <div className="flex items-center justify-between">
                            <span className="text-xs text-gray-500">Combined weight</span>
                            <span className={`text-xs font-semibold ${overLimit ? "text-red-400" : combinedWeight === 100 ? "text-green-400" : "text-gray-300"}`}>
                                {combinedWeight}% / 100%
                            </span>
                        </div>
                        <div className="h-1.5 w-full rounded-full bg-gray-800 overflow-hidden">
                            <div
                                className={`h-full rounded-full transition-all ${overLimit ? "bg-red-500" : combinedWeight === 100 ? "bg-green-500" : "bg-blue-500"}`}
                                style={{ width: `${Math.min(combinedWeight, 100)}%` }}
                            />
                        </div>
                        {overLimit && (
                            <p className="text-xs text-red-400">
                                Total weight exceeds 100%. Reduce weights before submitting.
                            </p>
                        )}
                    </div>

                    <button
                        onClick={handleSubmit}
                        disabled={saving || overLimit}
                        className="w-full rounded-xl bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white transition-all hover:bg-blue-500 active:scale-95 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                        {saving ? "Saving…" : "Save Criteria"}
                    </button>
                </div>
            </div>
        </>
    );
}

// ─── Shared UI ────────────────────────────────────────────────────────────────

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
                <p className="text-sm text-gray-500">Only Coordinators can access this page.</p>
            </div>
        </div>
    );
}
