'use client';

// TODO(parallel: #298, @you, 2026-05-05): GET/PUT /ai-validation/config frontend page
//   Affects: lib/ai-validation-api.ts, P7-AI-Validation-api.yaml §7.0 Configuration
//   Coordinate before editing: check with team

import React, { useEffect, useState, useCallback } from 'react';
import {
  fetchValidationConfig,
  updateValidationConfig,
  ValidationConfigData,
} from '@/lib/ai-validation-api';
import { showToast } from '@/components/toast/ToastContext';

// Allowed OpenAI models — must match the server-side whitelist exactly.
const ALLOWED_MODELS = ['gpt-4o', 'gpt-4o-mini', 'gpt-4-turbo'] as const;
type AllowedModel = (typeof ALLOWED_MODELS)[number];

interface FormState {
  reviewWeight: string;
  implementationWeight: string;
  openaiModel: AllowedModel;
  maxDiffLines: string;
  excludedFilePatterns: string; // textarea: one pattern per line
}

function configDataToFormState(data: ValidationConfigData): FormState {
  return {
    reviewWeight: String(data.reviewWeight),
    implementationWeight: String(data.implementationWeight),
    openaiModel: (ALLOWED_MODELS.includes(data.openaiModel as AllowedModel)
      ? data.openaiModel
      : 'gpt-4o') as AllowedModel,
    maxDiffLines: String(data.maxDiffLines),
    excludedFilePatterns: (data.excludedFilePatterns ?? []).join('\n'),
  };
}

