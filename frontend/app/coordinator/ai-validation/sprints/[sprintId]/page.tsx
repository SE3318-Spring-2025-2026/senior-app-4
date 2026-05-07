"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useParams, useRouter, useSearchParams } from "next/navigation";
import { AlertTriangle, Loader2, Play, RefreshCw, ShieldAlert } from "lucide-react";
import { useAuthGuard } from "@/hooks/useAuthGuard";
import { getUser } from "@/lib/auth";
import Sidebar from "@/components/Sidebar";
import JobProgress from "@/components/ai-validation/JobProgress";
import TriggerModal from "@/components/ai-validation/TriggerModal";
import SprintResultsDashboard from "@/components/ai-validation/SprintResultsDashboard";
import {
  fetchSprintValidationResults,
  getActiveJobForSprint,
  SprintValidationResultsData,
} from "@/lib/ai-validation-api";
import { fetchGroups } from "@/lib/groups-api";

type PageState = "loading" | "results" | "progress" | "empty" | "forbidden" | "error";

export default function SprintAiValidationPage() {
  const authStatus = useAuthGuard(["coordinator", "professor"]);
  if (authStatus === "loading") return <FullSpinner />;
  if (authStatus === "denied") return <AccessDenied />;
  return <SprintPageLayout />;
}

function SprintPageLayout() {
  const params = useParams<{ sprintId: string }>();
  const router = useRouter();
  const searchParams = useSearchParams();
  const sprintId = params.sprintId;
  const user = getUser();
  const isCoordinator = user?.role === "coordinator";

  const [pageState, setPageState] = useState<PageState>("loading");
  const [results, setResults] = useState<SprintValidationResultsData | null>(null);
  const [allTeams, setAllTeams] = useState<{ id: string; name: string }[]>([]);
  const [teamsForTrigger, setTeamsForTrigger] = useState<{ id: string; name: string }[]>([]);
  const [selectedTeamId, setSelectedTeamId] = useState("");
  const [jobId, setJobId] = useState<number | null>(() => {
    const raw = searchParams.get("jobId");
    return raw ? Number(raw) : null;
  });
  const [showModal, setShowModal] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!isCoordinator) return;
    fetchGroups(0, 200)
      .then((page) => setTeamsForTrigger(page.content.map((g) => ({ id: String(g.id), name: g.groupName }))))
      .catch(() => setTeamsForTrigger([]));
  }, [isCoordinator]);

  const syncJobIdToUrl = useCallback(
    (id: number | null) => {
      const next = new URLSearchParams(searchParams.toString());
      if (id === null) {
        next.delete("jobId");
      } else {
        next.set("jobId", String(id));
      }
      const query = next.toString();
      router.replace(`/coordinator/ai-validation/sprints/${sprintId}${query ? `?${query}` : ""}`, { scroll: false });
    },
    [router, searchParams, sprintId]
  );

  const loadResults = useCallback(
    async (teamId?: string, preserveTeamOptions = false) => {
      setPageState("loading");
      setErrorMessage(null);
      try {
        const response = await fetchSprintValidationResults(sprintId, teamId);
        setResults(response.data);
        if (!teamId || !preserveTeamOptions || allTeams.length === 0) {
          setAllTeams(response.data.teams.map((team) => ({ id: String(team.teamId), name: team.teamName })));
        }
        setPageState(response.data.teams.length > 0 ? "results" : "empty");
      } catch (err: unknown) {
        const error = err as Error & { httpStatus?: number; errorCode?: string };
        if (error.httpStatus === 403) {
          setPageState("forbidden");
        } else if (error.httpStatus === 404) {
          setResults(null);
          setPageState("empty");
        } else {
          setErrorMessage(error.message || "Validation results could not be loaded.");
          setPageState("error");
        }
      }
    },
    [allTeams.length, sprintId]
  );

  const loadPage = useCallback(async () => {
    setPageState("loading");
    setErrorMessage(null);

    try {
      const active = await getActiveJobForSprint(sprintId);
      if (active) {
        setJobId(active.data.jobId);
        syncJobIdToUrl(active.data.jobId);
        setPageState("progress");
        return;
      }
      syncJobIdToUrl(null);
      setJobId(null);
      await loadResults(selectedTeamId || undefined);
    } catch (err: unknown) {
      const error = err as Error & { httpStatus?: number };
      if (error.httpStatus === 403) {
        setPageState("forbidden");
      } else {
        await loadResults(selectedTeamId || undefined);
      }
    }
  }, [loadResults, selectedTeamId, sprintId, syncJobIdToUrl]);

  useEffect(() => {
    const raw = searchParams.get("jobId");
    if (raw) {
      setJobId(Number(raw));
      setPageState("progress");
    } else {
      loadPage();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sprintId]);

  const handleJobStarted = (newJobId: number) => {
    setShowModal(false);
    setJobId(newJobId);
    syncJobIdToUrl(newJobId);
    setPageState("progress");
  };

  const handleJobIdChange = (newJobId: number) => {
    setJobId(newJobId);
    syncJobIdToUrl(newJobId);
  };

  const handleTeamFilterChange = (value: string) => {
    setSelectedTeamId(value);
    loadResults(value || undefined, true);
  };

  return (
    <div className="min-h-screen bg-gray-950 flex">
      <Sidebar activePage="ai-validation-sprints" />
      <main className="flex-1 flex flex-col min-w-0">
        <div className="border-b border-white/5 px-4 sm:px-8 py-4 flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <h1 className="text-base font-semibold text-white">Sprint Results</h1>
            <p className="text-xs text-gray-500 mt-0.5 font-mono">Sprint {sprintId}</p>
          </div>
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
            {isCoordinator && allTeams.length > 0 && pageState === "results" && (
              <label className="flex items-center gap-2 text-xs text-gray-500">
                Team
                <select
                  value={selectedTeamId}
                  onChange={(event) => handleTeamFilterChange(event.target.value)}
                  className="min-w-48 px-3 py-2 bg-gray-900 border border-white/10 rounded-lg text-sm text-white focus:outline-none focus:border-blue-500/60"
                >
                  <option value="">All teams</option>
                  {allTeams.map((team) => (
                    <option key={team.id} value={team.id}>{team.name}</option>
                  ))}
                </select>
              </label>
            )}
            <button
              type="button"
              onClick={loadPage}
              className="inline-flex items-center justify-center gap-2 px-3 py-2 rounded-lg bg-white/5 border border-white/10 text-xs text-gray-300 hover:text-white hover:bg-white/10 transition-colors"
            >
              <RefreshCw className="w-3.5 h-3.5" />
              Refresh
            </button>
            {isCoordinator && (
              <button
                type="button"
                onClick={() => setShowModal(true)}
                className="inline-flex items-center justify-center gap-2 px-4 py-2 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-sm font-medium transition-colors"
              >
                <Play className="w-4 h-4" />
                Run AI Validation
              </button>
            )}
          </div>
        </div>

        <div className="flex-1 p-4 sm:p-8 overflow-y-auto">
          {pageState === "loading" && <ResultsSkeleton />}

          {pageState === "progress" && jobId !== null && (
            <div className="max-w-2xl mx-auto">
              <JobProgress
                jobId={jobId}
                onJobIdChange={handleJobIdChange}
                onTerminal={(status) => {
                  if (status !== "FAILED") loadResults(selectedTeamId || undefined);
                }}
              />
            </div>
          )}

          {pageState === "results" && results && (
            <SprintResultsDashboard results={results} />
          )}

          {pageState === "empty" && (
            <EmptyState isCoordinator={isCoordinator} onRun={() => setShowModal(true)} />
          )}

          {pageState === "forbidden" && <AccessDenied />}

          {pageState === "error" && (
            <div className="max-w-2xl mx-auto rounded-lg border border-red-500/20 bg-red-500/5 p-6 flex items-start gap-4">
              <AlertTriangle className="w-5 h-5 text-red-400 mt-0.5 shrink-0" />
              <div>
                <p className="text-sm font-semibold text-white">Validation results could not be loaded</p>
                <p className="text-sm text-gray-400 mt-1">{errorMessage}</p>
              </div>
            </div>
          )}
        </div>
      </main>

      {showModal && isCoordinator && (
        <TriggerModal
          sprintId={sprintId}
          teams={teamsForTrigger}
          onClose={() => setShowModal(false)}
          onJobStarted={handleJobStarted}
        />
      )}
    </div>
  );
}

