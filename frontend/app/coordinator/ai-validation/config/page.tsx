'use client';

import React, { useEffect, useState, useCallback } from 'react';
import { useAuthGuard } from '@/hooks/useAuthGuard';
import Sidebar from '@/components/Sidebar';
import {
  fetchValidationConfig,
  updateValidationConfig,
  ValidationConfigData,
} from '@/lib/ai-validation-api';
import { showToast } from '@/components/toast/ToastContext';

const ALLOWED_MODELS = ['gpt-4o', 'gpt-4o-mini', 'gpt-4-turbo'] as const;
type AllowedModel = (typeof ALLOWED_MODELS)[number];

interface FormState {
  reviewWeight: string;
  implementationWeight: string;
  openaiModel: AllowedModel;
  maxDiffLines: string;
  excludedFilePatterns: string;
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
  const authStatus = useAuthGuard('coordinator');
  if (authStatus === 'loading') return <Spinner />;
  if (authStatus === 'denied') return <AccessDenied />;
  return <ConfigLayout />;
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
        <p className="text-sm text-gray-500">Only Coordinators can access this page.</p>
      </div>
    </div>
  );
}

function ConfigLayout() {
  return (
    <div className="min-h-screen bg-gray-950 flex">
      <Sidebar activePage="ai-validation" />
      <main className="flex-1 flex flex-col min-w-0">
        <div className="border-b border-white/5 px-8 py-4 flex items-center justify-between">
          <div>
            <h1 className="text-base font-semibold text-white">AI Validation Configuration</h1>
            <p className="text-xs text-gray-500 mt-0.5">
              Scoring weights, OpenAI model, and diff filter rules
            </p>
          </div>
          <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-white/5 border border-white/10">
            <div className="w-1.5 h-1.5 rounded-full bg-green-400 animate-pulse" />
            <span className="text-xs text-gray-400">System Online</span>
          </div>
        </div>
        <div className="flex-1 p-8">
          <div className="max-w-2xl mx-auto">
            <ConfigForm />
          </div>
        </div>
      </main>
    </div>
  );
}

