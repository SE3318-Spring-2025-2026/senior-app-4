'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthGuard } from '@/hooks/useAuthGuard';
import Sidebar from '@/components/Sidebar';
import { buildHeaders, API_BASE } from '@/lib/api-utils';

interface ActiveSprint {
  sprintId: number;
  sprintName: string;
  startDate: string;
  endDate: string;
  status: string;
}

export default function ValidationSprintsPage() {
  const authStatus = useAuthGuard('coordinator');
  if (authStatus === 'loading') return <FullSpinner />;
  if (authStatus === 'denied') return <AccessDenied />;
  return <SprintsLayout />;
}

function SprintsLayout() {
  const router = useRouter();
  const [activeSprint, setActiveSprint] = useState<ActiveSprint | null>(null);
  const [loading, setLoading] = useState(true);
  const [manualId, setManualId] = useState('');

  useEffect(() => {
    fetch(`${API_BASE}/schedules/active-sprint`, { headers: buildHeaders() })
      .then((r) => (r.ok ? r.json() : null))
      .then((data: ActiveSprint | null) => { setActiveSprint(data); })
      .catch(() => { setActiveSprint(null); })
      .finally(() => setLoading(false));
  }, []);

  const goToSprint = (id: string | number) => {
    router.push(`/coordinator/ai-validation/sprints/${id}`);
  };

  return (
    <div className="min-h-screen bg-gray-950 flex">
      <Sidebar activePage="ai-validation-sprints" />
      <main className="flex-1 flex flex-col min-w-0">
        <div className="border-b border-white/5 px-8 py-4">
          <h1 className="text-base font-semibold text-white">Validation Sprints</h1>
          <p className="text-xs text-gray-500 mt-0.5">Select a sprint to run or monitor AI validation</p>
        </div>

        <div className="flex-1 p-8">
          <div className="max-w-2xl mx-auto space-y-5">
            {/* Active sprint card */}
            <div className="bg-gray-900 border border-white/8 rounded-2xl overflow-hidden">
              <div className="px-6 py-4 border-b border-white/5 flex items-center gap-3">
                <div className="w-8 h-8 rounded-lg bg-green-500/10 border border-green-500/20 flex items-center justify-center">
                  <svg className="w-4 h-4 text-green-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                </div>
                <div>
                  <p className="text-sm font-semibold text-white">Active Sprint</p>
                  <p className="text-xs text-gray-500">Currently running sprint</p>
                </div>
              </div>
              <div className="p-6">
                {loading ? (
                  <div className="flex items-center gap-3">
                    <svg className="w-4 h-4 animate-spin text-blue-500" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                    </svg>
                    <span className="text-sm text-gray-500">Loading active sprint…</span>
                  </div>
                ) : activeSprint ? (
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-sm font-semibold text-white">{activeSprint.sprintName}</p>
                      <p className="text-xs text-gray-500 mt-0.5 font-mono">
                        ID: {activeSprint.sprintId} · {activeSprint.startDate} → {activeSprint.endDate}
                      </p>
                    </div>
                    <button
                      onClick={() => goToSprint(activeSprint.sprintId)}
                      className="flex items-center gap-2 px-4 py-2 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-sm font-medium transition-all shadow-lg shadow-blue-600/20"
                    >
                      <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M5.25 5.653c0-.856.917-1.398 1.667-.986l11.54 6.347a1.125 1.125 0 010 1.972l-11.54 6.347a1.125 1.125 0 01-1.667-.986V5.653z" />
                      </svg>
                      Open
                    </button>
                  </div>
                ) : (
                  <p className="text-sm text-gray-500">No active sprint found.</p>
                )}
              </div>
            </div>

            {/* Manual sprint ID */}
            <div className="bg-gray-900 border border-white/8 rounded-2xl overflow-hidden">
              <div className="px-6 py-4 border-b border-white/5 flex items-center gap-3">
                <div className="w-8 h-8 rounded-lg bg-blue-500/10 border border-blue-500/20 flex items-center justify-center">
                  <svg className="w-4 h-4 text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" />
                  </svg>
                </div>
                <div>
                  <p className="text-sm font-semibold text-white">Go to Sprint by ID</p>
                  <p className="text-xs text-gray-500">Enter a sprint ID manually</p>
                </div>
              </div>
              <div className="p-6">
                <form
                  onSubmit={(e) => { e.preventDefault(); if (manualId.trim()) goToSprint(manualId.trim()); }}
                  className="flex gap-3"
                >
                  <input
                    type="number"
                    min={1}
                    value={manualId}
                    onChange={(e) => setManualId(e.target.value)}
                    placeholder="Sprint ID"
                    className="flex-1 px-4 py-2.5 bg-gray-800 border border-white/10 rounded-xl text-sm text-white placeholder-gray-600 font-mono focus:outline-none focus:border-blue-500/60 focus:ring-1 focus:ring-blue-500/30 transition-all"
                  />
                  <button
                    type="submit"
                    disabled={!manualId.trim()}
                    className="px-5 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-sm font-medium transition-all shadow-lg shadow-blue-600/20 disabled:opacity-50 disabled:cursor-not-allowed disabled:shadow-none"
                  >
                    Go
                  </button>
                </form>
              </div>
            </div>
          </div>
        </div>
      </main>
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
