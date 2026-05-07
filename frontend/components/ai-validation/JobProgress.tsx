'use client';

import { useEffect, useRef, useState } from 'react';
import {
  getJobStatus,
  retryJob,
  JobStatusData,
  JobStatus,
  JobCurrentStep,
} from '@/lib/ai-validation-api';
import { showToast } from '@/components/toast/ToastContext';

const POLL_INTERVAL_MS = 5000;
const TERMINAL_STATES = new Set<JobStatus>(['COMPLETED', 'FAILED', 'PARTIALLY_COMPLETED']);

const STEP_LABELS: Record<NonNullable<JobCurrentStep>, string> = {
  LOADING_CONTEXT: 'Loading sprint context…',
  FETCHING_PR_DETAILS: 'Fetching PR details from GitHub…',
  FETCHING_DIFFS: 'Fetching file diffs…',
  AI_REVIEW_VERIFICATION: 'AI verifying code reviews…',
  AI_IMPLEMENTATION_VALIDATION: 'AI validating implementation…',
  STORING_RESULTS: 'Storing validation results…',
};

const STATUS_CONFIG: Record<JobStatus, { label: string; color: string; dot: string }> = {
  QUEUED: { label: 'Queued', color: 'text-gray-400', dot: 'bg-gray-400' },
  IN_PROGRESS: { label: 'In Progress', color: 'text-blue-400', dot: 'bg-blue-400 animate-pulse' },
  COMPLETED: { label: 'Completed', color: 'text-green-400', dot: 'bg-green-400' },
  PARTIALLY_COMPLETED: { label: 'Partially Completed', color: 'text-amber-400', dot: 'bg-amber-400' },
  FAILED: { label: 'Failed', color: 'text-red-400', dot: 'bg-red-400' },
};

function progressBarColor(status: JobStatus): string {
  if (status === 'FAILED') return 'bg-red-500';
  if (status === 'COMPLETED') return 'bg-green-500';
  if (status === 'PARTIALLY_COMPLETED') return 'bg-amber-500';
  return 'bg-blue-500';
}

function stepLabel(job: JobStatusData): string {
  if (job.currentStep) return STEP_LABELS[job.currentStep];
  if (TERMINAL_STATES.has(job.jobStatus)) return 'Pipeline finished';
  return 'Waiting to start…';
}

interface Props {
  readonly jobId: number;
  readonly onJobIdChange?: (newJobId: number) => void;
  readonly onTerminal?: (status: JobStatus) => void;
}