function ConfigForm() {
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
  const [fieldErrors, setFieldErrors] = useState<{ maxDiffLines?: string; openaiModel?: string; weights?: string }>({});

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

  useEffect(() => { loadConfig(); }, [loadConfig]);

  const reviewWeightNum = Number.parseInt(form.reviewWeight, 10) || 0;
  const implWeightNum = Number.parseInt(form.implementationWeight, 10) || 0;
  const weightSum = reviewWeightNum + implWeightNum;
  const weightsValid = weightSum === 100;

  function handleChange(
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>
  ) {
    const { name, value } = e.target;
    if (name === 'maxDiffLines') setFieldErrors((prev) => ({ ...prev, maxDiffLines: undefined }));
    if (name === 'openaiModel') setFieldErrors((prev) => ({ ...prev, openaiModel: undefined }));
    setForm((prev) => ({ ...prev, [name]: value }));
  }

  async function handleSubmit(e: React.SyntheticEvent<HTMLFormElement>) {
    e.preventDefault();
    const errors: { maxDiffLines?: string; openaiModel?: string; weights?: string } = {};
    if (!weightsValid) {
      errors.weights = 'Review weight + Implementation weight must equal 100.';
    }
    const maxLines = Number.parseInt(form.maxDiffLines, 10);
    if (form.maxDiffLines !== '' && (Number.isNaN(maxLines) || maxLines < 1)) {
      errors.maxDiffLines = 'Must be at least 1.';
    }
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      return;
    }
    setFieldErrors({});
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
      const error = err as Error & { errorCode?: string };
      if (error.errorCode === 'INVALID_WEIGHTS') {
        setFieldErrors((prev) => ({ ...prev, weights: error.message || 'Invalid weights.' }));
      } else if (error.errorCode === 'INVALID_MAX_DIFF_LINES') {
        setFieldErrors((prev) => ({ ...prev, maxDiffLines: error.message || 'Must be at least 1.' }));
      } else if (error.errorCode === 'INVALID_OPENAI_MODEL') {
        setFieldErrors((prev) => ({ ...prev, openaiModel: error.message || 'Invalid model.' }));
      } else {
        showToast(error.message || 'Failed to save configuration.', 'error');
      }
    } finally {
      setSaving(false);
    }
  }

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

  if (loadError) {
    return (
      <div className="bg-gray-900 border border-white/8 rounded-2xl p-8">
        <div className="flex items-start gap-3">
          <div className="w-8 h-8 rounded-lg bg-red-500/10 border border-red-500/20 flex items-center justify-center shrink-0">
            <svg className="w-4 h-4 text-red-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 9v3.75m9.303 3.376c.866 1.5-.217 3.374-1.948 3.374H2.645c-1.73 0-2.813-1.874-1.948-3.374l8.491-14.657c.866-1.5 3.032-1.5 3.898 0l4.217 7.296z" />
            </svg>
          </div>
          <div className="flex-1">
            <p className="text-sm font-semibold text-white">Failed to load configuration</p>
            <p className="text-xs text-gray-500 mt-1">{loadError}</p>
            <button
              onClick={loadConfig}
              className="mt-4 px-4 py-2 rounded-lg bg-white/5 border border-white/10 text-sm text-gray-300 hover:text-white hover:bg-white/10 transition-colors"
            >
              Retry
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="space-y-5">
      {/* Scoring Weights */}
      <div className="bg-gray-900 border border-white/8 rounded-2xl overflow-hidden">
        <div className="px-6 py-4 border-b border-white/5 flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-blue-500/10 border border-blue-500/20 flex items-center justify-center">
            <svg className="w-4 h-4 text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M3 13.125C3 12.504 3.504 12 4.125 12h2.25c.621 0 1.125.504 1.125 1.125v6.75C7.5 20.496 6.996 21 6.375 21h-2.25A1.125 1.125 0 013 19.875v-6.75zM9.75 8.625c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v11.25c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 01-1.125-1.125V8.625zM16.5 4.125c0-.621.504-1.125 1.125-1.125h2.25C20.496 3 21 3.504 21 4.125v15.75c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 01-1.125-1.125V4.125z" />
            </svg>
          </div>
          <div>
            <p className="text-sm font-semibold text-white">Scoring Weights</p>
            <p className="text-xs text-gray-500">Must sum to 100%</p>
          </div>
        </div>
        <div className="p-6 space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <label htmlFor="reviewWeight" className="text-xs font-medium text-gray-400 uppercase tracking-wider">
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
                className="w-full px-4 py-3 bg-gray-800 border border-white/10 rounded-xl text-sm text-white focus:outline-none focus:border-blue-500/60 focus:ring-1 focus:ring-blue-500/30 transition-all"
              />
            </div>
            <div className="space-y-2">
              <label htmlFor="implementationWeight" className="text-xs font-medium text-gray-400 uppercase tracking-wider">
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
                className="w-full px-4 py-3 bg-gray-800 border border-white/10 rounded-xl text-sm text-white focus:outline-none focus:border-blue-500/60 focus:ring-1 focus:ring-blue-500/30 transition-all"
              />
            </div>
          </div>
          <div className={`flex items-center gap-2 text-sm font-medium ${weightsValid ? 'text-green-400' : 'text-red-400'}`}>
            <span className={`inline-block w-2 h-2 rounded-full ${weightsValid ? 'bg-green-400' : 'bg-red-400'}`} />
            {weightsValid
              ? 'Weights sum to 100 ✓'
              : `Weights sum to ${weightSum} — must equal 100`}
          </div>
          {fieldErrors.weights && (
            <p className="text-xs text-red-400 mt-1">{fieldErrors.weights}</p>
          )}
        </div>
      </div>

      {/* OpenAI Model */}
      <div className="bg-gray-900 border border-white/8 rounded-2xl overflow-hidden">
        <div className="px-6 py-4 border-b border-white/5 flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-purple-500/10 border border-purple-500/20 flex items-center justify-center">
            <svg className="w-4 h-4 text-purple-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09z" />
            </svg>
          </div>
          <div>
            <p className="text-sm font-semibold text-white">OpenAI Model</p>
            <p className="text-xs text-gray-500">Model used for validation prompts</p>
          </div>
        </div>
        <div className="p-6">
          <div className="space-y-2">
            <label htmlFor="openaiModel" className="text-xs font-medium text-gray-400 uppercase tracking-wider">
              Model
            </label>
            <select
              id="openaiModel"
              name="openaiModel"
              value={form.openaiModel}
              onChange={handleChange}
              className="w-full px-4 py-3 bg-gray-800 border border-white/10 rounded-xl text-sm text-white focus:outline-none focus:border-blue-500/60 focus:ring-1 focus:ring-blue-500/30 transition-all"
            >
              {ALLOWED_MODELS.map((m) => (
                <option key={m} value={m}>{m}</option>
              ))}
            </select>
            {fieldErrors.openaiModel && (
              <p className="text-xs text-red-400 mt-1">{fieldErrors.openaiModel}</p>
            )}
          </div>
        </div>
      </div>

      {/* Diff Settings */}
      <div className="bg-gray-900 border border-white/8 rounded-2xl overflow-hidden">
        <div className="px-6 py-4 border-b border-white/5 flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-amber-500/10 border border-amber-500/20 flex items-center justify-center">
            <svg className="w-4 h-4 text-amber-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M17.25 6.75L22.5 12l-5.25 5.25m-10.5 0L1.5 12l5.25-5.25m7.5-3l-4.5 16.5" />
            </svg>
          </div>
          <div>
            <p className="text-sm font-semibold text-white">Diff Settings</p>
            <p className="text-xs text-gray-500">File filtering and size limits</p>
          </div>
        </div>
        <div className="p-6 space-y-5">
          <div className="space-y-2">
            <label htmlFor="maxDiffLines" className="text-xs font-medium text-gray-400 uppercase tracking-wider">
              Max Diff Lines
            </label>
            <input
              id="maxDiffLines"
              name="maxDiffLines"
              type="number"
              min={1}
              value={form.maxDiffLines}
              onChange={handleChange}
              className="w-full px-4 py-3 bg-gray-800 border border-white/10 rounded-xl text-sm text-white focus:outline-none focus:border-blue-500/60 focus:ring-1 focus:ring-blue-500/30 transition-all"
            />
            {fieldErrors.maxDiffLines ? (
              <p className="text-xs text-red-400">{fieldErrors.maxDiffLines}</p>
            ) : (
              <p className="text-xs text-gray-600">Maximum diff lines sent to OpenAI per PR. Excess is truncated.</p>
            )}
          </div>
          <div className="space-y-2">
            <label htmlFor="excludedFilePatterns" className="text-xs font-medium text-gray-400 uppercase tracking-wider">
              Excluded File Patterns
            </label>
            <textarea
              id="excludedFilePatterns"
              name="excludedFilePatterns"
              rows={5}
              value={form.excludedFilePatterns}
              onChange={handleChange}
              placeholder={'package-lock.json\n*.min.js\n*.svg'}
              className="w-full px-4 py-3 bg-gray-800 border border-white/10 rounded-xl text-sm text-white font-mono focus:outline-none focus:border-blue-500/60 focus:ring-1 focus:ring-blue-500/30 transition-all resize-y"
            />
            <p className="text-xs text-gray-600">
              One pattern per line. Leave empty to include all files. Supports glob patterns (e.g. <code className="text-blue-400">*.svg</code>).
            </p>
          </div>
        </div>
      </div>

      {/* Actions */}
      <div className="flex justify-end gap-3">
        <button
          type="button"
          onClick={loadConfig}
          disabled={loading || saving}
          className="px-5 py-2.5 rounded-xl border border-white/10 text-sm text-gray-300 hover:text-white hover:bg-white/5 transition-colors disabled:opacity-50"
        >
          Reset
        </button>
        <button
          id="save-config-btn"
          type="submit"
          disabled={saving || !weightsValid}
          className="flex items-center gap-2 px-6 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-sm font-medium transition-all shadow-lg shadow-blue-600/20 disabled:opacity-50 disabled:cursor-not-allowed disabled:shadow-none"
        >
          {saving && (
            <svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
            </svg>
          )}
          {saving ? 'Saving…' : 'Save Configuration'}
        </button>
      </div>
    </form>
  );
}
