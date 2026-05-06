'use client';

import { useState } from 'react';
import { triggerValidation } from '@/lib/ai-validation-api';
import { showToast } from '@/components/toast/ToastContext';

interface Team {
  id: string;
  name: string;
}

interface Props {
  readonly sprintId: string;
  readonly teams?: Team[];
  readonly onClose: () => void;
  readonly onJobStarted: (jobId: number) => void;
}

export default function TriggerModal({ sprintId, teams = [], onClose, onJobStarted }: Props) {
  const [teamId, setTeamId] = useState('');
  const [issueKeysRaw, setIssueKeysRaw] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.SyntheticEvent<HTMLFormElement>) => {
    e.preventDefault();
    setLoading(true);
    try {
      const issueKeys = issueKeysRaw
        .split(/[\s,]+/)
        .map((k) => k.trim())
        .filter((k) => k.length > 0);

      const res = await triggerValidation(sprintId, {
        teamId: teamId || undefined,
        issueKeys: issueKeys.length > 0 ? issueKeys : undefined,
      });

      showToast('Validation job queued successfully.', 'success');
      onJobStarted(res.data.jobId);
    } catch (err: unknown) {
      const error = err as Error & { errorCode?: string; existingJobId?: number | null };
      if (error.errorCode === 'VALIDATION_ALREADY_RUNNING') {
        if (error.existingJobId != null) {
          onJobStarted(error.existingJobId);
        }
        showToast('A validation job is already running — resuming progress view.', 'warning');
      } else {
        showToast(error.message || 'Failed to trigger validation.', 'error');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      {/* Backdrop */}
      <button
        type="button"
        className="absolute inset-0 w-full h-full bg-black/60 backdrop-blur-sm cursor-default"
        aria-label="Close modal"
        onClick={onClose}
      />

      {/* Modal */}
      <div className="relative w-full max-w-md bg-gray-900 border border-white/10 rounded-2xl shadow-2xl">
        {/* Header */}
        <div className="px-6 py-4 border-b border-white/5 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-lg bg-blue-500/10 border border-blue-500/20 flex items-center justify-center">
              <svg className="w-4 h-4 text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M5.25 5.653c0-.856.917-1.398 1.667-.986l11.54 6.347a1.125 1.125 0 010 1.972l-11.54 6.347a1.125 1.125 0 01-1.667-.986V5.653z" />
              </svg>
            </div>
            <div>
              <p className="text-sm font-semibold text-white">Run AI Validation</p>
              <p className="text-xs text-gray-500">Start a background validation job</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 flex items-center justify-center rounded-lg text-gray-500 hover:text-white hover:bg-white/5 transition-colors"
            aria-label="Close"
          >
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="p-6 space-y-5">
            {/* Team filter */}
            <div className="space-y-2">
              <label
                htmlFor="trigger-team"
                className="text-xs font-medium text-gray-400 uppercase tracking-wider"
              >
                Team <span className="normal-case text-gray-600">(optional — leave empty for all)</span>
              </label>
              {teams.length > 0 ? (
                <select
                  id="trigger-team"
                  value={teamId}
                  onChange={(e) => setTeamId(e.target.value)}
                  className="w-full px-4 py-3 bg-gray-800 border border-white/10 rounded-xl text-sm text-white focus:outline-none focus:border-blue-500/60 focus:ring-1 focus:ring-blue-500/30 transition-all"
                >
                  <option value="">All teams</option>
                  {teams.map((t) => (
                    <option key={t.id} value={t.id}>{t.name}</option>
                  ))}
                </select>
              ) : (
                <input
                  id="trigger-team"
                  type="text"
                  value={teamId}
                  onChange={(e) => setTeamId(e.target.value)}
                  placeholder="Team UUID (optional)"
                  className="w-full px-4 py-3 bg-gray-800 border border-white/10 rounded-xl text-sm text-white placeholder-gray-600 font-mono focus:outline-none focus:border-blue-500/60 focus:ring-1 focus:ring-blue-500/30 transition-all"
                />
              )}
            </div>

            {/* Issue keys */}
            <div className="space-y-2">
              <label
                htmlFor="trigger-issue-keys"
                className="text-xs font-medium text-gray-400 uppercase tracking-wider"
              >
                Issue Keys <span className="normal-case text-gray-600">(optional — comma or space separated)</span>
              </label>
              <textarea
                id="trigger-issue-keys"
                rows={3}
                value={issueKeysRaw}
                onChange={(e) => setIssueKeysRaw(e.target.value)}
                placeholder={'PROJ-123, PROJ-124\nPROJ-125'}
                className="w-full px-4 py-3 bg-gray-800 border border-white/10 rounded-xl text-sm text-white font-mono placeholder-gray-600 focus:outline-none focus:border-blue-500/60 focus:ring-1 focus:ring-blue-500/30 transition-all resize-none"
              />
              <p className="text-xs text-gray-600">Leave empty to validate all issues in the sprint.</p>
            </div>

            {/* Info note */}
            <div className="flex items-start gap-3 p-3 bg-blue-500/5 border border-blue-500/15 rounded-xl">
              <svg className="w-4 h-4 text-blue-400 mt-0.5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M11.25 11.25l.041-.02a.75.75 0 011.063.852l-.708 2.836a.75.75 0 001.063.853l.041-.021M21 12a9 9 0 11-18 0 9 9 0 0118 0zm-9-3.75h.008v.008H12V8.25z" />
              </svg>
              <p className="text-xs text-gray-400">
                Validation runs in the background and may take a few minutes. You can track progress on this page.
              </p>
            </div>
          </div>

          {/* Footer */}
          <div className="px-6 pb-6 flex gap-3">
            <button
              type="button"
              onClick={onClose}
              disabled={loading}
              className="flex-1 px-4 py-2.5 rounded-xl border border-white/10 text-sm text-gray-300 hover:text-white hover:bg-white/5 transition-colors disabled:opacity-50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="flex-1 flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-sm font-medium transition-all shadow-lg shadow-blue-600/20 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? (
                <>
                  <svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                  </svg>
                  Starting…
                </>
              ) : (
                <>
                  <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M5.25 5.653c0-.856.917-1.398 1.667-.986l11.54 6.347a1.125 1.125 0 010 1.972l-11.54 6.347a1.125 1.125 0 01-1.667-.986V5.653z" />
                  </svg>
                  Run Validation
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
