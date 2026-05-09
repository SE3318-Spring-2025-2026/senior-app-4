"use client";

import { useEffect, useState } from "react";
import { toast } from "sonner";
import { RefreshCw, Star } from "lucide-react";
import { 
  fetchDemonstrationGrade, 
  saveDemonstrationGrade 
} from "@/lib/final-grading-api";
import { 
  fetchCriteriaForDeliverableType, 
  type GradingCriteriaItem 
} from "@/lib/submissions-api";

const PRES_SOFT_OPTIONS = [
  { label: "A", score: 100 },
  { label: "B", score: 80 },
  { label: "C", score: 60 },
  { label: "D", score: 50 },
  { label: "F", score: 0 },
] as const;

function presLetterToScore(letter: string): number {
  return PRES_SOFT_OPTIONS.find((o) => o.label === letter)?.score ?? 0;
}

interface PresentationGradePanelProps {
  groupId: number;
  onSaved?: () => void;
}

export function PresentationGradePanel({ groupId, onSaved }: PresentationGradePanelProps) {
  const [criteria, setCriteria] = useState<GradingCriteriaItem[]>([]);
  const [inputs, setInputs] = useState<Record<number, string>>({});
  const [savedGrade, setSavedGrade] = useState<number | null>(null);
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    Promise.all([
      fetchCriteriaForDeliverableType("DEMONSTRATION"),
      fetchDemonstrationGrade(groupId),
    ]).then(([crit, existing]) => {
      if (cancelled) return;
      setCriteria(crit);
      // Initialize inputs with empty strings or existing values if we had them
      // Note: Backend doesn't store per-criteria scores for demonstration currently, 
      // just the final grade. So we start fresh or show the saved total.
      setInputs(Object.fromEntries(crit.map((c) => [c.id, ""])));
      setSavedGrade(existing);
    }).catch((err) => {
      console.error("Failed to load demonstration grading data:", err);
    }).finally(() => { 
      if (!cancelled) setLoading(false); 
    });
    return () => { cancelled = true; };
  }, [groupId]);

  const weightedScore: number | null = (() => {
    if (criteria.length === 0) return null;
    if (criteria.some((c) => !inputs[c.id])) return null;
    
    let totalWeight = 0;
    let weightedSum = 0;
    for (const c of criteria) {
      const val = inputs[c.id];
      const score = c.gradingType === "BINARY" ? (val === "100" ? 100 : 0) : presLetterToScore(val);
      totalWeight += c.weight;
      weightedSum += score * c.weight;
    }
    return totalWeight === 0 ? null : weightedSum / totalWeight;
  })();

  const allFilled = criteria.length > 0 && criteria.every((c) => !!inputs[c.id]);

  const handleSave = async () => {
    if (weightedScore === null) { 
      toast.error("Please fill all criteria first."); 
      return; 
    }
    setSaving(true);
    try {
      const grade = parseFloat(weightedScore.toFixed(2));
      await saveDemonstrationGrade(groupId, grade);
      setSavedGrade(grade);
      toast.success("Presentation grade saved successfully.");
      if (onSaved) onSaved();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Failed to save grade.");
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="flex h-32 items-center justify-center rounded-2xl border border-white/8 bg-gray-900/50">
        <RefreshCw className="h-5 w-5 animate-spin text-blue-500" />
      </div>
    );
  }

  return (
    <div className="rounded-2xl border border-white/8 bg-gray-900 p-6 shadow-xl">
      <div className="mb-6 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Star className="h-5 w-5 text-purple-400 fill-purple-400/20" />
          <div>
            <h3 className="text-base font-semibold text-white">Presentation Grade</h3>
            <p className="text-xs text-gray-500">Sunum Puanı Girişi</p>
          </div>
        </div>
        {savedGrade !== null && (
          <div className="text-right">
            <span className="rounded-full border border-purple-500/30 bg-purple-500/10 px-3 py-1 text-sm font-bold text-purple-300">
              {savedGrade.toFixed(1)} / 100
            </span>
            <p className="mt-1 text-[10px] text-gray-500 uppercase tracking-wider">Current Grade</p>
          </div>
        )}
      </div>

      {criteria.length === 0 ? (
        <div className="rounded-xl border border-dashed border-white/10 bg-white/5 px-4 py-8 text-center">
          <p className="text-sm font-medium text-white">No rubric criteria defined</p>
          <p className="mt-1 text-xs text-gray-500">Coordinator needs to add DEMONSTRATION criteria in the rubrics page.</p>
        </div>
      ) : (
        <div className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-1 xl:grid-cols-2">
            {criteria.map((c) => (
              <div key={c.id} className="rounded-xl border border-white/5 bg-white/[0.02] p-4 transition-colors hover:bg-white/[0.04]">
                <div className="mb-3 flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium text-white" title={c.name}>{c.name}</p>
                    {c.description && <p className="mt-0.5 line-clamp-1 text-[11px] text-gray-500" title={c.description}>{c.description}</p>}
                  </div>
                  <div className="flex shrink-0 items-center gap-1.5">
                    <span className="text-[10px] font-bold text-gray-500 uppercase tracking-tighter">{c.weight}%</span>
                    <span className={`rounded-md border px-1.5 py-0.5 text-[10px] font-bold uppercase ${c.gradingType === "BINARY" ? "border-amber-500/20 bg-amber-500/10 text-amber-400" : "border-blue-500/20 bg-blue-500/10 text-blue-400"}`}>
                      {c.gradingType === "BINARY" ? "Bin" : "Soft"}
                    </span>
                  </div>
                </div>
                <div className="flex flex-wrap gap-1.5">
                  {c.gradingType === "BINARY" ? (
                    [{ label: "S", score: "100" }, { label: "F", score: "0" }].map(({ label, score }) => (
                      <button 
                        key={label} 
                        type="button"
                        onClick={() => setInputs((prev) => ({ ...prev, [c.id]: score }))}
                        className={`flex-1 min-w-[3rem] rounded-lg border py-1.5 text-xs font-bold transition-all ${inputs[c.id] === score
                          ? label === "S" ? "border-green-500/40 bg-green-500/20 text-green-300 shadow-[0_0_10px_rgba(34,197,94,0.1)]" : "border-red-500/40 bg-red-500/20 text-red-300 shadow-[0_0_10px_rgba(239,68,68,0.1)]"
                          : "border-white/5 bg-white/5 text-gray-500 hover:bg-white/10 hover:text-gray-300"}`}
                      >
                        {label}
                      </button>
                    ))
                  ) : (
                    PRES_SOFT_OPTIONS.map(({ label }) => (
                      <button 
                        key={label} 
                        type="button"
                        onClick={() => setInputs((prev) => ({ ...prev, [c.id]: label }))}
                        className={`flex-1 min-w-[2.5rem] rounded-lg border py-1.5 text-xs font-bold transition-all ${inputs[c.id] === label
                          ? "border-purple-500/40 bg-purple-500/20 text-purple-200 shadow-[0_0_10px_rgba(168,85,247,0.1)]"
                          : "border-white/5 bg-white/5 text-gray-500 hover:bg-white/10 hover:text-gray-300"}`}
                      >
                        {label}
                      </button>
                    ))
                  )}
                </div>
              </div>
            ))}
          </div>

          <div className="pt-2">
            {weightedScore !== null ? (
              <div className="mb-4 flex items-center justify-between rounded-xl border border-purple-500/20 bg-purple-500/10 px-4 py-3 animate-in fade-in slide-in-from-bottom-2">
                <div className="flex items-center gap-2">
                  <div className="h-1.5 w-1.5 rounded-full bg-purple-400 animate-pulse" />
                  <span className="text-xs font-medium text-purple-200">Weighted score preview</span>
                </div>
                <span className="text-sm font-bold text-purple-300">{weightedScore.toFixed(1)} / 100</span>
              </div>
            ) : (
              <div className="mb-4 rounded-xl border border-white/5 bg-white/[0.02] px-4 py-3 text-center">
                <span className="text-xs text-gray-500 italic">Select all criteria to calculate grade</span>
              </div>
            )}

            <button 
              type="button" 
              onClick={handleSave} 
              disabled={saving || !allFilled}
              className="group relative w-full overflow-hidden rounded-xl bg-purple-600 px-4 py-3 text-sm font-bold text-white transition-all hover:bg-purple-500 hover:shadow-[0_0_20px_rgba(147,51,234,0.3)] disabled:cursor-not-allowed disabled:opacity-50 disabled:shadow-none"
            >
              <div className="relative z-10 flex items-center justify-center gap-2">
                {saving ? (
                  <RefreshCw className="h-4 w-4 animate-spin" />
                ) : (
                  <Star className="h-4 w-4 transition-transform group-hover:scale-110" />
                )}
                {saving ? "Saving Grade..." : (savedGrade !== null ? "Update Presentation Grade" : "Save Presentation Grade")}
              </div>
              <div className="absolute inset-0 translate-y-full bg-gradient-to-t from-white/10 to-transparent transition-transform group-hover:translate-y-0" />
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