export default function JobProgress({ jobId, onJobIdChange, onTerminal }: Props) {
  const [job, setJob] = useState<JobStatusData | null>(null);
  const [retrying, setRetrying] = useState(false);
  const pollRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const terminalNotifiedRef = useRef(false);

  const fetchStatus = async (id: number) => {
    try {
      const res = await getJobStatus(id);
      setJob(res.data);
      if (!TERMINAL_STATES.has(res.data.jobStatus)) {
        pollRef.current = setTimeout(() => fetchStatus(id), POLL_INTERVAL_MS);
      } else if (!terminalNotifiedRef.current) {
        terminalNotifiedRef.current = true;
        onTerminal?.(res.data.jobStatus);
      }
    } catch (err: unknown) {
      const error = err as Error & { httpStatus?: number };
      const status = error.httpStatus;
      if (status === 403 || status === 404) {
        return;
      }
      pollRef.current = setTimeout(() => fetchStatus(id), POLL_INTERVAL_MS);
    }
  };

  useEffect(() => {
    if (pollRef.current) clearTimeout(pollRef.current);
    setJob(null);
    terminalNotifiedRef.current = false;
    fetchStatus(jobId);
    return () => { if (pollRef.current) clearTimeout(pollRef.current); };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [jobId]);

  const handleRetry = async () => {
    if (!job) return;
    setRetrying(true);
    try {
      const res = await retryJob(job.jobId);
      onJobIdChange?.(res.data.jobId);
    } catch (err: unknown) {
      showToast(err instanceof Error ? err.message : 'Retry failed.', 'error');
    } finally {
      setRetrying(false);
    }
  };

  if (!job) {
    return (
      <div className="bg-gray-900 border border-white/8 rounded-2xl p-8 flex items-center justify-center">
        <svg className="w-5 h-5 animate-spin text-blue-500" fill="none" viewBox="0 0 24 24">
          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
        </svg>
      </div>
    );
  }

  const statusCfg = STATUS_CONFIG[job.jobStatus];
  const pct = Math.max(0, Math.min(100, job.progressPercentage ?? 0));
  const canRetry = job.jobStatus === 'FAILED' || job.jobStatus === 'PARTIALLY_COMPLETED';

  return (
    <div className="bg-gray-900 border border-white/8 rounded-2xl overflow-hidden">
      {/* Header */}
      <div className="px-6 py-4 border-b border-white/5 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-blue-500/10 border border-blue-500/20 flex items-center justify-center">
            <svg className="w-4 h-4 text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09z" />
            </svg>
          </div>
          <div>
            <p className="text-sm font-semibold text-white">AI Validation Job</p>
            <p className="text-xs text-gray-500 font-mono">#{job.jobId}</p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <span className={`w-2 h-2 rounded-full ${statusCfg.dot}`} />
          <span className={`text-xs font-medium ${statusCfg.color}`}>{statusCfg.label}</span>
        </div>
      </div>

      <div className="p-6 space-y-5">
        {/* Progress bar */}
        <div className="space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-xs text-gray-400">{stepLabel(job)}</span>
            <span className="text-xs font-semibold text-white">{pct}%</span>
          </div>
          <progress
            value={pct}
            max={100}
            aria-valuenow={pct}
            aria-valuemin={0}
            aria-valuemax={100}
            className="sr-only"
          />
          <div className="h-2 bg-gray-800 rounded-full overflow-hidden" aria-hidden>
            <div
              className={`h-full rounded-full transition-all duration-500 ${progressBarColor(job.jobStatus)}`}
              style={{ width: `${pct}%` }}
            />
          </div>
        </div>

        {/* Issue counters */}
        <div className="grid grid-cols-3 gap-3">
          <Counter label="Total" value={job.issuesTotal} color="text-white" />
          <Counter label="Completed" value={job.issuesCompleted} color="text-green-400" />
          <Counter label="Failed" value={job.issuesFailed} color={job.issuesFailed > 0 ? 'text-red-400' : 'text-gray-500'} />
        </div>

        {/* Message */}
        {job.message && (
          <p className="text-xs text-gray-500">{job.message}</p>
        )}

        {/* Failure reason */}
        {job.failureReason && (
          <div className="flex items-start gap-3 p-4 bg-red-500/5 border border-red-500/20 rounded-xl">
            <svg className="w-4 h-4 text-red-400 mt-0.5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 9v3.75m9.303 3.376c.866 1.5-.217 3.374-1.948 3.374H2.645c-1.73 0-2.813-1.874-1.948-3.374l8.491-14.657c.866-1.5 3.032-1.5 3.898 0l4.217 7.296z" />
            </svg>
            <p className="text-xs text-red-300">{job.failureReason}</p>
          </div>
        )}

        {/* Timestamps */}
        <div className="flex items-center gap-4 text-xs text-gray-600">
          <span>Started: {new Date(job.startedAt).toLocaleString()}</span>
          {job.completedAt && (
            <span>Completed: {new Date(job.completedAt).toLocaleString()}</span>
          )}
        </div>

        {/* Retry button */}
        {canRetry && (
          <div className="pt-1">
            <button
              onClick={handleRetry}
              disabled={retrying}
              className="flex items-center gap-2 px-4 py-2 rounded-xl bg-amber-500/10 border border-amber-500/20 text-amber-400 text-sm font-medium hover:bg-amber-500/20 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {retrying ? (
                <svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                </svg>
              ) : (
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0l3.181 3.183a8.25 8.25 0 0013.803-3.7M4.031 9.865a8.25 8.25 0 0113.803-3.7l3.181 3.182m0-4.991v4.99" />
                </svg>
              )}
              {retrying ? 'Retrying…' : 'Retry Failed Issues'}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

function Counter({ label, value, color }: Readonly<{ label: string; value: number; color: string }>) {
  return (
    <div className="bg-gray-800 rounded-xl px-4 py-3 text-center">
      <p className={`text-lg font-bold ${color}`}>{value}</p>
      <p className="text-xs text-gray-500 mt-0.5">{label}</p>
    </div>
  );
}
