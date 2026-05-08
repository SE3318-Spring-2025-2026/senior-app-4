"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { Calculator, Check, Plus, RefreshCw, Save, Send, SlidersHorizontal } from "lucide-react";
import { toast } from "sonner";
import Sidebar from "@/components/Sidebar";
import { useAuthGuard } from "@/hooks/useAuthGuard";
import { fetchGroups, type ApiGroupListItem } from "@/lib/groups-api";
import {
  createFinalGradingSprint,
  fetchFinalGradingSprints,
  fetchSprintDeliverableWeights,
  publishFinalGrades,
  recalculateFinalGrades,
  replaceSprintDeliverableWeights,
  updateFinalGradingSprint,
  type FinalDeliverableType,
  type FinalGradeResponse,
  type SprintConfig,
  type SprintDeliverableWeightItem,
} from "@/lib/final-grading-api";

type WeightRow = {
  key: number;
  sprintId: string;
  deliverableType: FinalDeliverableType;
  weight: string;
};

type SprintEdit = {
  sprintName: string;
  startDate: string;
  endDate: string;
  status: string;
  requiredStoryPoints: string;
};

const deliverables: { value: FinalDeliverableType; label: string }[] = [
  { value: "PROPOSAL", label: "Proposal" },
  { value: "STATEMENT_OF_WORK", label: "Statement of Work" },
  { value: "DEMONSTRATION", label: "Demonstration" },
];

let rowCounter = 0;

export default function CoordinatorFinalGradingPage() {
  const authStatus = useAuthGuard("coordinator");
  if (authStatus === "loading") return <Spinner />;
  if (authStatus === "denied") return <AccessDenied />;
  return <FinalGradingWorkspace />;
}