export default function AiValidationConfigPage() {
  const [form, setForm] = useState<FormState>({
    reviewWeight: '40',
    implementationWeight: '60',
    openaiModel: 'gpt-4o',
    maxDiffLines: '500',
    excludedFilePatterns: '',
  });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);

  // ── Load config on mount ──────────────────────────────────────────────
  const loadConfig = useCallback(async () => {
    setLoading(true);
    setLoadError(null);
    try {
      const response = await fetchValidationConfig();
      setForm(configDataToFormState(response.data));
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to load configuration.';
      setLoadError(msg);
      showToast(msg, 'error');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadConfig();
  }, [loadConfig]);

  // ── Derived: weight sum indicator ─────────────────────────────────────
  const reviewWeightNum = parseInt(form.reviewWeight, 10) || 0;
  const implWeightNum = parseInt(form.implementationWeight, 10) || 0;
  const weightSum = reviewWeightNum + implWeightNum;
  const weightsValid = weightSum === 100;

  // ── Change handlers ───────────────────────────────────────────────────
  function handleChange(
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>
  ) {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  }

  // ── Save ──────────────────────────────────────────────────────────────
  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();

    // Client-side guard (server will also validate)
    if (!weightsValid) {
      showToast('reviewWeight + implementationWeight must equal 100.', 'error');
      return;
    }
    const maxLines = parseInt(form.maxDiffLines, 10);
    if (form.maxDiffLines !== '' && maxLines < 1) {
      showToast('maxDiffLines must be at least 1.', 'error');
      return;
    }

    setSaving(true);
    try {
      const patterns = form.excludedFilePatterns
        .split('\n')
        .map((p) => p.trim())
        .filter((p) => p.length > 0);

      const updated = await updateValidationConfig({
        reviewWeight: reviewWeightNum,
        implementationWeight: implWeightNum,
        openaiModel: form.openaiModel,
        maxDiffLines: maxLines,
        excludedFilePatterns: patterns,
      });

      setForm(configDataToFormState(updated.data));
      showToast('Configuration saved successfully.', 'success');
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to save configuration.';
      showToast(msg, 'error');
    } finally {
      setSaving(false);
    }
  }

  // ── Render ────────────────────────────────────────────────────────────
  if (loading) {
    return (
      <div className="p-8 flex items-center justify-center min-h-[60vh]">
        <div className="flex flex-col items-center gap-4">
          <div className="w-10 h-10 border-4 border-indigo-500 border-t-transparent rounded-full animate-spin" />
          <p className="text-slate-400 text-sm">Loading configuration…</p>
        </div>
      </div>
    );
  }

  if (loadError) {
    return (
      <div className="p-8">
        <div className="rounded-xl bg-red-900/20 border border-red-700/40 p-6 text-red-300 text-sm">
          <p className="font-semibold mb-1">Failed to load configuration</p>
          <p>{loadError}</p>
          <button
            onClick={loadConfig}
            className="mt-4 px-4 py-2 rounded-lg bg-red-700 hover:bg-red-600 transition text-white text-sm"
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="p-6 max-w-2xl mx-auto">
      {/* ── Page header ── */}
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-white tracking-tight">
          AI Validation Configuration
        </h1>
        <p className="mt-1 text-sm text-slate-400">
          Adjust scoring weights, the OpenAI model, and diff filter rules.
          Changes apply to <span className="text-indigo-400 font-medium">future runs only</span> — existing results are not recalculated.
        </p>
      </div>

      <form onSubmit={handleSubmit} noValidate>
        {/* ── Card ── */}
        <div className="rounded-2xl bg-slate-800/60 border border-slate-700/50 backdrop-blur-sm divide-y divide-slate-700/40">

          {/* ── Scoring weights ── */}
          <section className="p-6">
            <h2 className="text-sm font-semibold text-slate-300 uppercase tracking-wider mb-4">
              Scoring Weights
            </h2>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label
                  htmlFor="reviewWeight"
                  className="block text-xs font-medium text-slate-400 mb-1"
                >
                  Review Weight (%)
                </label>
                <input
                  id="reviewWeight"
                  name="reviewWeight"
                  type="number"
                  min={0}
                  max={100}
                  required
                  value={form.reviewWeight}
                  onChange={handleChange}
                  className="w-full rounded-lg bg-slate-900/60 border border-slate-600 px-3 py-2 text-white text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/60 transition"
                />
              </div>
              <div>
                <label
                  htmlFor="implementationWeight"
                  className="block text-xs font-medium text-slate-400 mb-1"
                >
                  Implementation Weight (%)
                </label>
                <input
                  id="implementationWeight"
                  name="implementationWeight"
                  type="number"
                  min={0}
                  max={100}
                  required
                  value={form.implementationWeight}
                  onChange={handleChange}
                  className="w-full rounded-lg bg-slate-900/60 border border-slate-600 px-3 py-2 text-white text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/60 transition"
                />
              </div>
            </div>

            {/* Live weight-sum indicator */}
            <div
              className={`mt-3 flex items-center gap-2 text-sm font-medium transition-colors ${
                weightsValid ? 'text-emerald-400' : 'text-red-400'
              }`}
            >
              <span
                className={`inline-block w-2 h-2 rounded-full ${
                  weightsValid ? 'bg-emerald-400' : 'bg-red-400'
                }`}
              />
              {weightsValid
                ? 'Weights sum to 100 ✓'
                : `Weights sum to ${weightSum} — must equal 100`}
            </div>
          </section>

          {/* ── OpenAI model ── */}
          <section className="p-6">
            <h2 className="text-sm font-semibold text-slate-300 uppercase tracking-wider mb-4">
              OpenAI Model
            </h2>
            <div>
              <label
                htmlFor="openaiModel"
                className="block text-xs font-medium text-slate-400 mb-1"
              >
                Model
              </label>
              <select
                id="openaiModel"
                name="openaiModel"
                value={form.openaiModel}
                onChange={handleChange}
                className="w-full rounded-lg bg-slate-900/60 border border-slate-600 px-3 py-2 text-white text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/60 transition"
              >
                {ALLOWED_MODELS.map((m) => (
                  <option key={m} value={m}>
                    {m}
                  </option>
                ))}
              </select>
            </div>
          </section>

          {/* ── Diff settings ── */}
          <section className="p-6">
            <h2 className="text-sm font-semibold text-slate-300 uppercase tracking-wider mb-4">
              Diff Settings
            </h2>
            <div>
              <label
                htmlFor="maxDiffLines"
                className="block text-xs font-medium text-slate-400 mb-1"
              >
                Max Diff Lines
              </label>
              <input
                id="maxDiffLines"
                name="maxDiffLines"
                type="number"
                min={1}
                value={form.maxDiffLines}
                onChange={handleChange}
                className="w-full rounded-lg bg-slate-900/60 border border-slate-600 px-3 py-2 text-white text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/60 transition"
              />
              <p className="mt-1 text-xs text-slate-500">
                Maximum number of diff lines sent to OpenAI per PR. Excess is truncated.
              </p>
            </div>
          </section>

          {/* ── Excluded file patterns ── */}
          <section className="p-6">
            <h2 className="text-sm font-semibold text-slate-300 uppercase tracking-wider mb-4">
              Excluded File Patterns
            </h2>
            <div>
              <label
                htmlFor="excludedFilePatterns"
                className="block text-xs font-medium text-slate-400 mb-1"
              >
                Patterns (one per line)
              </label>
              <textarea
                id="excludedFilePatterns"
                name="excludedFilePatterns"
                rows={5}
                value={form.excludedFilePatterns}
                onChange={handleChange}
                placeholder="package-lock.json&#10;*.min.js&#10;*.svg"
                className="w-full rounded-lg bg-slate-900/60 border border-slate-600 px-3 py-2 text-white text-sm font-mono focus:outline-none focus:ring-2 focus:ring-indigo-500/60 transition resize-y"
              />
              <p className="mt-1 text-xs text-slate-500">
                Leave empty to include all files. Supports glob patterns (e.g.{' '}
                <code className="text-indigo-400">*.svg</code>).
              </p>
            </div>
          </section>
        </div>

        {/* ── Actions ── */}
        <div className="mt-6 flex justify-end gap-3">
          <button
            type="button"
            onClick={loadConfig}
            disabled={loading || saving}
            className="px-5 py-2.5 rounded-lg border border-slate-600 text-slate-300 text-sm hover:bg-slate-700 transition disabled:opacity-50"
          >
            Reset
          </button>
          <button
            id="save-config-btn"
            type="submit"
            disabled={saving || !weightsValid}
            className="px-6 py-2.5 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-medium transition disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
          >
            {saving && (
              <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
            )}
            {saving ? 'Saving…' : 'Save Configuration'}
          </button>
        </div>
      </form>
    </div>
  );
}