function ResultsSkeleton() {
  return (
    <div className="space-y-6" aria-label="Loading validation results">
      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
        {[0, 1, 2].map((item) => (
          <div key={item} className="h-36 rounded-lg bg-gray-900 border border-white/8 animate-pulse" />
        ))}
      </div>
      <div className="rounded-lg bg-gray-900 border border-white/8 p-6 animate-pulse">
        <div className="h-4 w-48 bg-gray-800 rounded" />
        <div className="mt-6 space-y-3">
          {[0, 1, 2, 3, 4].map((item) => (
            <div key={item} className="h-12 bg-gray-800/70 rounded" />
          ))}
        </div>
      </div>
    </div>
  );
}

function EmptyState({
  isCoordinator,
  onRun,
}: {
  readonly isCoordinator: boolean;
  readonly onRun: () => void;
}) {
  return (
    <div className="max-w-xl mx-auto rounded-lg border border-white/8 bg-gray-900 p-10 text-center">
      <div className="w-14 h-14 rounded-lg bg-gray-800 border border-white/5 flex items-center justify-center mx-auto">
        <Loader2 className="w-6 h-6 text-gray-500" />
      </div>
      <h2 className="text-lg font-semibold text-white mt-5">No validation results yet</h2>
      <p className="text-sm text-gray-500 mt-2">
        AI validation has not produced dashboard results for this sprint.
      </p>
      {isCoordinator && (
        <button
          type="button"
          onClick={onRun}
          className="mt-6 inline-flex items-center gap-2 px-5 py-2.5 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-sm font-medium transition-colors"
        >
          <Play className="w-4 h-4" />
          Run AI Validation
        </button>
      )}
    </div>
  );
}

function FullSpinner() {
  return (
    <div className="min-h-screen bg-gray-950 flex items-center justify-center">
      <Loader2 className="w-6 h-6 animate-spin text-blue-500" />
    </div>
  );
}

function AccessDenied() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-950 p-6">
      <div className="max-w-sm text-center space-y-4">
        <div className="w-16 h-16 rounded-lg bg-red-500/10 border border-red-500/20 flex items-center justify-center mx-auto">
          <ShieldAlert className="w-7 h-7 text-red-400" />
        </div>
        <h1 className="text-lg font-semibold text-white">Access Denied</h1>
        <p className="text-sm text-gray-500">You do not have permission to view this validation dashboard.</p>
        <Link
          href="/dashboard"
          className="inline-flex items-center justify-center px-4 py-2 rounded-lg bg-white/5 border border-white/10 text-sm text-gray-300 hover:text-white hover:bg-white/10 transition-colors"
        >
          Back to Dashboard
        </Link>
      </div>
    </div>
  );
}