function FinalGradingWorkspace() {
  const [sprints, setSprints] = useState<SprintConfig[]>([]);
  const [groups, setGroups] = useState<ApiGroupListItem[]>([]);
  const [weights, setWeights] = useState<WeightRow[]>([]);
  const [sprintEdits, setSprintEdits] = useState<Record<number, SprintEdit>>({});
  const [newSprint, setNewSprint] = useState<SprintEdit>({
    sprintName: "",
    startDate: "",
    endDate: "",
    status: "Planned",
    requiredStoryPoints: "5",
  });
  const [selectedGroupId, setSelectedGroupId] = useState("");
  const [finalGrades, setFinalGrades] = useState<FinalGradeResponse["data"] | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [sprintData, weightData, groupPage] = await Promise.all([
        fetchFinalGradingSprints(),
        fetchSprintDeliverableWeights(),
        fetchGroups(0, 100, "all", "", "all"),
      ]);
      setSprints(sprintData);
      setGroups(groupPage.content ?? []);
      setSprintEdits(Object.fromEntries(sprintData.map((sprint) => [sprint.id, toSprintEdit(sprint)])));
      setWeights(weightData.length > 0 ? weightData.map(toWeightRow) : [emptyWeightRow(sprintData[0]?.id)]);
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Final grading data could not be loaded.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const totalWeight = useMemo(
    () => weights.reduce((sum, row) => sum + (Number.parseFloat(row.weight) || 0), 0),
    [weights],
  );

  const createSprint = async () => {
    const payload = sprintPayload(newSprint);
    if (!payload) return;
    setSaving("new-sprint");
    try {
      await createFinalGradingSprint(payload);
      toast.success("Sprint saved.");
      setNewSprint({ sprintName: "", startDate: "", endDate: "", status: "Planned", requiredStoryPoints: "5" });
      await load();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Sprint could not be saved.");
    } finally {
      setSaving(null);
    }
  };

  const saveSprint = async (sprintId: number) => {
    const edit = sprintEdits[sprintId];
    const payload = sprintPayload(edit);
    if (!payload) return;
    setSaving(`sprint-${sprintId}`);
    try {
      await updateFinalGradingSprint(sprintId, payload);
      toast.success("Sprint updated.");
      await load();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Sprint could not be updated.");
    } finally {
      setSaving(null);
    }
  };

  const saveWeights = async () => {
    const payload: SprintDeliverableWeightItem[] = [];
    const seen = new Set<string>();
    for (const row of weights) {
      const sprintId = Number(row.sprintId);
      const weight = Number.parseFloat(row.weight);
      if (!sprintId || Number.isNaN(weight) || weight <= 0) {
        toast.error("Every association needs a sprint and a positive weight.");
        return;
      }
      const key = `${sprintId}:${row.deliverableType}`;
      if (seen.has(key)) {
        toast.error("Duplicate sprint and deliverable association.");
        return;
      }
      seen.add(key);
      payload.push({ sprintId, deliverableType: row.deliverableType, weight });
    }
    if (Math.abs(totalWeight - 100) > 0.01) {
      toast.error("Total weight must be exactly 100%.");
      return;
    }
    setSaving("weights");
    try {
      const saved = await replaceSprintDeliverableWeights(payload);
      setWeights(saved.map(toWeightRow));
      toast.success("Deliverable associations saved.");
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Weights could not be saved.");
    } finally {
      setSaving(null);
    }
  };

  const recalculate = async () => {
    const groupId = Number(selectedGroupId);
    if (!groupId) {
      toast.error("Select a group.");
      return;
    }
    setSaving("recalculate");
    try {
      const response = await recalculateFinalGrades(groupId);
      setFinalGrades(response.data);
      toast.success("Final grades recalculated.");
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Final grades could not be recalculated.");
    } finally {
      setSaving(null);
    }
  };

  const publish = async () => {
    const groupId = Number(selectedGroupId);
    if (!groupId) return;
    setSaving("publish");
    try {
      const response = await publishFinalGrades(groupId);
      setFinalGrades(response.data);
      toast.success("Final grades published.");
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Final grades could not be published.");
    } finally {
      setSaving(null);
    }
  };

  return (
    <div className="flex min-h-screen bg-gray-950 text-white">
      <Sidebar activePage="final-grading" />
      <main className="flex min-w-0 flex-1 flex-col">
        <div className="flex items-center justify-between border-b border-white/5 px-8 py-4">
          <div>
            <h1 className="text-base font-semibold">Final Grading</h1>
          </div>
          <button
            type="button"
            onClick={load}
            disabled={loading}
            className="inline-flex items-center gap-2 rounded-lg border border-white/10 px-3 py-1.5 text-xs text-gray-300 hover:bg-white/5 disabled:opacity-50"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${loading ? "animate-spin" : ""}`} />
            Refresh
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-8">
          {loading ? (
            <SpinnerBlock />
          ) : (
            <div className="mx-auto grid max-w-7xl gap-6 xl:grid-cols-[minmax(0,1fr)_420px]">
              <div className="space-y-6">
                <section className="rounded-lg border border-white/10 bg-gray-900">
                  <SectionHeader icon={<SlidersHorizontal className="h-4 w-4" />} title="Sprints" />
                  <div className="overflow-x-auto">
                    <table className="w-full min-w-[760px] text-sm">
                      <thead className="border-b border-white/5 text-left text-xs uppercase text-gray-500">
                        <tr>
                          <th className="px-5 py-3">Name</th>
                          <th className="px-5 py-3">Start</th>
                          <th className="px-5 py-3">End</th>
                          <th className="px-5 py-3">Status</th>
                          <th className="px-5 py-3">Target SP</th>
                          <th className="px-5 py-3 text-right">Save</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-white/5">
                        {sprints.map((sprint) => {
                          const edit = sprintEdits[sprint.id] ?? toSprintEdit(sprint);
                          return (
                            <tr key={sprint.id} className="bg-white/[0.02]">
                              <td className="px-5 py-3">
                                <input className="field min-w-40" value={edit.sprintName} onChange={(e) => updateSprintEdit(setSprintEdits, sprint.id, "sprintName", e.target.value)} />
                              </td>
                              <td className="px-5 py-3">
                                <input className="field" type="date" value={edit.startDate} onChange={(e) => updateSprintEdit(setSprintEdits, sprint.id, "startDate", e.target.value)} />
                              </td>
                              <td className="px-5 py-3">
                                <input className="field" type="date" value={edit.endDate} onChange={(e) => updateSprintEdit(setSprintEdits, sprint.id, "endDate", e.target.value)} />
                              </td>
                              <td className="px-5 py-3">
                                <select className="field" value={edit.status} onChange={(e) => updateSprintEdit(setSprintEdits, sprint.id, "status", e.target.value)}>
                                  <option>Planned</option>
                                  <option>Active</option>
                                  <option>Completed</option>
                                </select>
                              </td>
                              <td className="px-5 py-3">
                                <input className="field w-24" type="number" min={0} value={edit.requiredStoryPoints} onChange={(e) => updateSprintEdit(setSprintEdits, sprint.id, "requiredStoryPoints", e.target.value)} />
                              </td>
                              <td className="px-5 py-3 text-right">
                                <button className="iconButton" onClick={() => saveSprint(sprint.id)} disabled={saving === `sprint-${sprint.id}`}>
                                  <Save className="h-4 w-4" />
                                </button>
                              </td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  </div>
                  <div className="grid gap-3 border-t border-white/5 p-5 md:grid-cols-[1.2fr_repeat(4,1fr)_auto]">
                    <input className="field" placeholder="Sprint name" value={newSprint.sprintName} onChange={(e) => setNewSprint((prev) => ({ ...prev, sprintName: e.target.value }))} />
                    <input className="field" type="date" value={newSprint.startDate} onChange={(e) => setNewSprint((prev) => ({ ...prev, startDate: e.target.value }))} />
                    <input className="field" type="date" value={newSprint.endDate} onChange={(e) => setNewSprint((prev) => ({ ...prev, endDate: e.target.value }))} />
                    <select className="field" value={newSprint.status} onChange={(e) => setNewSprint((prev) => ({ ...prev, status: e.target.value }))}>
                      <option>Planned</option>
                      <option>Active</option>
                      <option>Completed</option>
                    </select>
                    <input className="field" type="number" min={0} value={newSprint.requiredStoryPoints} onChange={(e) => setNewSprint((prev) => ({ ...prev, requiredStoryPoints: e.target.value }))} />
                    <button className="primaryButton" onClick={createSprint} disabled={saving === "new-sprint"}>
                      <Plus className="h-4 w-4" />
                      Add
                    </button>
                  </div>
                </section>

                <section className="rounded-lg border border-white/10 bg-gray-900">
                  <SectionHeader icon={<Calculator className="h-4 w-4" />} title="Sprint Associations" />
                  <div className="space-y-3 p-5">
                    {weights.map((row) => (
                      <div key={row.key} className="grid gap-3 md:grid-cols-[1fr_1fr_120px_auto]">
                        <select className="field" value={row.sprintId} onChange={(e) => updateWeightRow(setWeights, row.key, "sprintId", e.target.value)}>
                          <option value="">Sprint</option>
                          {sprints.map((sprint) => (
                            <option key={sprint.id} value={sprint.id}>{sprint.sprintName}</option>
                          ))}
                        </select>
                        <select className="field" value={row.deliverableType} onChange={(e) => updateWeightRow(setWeights, row.key, "deliverableType", e.target.value as FinalDeliverableType)}>
                          {deliverables.map((deliverable) => (
                            <option key={deliverable.value} value={deliverable.value}>{deliverable.label}</option>
                          ))}
                        </select>
                        <input className="field" type="number" min={0} max={100} step={0.01} value={row.weight} onChange={(e) => updateWeightRow(setWeights, row.key, "weight", e.target.value)} />
                        <button className="secondaryButton" onClick={() => setWeights((prev) => prev.filter((item) => item.key !== row.key))} disabled={weights.length === 1}>
                          Remove
                        </button>
                      </div>
                    ))}
                    <div className="flex flex-wrap items-center justify-between gap-3 border-t border-white/5 pt-4">
                      <button className="secondaryButton" onClick={() => setWeights((prev) => [...prev, emptyWeightRow(sprints[0]?.id)])}>
                        <Plus className="h-4 w-4" />
                        Row
                      </button>
                      <div className="flex items-center gap-3">
                        <span className={`text-sm font-semibold ${Math.abs(totalWeight - 100) <= 0.01 ? "text-green-300" : "text-amber-300"}`}>
                          {totalWeight.toFixed(2)}%
                        </span>
                        <button className="primaryButton" onClick={saveWeights} disabled={saving === "weights"}>
                          <Check className="h-4 w-4" />
                          Save
                        </button>
                      </div>
                    </div>
                  </div>
                </section>
              </div>

              <section className="rounded-lg border border-white/10 bg-gray-900">
                <SectionHeader icon={<Send className="h-4 w-4" />} title="Publish" />
                <div className="space-y-4 p-5">
                  <select className="field w-full" value={selectedGroupId} onChange={(e) => { setSelectedGroupId(e.target.value); setFinalGrades(null); }}>
                    <option value="">Group</option>
                    {groups.map((group) => (
                      <option key={group.id} value={group.id}>{group.groupName}</option>
                    ))}
                  </select>
                  <div className="grid grid-cols-2 gap-3">
                    <button className="primaryButton justify-center" onClick={recalculate} disabled={saving === "recalculate" || !selectedGroupId}>
                      Recalculate
                    </button>
                    <button className="secondaryButton justify-center" onClick={publish} disabled={saving === "publish" || !finalGrades}>
                      Publish
                    </button>
                  </div>

                  {finalGrades && <FinalGradePreview data={finalGrades} />}
                </div>
              </section>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}

function FinalGradePreview({ data }: { data: FinalGradeResponse["data"] }) {
  return (
    <div className="space-y-4 border-t border-white/5 pt-4">
      <div className="grid grid-cols-2 gap-3">
        <Metric label="Team" value={formatNumber(data.teamGrade)} />
        <Metric label="Published" value={data.published ? "Yes" : "No"} />
      </div>
      {data.deliverables.length > 0 && (
        <div className="overflow-hidden rounded-lg border border-white/10">
          <table className="w-full text-xs">
            <thead className="border-b border-white/5 text-left text-gray-500">
              <tr>
                <th className="px-3 py-2">Deliverable</th>
                <th className="px-3 py-2">Scalar</th>
                <th className="px-3 py-2">Contribution</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5">
              {data.deliverables.map((item) => (
                <tr key={item.deliverableType}>
                  <td className="px-3 py-2 text-white">{formatDeliverable(item.deliverableType)}</td>
                  <td className="px-3 py-2 text-gray-300">{formatNumber(item.scalar)}</td>
                  <td className="px-3 py-2 text-gray-300">{formatNumber(item.contribution)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <div className="space-y-2">
        {data.students.map((student) => (
          <div key={student.userId} className="rounded-lg border border-white/10 bg-white/[0.03] px-3 py-2">
            <div className="flex items-center justify-between gap-3">
              <p className="truncate text-sm font-medium text-white">{student.fullName}</p>
              <p className="text-sm font-semibold text-blue-300">{formatNumber(student.finalGrade)}</p>
            </div>
            <p className="mt-1 text-xs text-gray-500">{student.githubUsername ?? "No GitHub"} - {(student.spRatio * 100).toFixed(0)}%</p>
          </div>
        ))}
      </div>
    </div>
  );
}

function SectionHeader({ icon, title }: { icon: React.ReactNode; title: string }) {
  return (
    <div className="flex items-center gap-3 border-b border-white/5 px-5 py-4">
      <div className="flex h-8 w-8 items-center justify-center rounded-lg border border-blue-500/20 bg-blue-500/10 text-blue-300">
        {icon}
      </div>
      <h2 className="text-sm font-semibold">{title}</h2>
    </div>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border border-white/10 bg-white/[0.03] px-4 py-3">
      <p className="text-xs text-gray-500">{label}</p>
      <p className="mt-1 text-lg font-semibold text-white">{value}</p>
    </div>
  );
}

function toSprintEdit(sprint: SprintConfig): SprintEdit {
  return {
    sprintName: sprint.sprintName,
    startDate: sprint.startDate,
    endDate: sprint.endDate,
    status: sprint.status,
    requiredStoryPoints: String(sprint.requiredStoryPoints ?? 0),
  };
}

function sprintPayload(edit: SprintEdit | undefined) {
  if (!edit?.sprintName.trim() || !edit.startDate || !edit.endDate) {
    toast.error("Sprint name, start date, and end date are required.");
    return null;
  }
  const requiredStoryPoints = Number(edit.requiredStoryPoints);
  if (!Number.isFinite(requiredStoryPoints) || requiredStoryPoints < 0) {
    toast.error("Target story points must be zero or higher.");
    return null;
  }
  return {
    sprintName: edit.sprintName.trim(),
    startDate: edit.startDate,
    endDate: edit.endDate,
    status: edit.status,
    requiredStoryPoints,
  };
}

function updateSprintEdit(
  setSprintEdits: React.Dispatch<React.SetStateAction<Record<number, SprintEdit>>>,
  sprintId: number,
  field: keyof SprintEdit,
  value: string,
) {
  setSprintEdits((prev) => ({
    ...prev,
    [sprintId]: {
      ...prev[sprintId],
      [field]: value,
    },
  }));
}

function emptyWeightRow(defaultSprintId?: number): WeightRow {
  return {
    key: ++rowCounter,
    sprintId: defaultSprintId ? String(defaultSprintId) : "",
    deliverableType: "PROPOSAL",
    weight: "",
  };
}

function toWeightRow(item: SprintDeliverableWeightItem): WeightRow {
  return {
    key: ++rowCounter,
    sprintId: String(item.sprintId),
    deliverableType: item.deliverableType,
    weight: String(item.weight),
  };
}

function updateWeightRow(
  setWeights: React.Dispatch<React.SetStateAction<WeightRow[]>>,
  key: number,
  field: keyof Omit<WeightRow, "key">,
  value: string,
) {
  setWeights((prev) => prev.map((row) => (row.key === key ? { ...row, [field]: value } : row)));
}

function formatDeliverable(value: string) {
  return deliverables.find((item) => item.value === value)?.label ?? value.replaceAll("_", " ");
}

function formatNumber(value: number | null | undefined) {
  return typeof value === "number" ? value.toFixed(2) : "-";
}

function SpinnerBlock() {
  return (
    <div className="flex h-64 items-center justify-center rounded-lg border border-white/10 bg-gray-900">
      <RefreshCw className="h-6 w-6 animate-spin text-blue-400" />
    </div>
  );
}

function Spinner() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-950">
      <RefreshCw className="h-6 w-6 animate-spin text-blue-400" />
    </div>
  );
}

function AccessDenied() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-950 px-6">
      <div className="rounded-lg border border-red-500/20 bg-red-500/10 p-8 text-center text-white">
        <h1 className="text-lg font-semibold">Access restricted</h1>
        <p className="mt-2 text-sm text-red-100/80">Only coordinators can access final grading.</p>
      </div>
    </div>
  );
}
