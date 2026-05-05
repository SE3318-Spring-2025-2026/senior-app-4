'use client';

import { useEffect, useState, useCallback } from 'react';
import { useParams, useRouter, useSearchParams } from 'next/navigation';
import { useAuthGuard } from '@/hooks/useAuthGuard';
import Sidebar from '@/components/Sidebar';
import JobProgress from '@/components/ai-validation/JobProgress';
import TriggerModal from '@/components/ai-validation/TriggerModal';
import { getActiveJobForSprint } from '@/lib/ai-validation-api';

type PageState = 'loading' | 'trigger' | 'progress' | 'not_found' | 'forbidden';

export default function SprintAiValidationPage() {
  const authStatus = useAuthGuard('coordinator');
  if (authStatus === 'loading') return <FullSpinner />;
  if (authStatus === 'denied') return <AccessDenied />;
  return <SprintPageLayout />;
}

function SprintPageLayout() {
  const params = useParams<{ sprintId: string }>();
  const router = useRouter();
  const searchParams = useSearchParams();
  const sprintId = params.sprintId;

  const [pageState, setPageState] = useState<PageState>('loading');
  const [jobId, setJobId] = useState<number | null>(() => {
    const raw = searchParams.get('jobId');
    return raw ? Number(raw) : null;
  });
  const [showModal, setShowModal] = useState(false);

  const syncJobIdToUrl = useCallback(
    (id: number | null) => {
      const next = new URLSearchParams(searchParams.toString());
      if (id === null) {
        next.delete('jobId');
      } else {
        next.set('jobId', String(id));
      }
      router.replace(`?${next.toString()}`, { scroll: false });
    },
    [router, searchParams]
  );

  const loadActiveJob = useCallback(async () => {
    setPageState('loading');
    try {
      const active = await getActiveJobForSprint(sprintId);
      if (active) {
        setJobId(active.data.jobId);
        syncJobIdToUrl(active.data.jobId);
        setPageState('progress');
      } else {
        syncJobIdToUrl(null);
        setJobId(null);
        setPageState('trigger');
      }
    } catch (err: unknown) {
      const error = err as Error & { httpStatus?: number };
      if (error.httpStatus === 403) {
        setPageState('forbidden');
      } else if (error.httpStatus === 404) {
        setPageState('not_found');
      } else {
        setPageState('trigger');
      }
    }
  }, [sprintId, syncJobIdToUrl]);

  useEffect(() => {
    const raw = searchParams.get('jobId');
    if (raw) {
      setJobId(Number(raw));
      setPageState('progress');
    } else {
      loadActiveJob();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sprintId]);

  const handleJobStarted = (newJobId: number) => {
    setShowModal(false);
    setJobId(newJobId);
    syncJobIdToUrl(newJobId);
    setPageState('progress');
  };

  const handleJobIdChange = (newJobId: number) => {
    setJobId(newJobId);
    syncJobIdToUrl(newJobId);
  };

  return (
    <div className="min-h-screen bg-gray-950 flex">
      <Sidebar activePage="ai-validation" />
      <main className="flex-1 flex flex-col min-w-0">
        {/* Top bar */}
        <div className="border-b border-white/5 px-8 py-4 flex items-center justify-between">
          <div>
            <h1 className="text-base font-semibold text-white">AI Validation</h1>
            <p className="text-xs text-gray-500 mt-0.5 font-mono">Sprint {sprintId}</p>
          </div>
          {pageState === 'progress' && (
            <button
              onClick={loadActiveJob}
              className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-white/5 border border-white/10 text-xs text-gray-400 hover:text-white hover:bg-white/10 transition-colors"
            >
              <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0l3.181 3.183a8.25 8.25 0 0013.803-3.7M4.031 9.865a8.25 8.25 0 0113.803-3.7l3.181 3.182m0-4.991v4.99" />
              </svg>
              Refresh
            </button>
          )}
        </div>

        <div className="flex-1 p-8">
          <div className="max-w-2xl mx-auto space-y-5">
            {pageState === 'loading' && (
              <div className="bg-gray-900 border border-white/8 rounded-2xl p-12 flex items-center justify-center">
                <svg className="w-5 h-5 animate-spin text-blue-500" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                </svg>
              </div>
            )}

            {pageState === 'not_found' && (
              <div className="bg-gray-900 border border-white/8 rounded-2xl p-10 text-center space-y-3">
                <div className="w-12 h-12 rounded-2xl bg-gray-800 flex items-center justify-center mx-auto">
                  <svg className="w-6 h-6 text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9.879 7.519c1.171-1.025 3.071-1.025 4.242 0 1.172 1.025 1.172 2.687 0 3.712-.203.179-.43.326-.67.442-.745.361-1.45.999-1.45 1.827v.75M21 12a9 9 0 11-18 0 9 9 0 0118 0zm-9 5.25h.008v.008H12v-.008z" />
                  </svg>
                </div>
                <p className="text-sm font-semibold text-white">Sprint not found</p>
                <p className="text-xs text-gray-500">The sprint with ID <span className="font-mono text-gray-400">{sprintId}</span> does not exist.</p>
              </div>
            )}

            {pageState === 'forbidden' && (
              <div className="bg-gray-900 border border-white/8 rounded-2xl p-10 text-center space-y-3">
                <div className="w-12 h-12 rounded-2xl bg-red-500/10 border border-red-500/20 flex items-center justify-center mx-auto">
                  <svg className="w-6 h-6 text-red-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M16.5 10.5V6.75a4.5 4.5 0 10-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 002.25-2.25v-6.75a2.25 2.25 0 00-2.25-2.25H6.75a2.25 2.25 0 00-2.25 2.25v6.75a2.25 2.25 0 002.25 2.25z" />
                  </svg>
                </div>
                <p className="text-sm font-semibold text-white">Access Denied</p>
                <p className="text-xs text-gray-500">You do not have permission to view this sprint&apos;s validation data.</p>
              </div>
            )}

            {pageState === 'trigger' && (
              <div className="bg-gray-900 border border-white/8 rounded-2xl overflow-hidden">
                <div className="px-6 py-4 border-b border-white/5 flex items-center gap-3">
                  <div className="w-8 h-8 rounded-lg bg-blue-500/10 border border-blue-500/20 flex items-center justify-center">
                    <svg className="w-4 h-4 text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09z" />
                    </svg>
                  </div>
                  <div>
                    <p className="text-sm font-semibold text-white">No Active Validation</p>
                    <p className="text-xs text-gray-500">No validation job is running for this sprint</p>
                  </div>
                </div>
                <div className="p-6 space-y-4">
                  <p className="text-sm text-gray-400">
                    Run AI validation to verify code review quality and implementation correctness for all issues in this sprint.
                  </p>
                  <button
                    onClick={() => setShowModal(true)}
                    className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-sm font-medium transition-all shadow-lg shadow-blue-600/20"
                  >
                    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M5.25 5.653c0-.856.917-1.398 1.667-.986l11.54 6.347a1.125 1.125 0 010 1.972l-11.54 6.347a1.125 1.125 0 01-1.667-.986V5.653z" />
                    </svg>
                    Run AI Validation
                  </button>
                </div>
              </div>
            )}

            {pageState === 'progress' && jobId !== null && (
              <JobProgress jobId={jobId} onJobIdChange={handleJobIdChange} />
            )}
          </div>
        </div>
      </main>

      {showModal && (
        <TriggerModal
          sprintId={sprintId}
          onClose={() => setShowModal(false)}
          onJobStarted={handleJobStarted}
        />
      )}
    </div>
  );
}

function FullSpinner() {
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
