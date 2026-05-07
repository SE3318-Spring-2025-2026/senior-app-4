"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import {
  AlertTriangle,
  Check,
  CheckCircle2,
  CircleAlert,
  FileSearch,
  X,
} from "lucide-react";
import {
  fetchIssueValidationDetails,
  IssueValidationDetailData,
  IssueValidationSummary,
  SprintValidationResultsData,
  TeamValidationResult,
} from "@/lib/ai-validation-api";
import { showToast } from "@/components/toast/ToastContext";

interface Props {
  readonly results: SprintValidationResultsData;
}

export default function SprintResultsDashboard({ results }: Props) {
  const [selectedIssue, setSelectedIssue] = useState<IssueValidationSummary | null>(null);
  const [detail, setDetail] = useState<IssueValidationDetailData | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState<string | null>(null);
  const openerRef = useRef<HTMLButtonElement | null>(null);

  const teams = useMemo(() => results.teams ?? [], [results.teams]);
  const totals = useMemo(() => {
    const issueCount = teams.reduce((sum, team) => sum + team.issueCount, 0);
    const validatedCount = teams.reduce(
      (sum, team) => sum + team.issues.filter((issue) => issue.status === "VALIDATED").length,
      0
    );
    return { issueCount, validatedCount };
  }, [teams]);

  const openIssue = (issue: IssueValidationSummary, opener: HTMLButtonElement) => {
    openerRef.current = opener;
    setSelectedIssue(issue);
    setDetail(null);
    setDetailError(null);
    setDetailLoading(true);

    fetchIssueValidationDetails(issue.issueKey)
      .then((response) => {
        setDetail(response.data);
      })
      .catch((err: unknown) => {
        const message = err instanceof Error ? err.message : "Issue details could not be loaded.";
        setDetailError(message);
        showToast(message, "error");
      })
      .finally(() => setDetailLoading(false));
  };

  const closeDrawer = () => {
    setSelectedIssue(null);
    setDetail(null);
    setDetailError(null);
    window.setTimeout(() => openerRef.current?.focus(), 0);
  };

  return (
    <>
      <div className="space-y-6">
        <section className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          {teams.map((team) => (
            <TeamScoreCard key={team.teamId} team={team} />
          ))}
        </section>

        <section className="bg-gray-900 border border-white/8 rounded-lg overflow-hidden">
          <div className="px-6 py-4 border-b border-white/5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h2 className="text-sm font-semibold text-white">Sprint Validation Results</h2>
              <p className="text-xs text-gray-500 mt-1">
                {totals.validatedCount} validated of {totals.issueCount} issues
                {results.evaluatedAt ? ` - evaluated ${new Date(results.evaluatedAt).toLocaleString()}` : ""}
              </p>
            </div>
            <div className="flex items-center gap-2 text-xs text-gray-500">
              <StatusBadge status="VALIDATED" />
              <StatusBadge status="FAILED" />
              <StatusBadge status="SKIPPED" />
            </div>
          </div>

          <div className="divide-y divide-white/5">
            {teams.map((team) => (
              <TeamIssueTable
                key={team.teamId}
                team={team}
                onOpenIssue={openIssue}
              />
            ))}
          </div>
        </section>
      </div>

      {selectedIssue && (
        <IssueDetailDrawer
          issue={selectedIssue}
          detail={detail}
          loading={detailLoading}
          error={detailError}
          onClose={closeDrawer}
        />
      )}
    </>
  );
}

function TeamScoreCard({ team }: { readonly team: TeamValidationResult }) {
  return (
    <article className="bg-gray-900 border border-white/8 rounded-lg p-5">
      <div className="flex items-start justify-between gap-4">
        <div className="min-w-0">
          <p className="text-xs text-gray-500">Team</p>
          <h3 className="text-base font-semibold text-white truncate mt-1">{team.teamName}</h3>
          <p className="text-xs text-gray-600 mt-1">Team #{team.teamId}</p>
        </div>
        <ScorePill score={team.overallSprintScore} />
      </div>
      <div className="mt-5 grid grid-cols-2 gap-3">
        <Metric label="Overall Score" value={formatScore(team.overallSprintScore)} />
        <Metric label="Issues" value={String(team.issueCount)} />
      </div>
    </article>
  );
}

function Metric({ label, value }: { readonly label: string; readonly value: string }) {
  return (
    <div className="bg-gray-800/70 border border-white/5 rounded-lg px-3 py-2">
      <p className="text-xs text-gray-500">{label}</p>
      <p className="text-sm font-semibold text-white mt-1">{value}</p>
    </div>
  );
}

function TeamIssueTable({
  team,
  onOpenIssue,
}: {
  readonly team: TeamValidationResult;
  readonly onOpenIssue: (issue: IssueValidationSummary, opener: HTMLButtonElement) => void;
}) {
  return (
    <div>
      <div className="px-6 py-4 flex items-center justify-between gap-4">
        <div>
          <h3 className="text-sm font-semibold text-white">{team.teamName}</h3>
          <p className="text-xs text-gray-500 mt-1">{team.issueCount} issue{team.issueCount === 1 ? "" : "s"}</p>
        </div>
        <ScorePill score={team.overallSprintScore} />
      </div>

      <div className="overflow-x-auto">
        <table className="w-full min-w-[1120px]">
          <thead>
            <tr className="border-y border-white/5 bg-gray-950/40">
              <ColumnHeader>Issue</ColumnHeader>
              <ColumnHeader>Assignee</ColumnHeader>
              <ColumnHeader>PR</ColumnHeader>
              <ColumnHeader>Merged</ColumnHeader>
              <ColumnHeader>Review</ColumnHeader>
              <ColumnHeader>Implementation</ColumnHeader>
              <ColumnHeader>Composite</ColumnHeader>
              <ColumnHeader>Review Quality</ColumnHeader>
              <ColumnHeader>Status</ColumnHeader>
            </tr>
          </thead>
          <tbody className="divide-y divide-white/5">
            {team.issues.map((issue) => (
              <tr key={issue.issueKey} className="hover:bg-white/[0.03] transition-colors">
                <td className="px-6 py-4 align-top">
                  <button
                    type="button"
                    onClick={(event) => onOpenIssue(issue, event.currentTarget)}
                    className="text-left group"
                  >
                    <span className="text-sm font-semibold text-blue-400 group-hover:text-blue-300">
                      {issue.issueKey}
                    </span>
                    {issue.issueTitle && (
                      <span className="block text-xs text-gray-500 mt-1 max-w-[240px] truncate">
                        {issue.issueTitle}
                      </span>
                    )}
                  </button>
                </td>
                <td className="px-6 py-4 text-sm text-gray-300 align-top">{issue.assignee || "-"}</td>
                <td className="px-6 py-4 text-sm text-gray-300 align-top">
                  {issue.prNumber ? <span className="font-mono">#{issue.prNumber}</span> : "-"}
                </td>
                <td className="px-6 py-4 align-top">
                  <BooleanMark value={issue.prMerged} />
                </td>
                <td className="px-6 py-4 align-top">
                  <ScoreText score={issue.reviewVerificationScore} />
                </td>
                <td className="px-6 py-4 align-top">
                  <ScoreText score={issue.implementationMatchScore} />
                </td>
                <td className="px-6 py-4 align-top">
                  <ScoreText score={issue.compositeScore} strong />
                </td>
                <td className="px-6 py-4 align-top">
                  {issue.reviewQuality ? <ReviewQualityChip quality={issue.reviewQuality} /> : <span className="text-xs text-gray-600">-</span>}
                </td>
                <td className="px-6 py-4 align-top">
                  <StatusBadge status={issue.status} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function ColumnHeader({ children }: { readonly children: React.ReactNode }) {
  return (
    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
      {children}
    </th>
  );
}

function IssueDetailDrawer({
  issue,
  detail,
  loading,
  error,
  onClose,
}: {
  readonly issue: IssueValidationSummary;
  readonly detail: IssueValidationDetailData | null;
  readonly loading: boolean;
  readonly error: string | null;
  readonly onClose: () => void;
}) {
  const drawerRef = useRef<HTMLDivElement | null>(null);
  const closeButtonRef = useRef<HTMLButtonElement | null>(null);

  useEffect(() => {
    drawerRef.current?.focus();
  }, []);

  const handleKeyDown = (event: React.KeyboardEvent<HTMLDivElement>) => {
    if (event.key === "Escape") {
      event.preventDefault();
      onClose();
      return;
    }

    if (event.key !== "Tab" || !drawerRef.current) return;

    const focusable = getFocusableElements(drawerRef.current);
    if (focusable.length === 0) {
      event.preventDefault();
      return;
    }

    const first = focusable[0];
    const last = focusable[focusable.length - 1];

    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first.focus();
    }
  };

  const review = detail?.reviewVerification;
  const implementation = detail?.implementationValidation;

  return (
    <div className="fixed inset-0 z-50 flex justify-end">
      <button
        type="button"
        className="absolute inset-0 bg-black/60 cursor-default"
        aria-label="Close issue details"
        onClick={onClose}
      />
      <aside
        ref={drawerRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="issue-detail-title"
        tabIndex={-1}
        onKeyDown={handleKeyDown}
        className="relative h-full w-full max-w-2xl bg-gray-950 border-l border-white/10 shadow-2xl overflow-y-auto focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500/70"
      >
        <div className="sticky top-0 z-10 bg-gray-950/95 backdrop-blur border-b border-white/8 px-6 py-4 flex items-start justify-between gap-4">
          <div>
            <h2 id="issue-detail-title" className="text-base font-semibold text-white">
              {issue.issueKey}
            </h2>
            <p className="text-xs text-gray-500 mt-1">{issue.issueTitle || detail?.issueDescription || "Issue validation detail"}</p>
          </div>
          <button
            ref={closeButtonRef}
            type="button"
            onClick={onClose}
            className="w-9 h-9 rounded-lg flex items-center justify-center text-gray-500 hover:text-white hover:bg-white/5 transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500/70"
            aria-label="Close"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <div className="p-6 space-y-5">
          {loading && <DrawerSkeleton />}

          {error && (
            <div className="rounded-lg border border-red-500/20 bg-red-500/5 p-4 flex items-start gap-3">
              <CircleAlert className="w-4 h-4 text-red-400 mt-0.5 shrink-0" />
              <p className="text-sm text-red-200">{error}</p>
            </div>
          )}

          {detail && (
            <>
              <section className="rounded-lg border border-white/8 bg-gray-900 p-5">
                <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                  <div>
                    <h3 className="text-sm font-semibold text-white">Issue Context</h3>
                    <p className="text-sm text-gray-400 mt-2">{detail.issueDescription || "No issue description provided."}</p>
                  </div>
                  <StatusBadge status={issue.status} />
                </div>
                <div className="mt-4 grid grid-cols-2 sm:grid-cols-4 gap-3">
                  <Metric label="PR" value={detail.prNumber ? `#${detail.prNumber}` : "-"} />
                  <Metric label="Merged" value={formatBool(issue.prMerged)} />
                  <Metric label="Composite" value={formatScore(issue.compositeScore)} />
                  <Metric label="Evaluated" value={detail.evaluatedAt ? new Date(detail.evaluatedAt).toLocaleDateString() : "-"} />
                </div>
                {detail.prUrl && (
                  <a
                    href={detail.prUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="inline-flex mt-4 text-xs font-medium text-blue-400 hover:text-blue-300"
                  >
                    Open pull request
                  </a>
                )}
              </section>

              <section className="rounded-lg border border-white/8 bg-gray-900 p-5">
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <h3 className="text-sm font-semibold text-white">Review Verification</h3>
                    <p className="text-xs text-gray-500 mt-1">Reviewer activity, review substance, and AI feedback</p>
                  </div>
                  <ScorePill score={review?.score} />
                </div>
                <div className="mt-4 grid grid-cols-2 gap-3">
                  <BooleanCard label="Has review" value={review?.hasReview} />
                  <BooleanCard label="PR merged" value={issue.prMerged} />
                  <BooleanCard label="Change requests" value={review?.hasChangeRequests} />
                  <BooleanCard label="Substantive comments" value={review?.hasSubstantiveComments} />
                </div>
                <div className="mt-4 flex flex-wrap items-center gap-3">
                  <Metric label="Reviewer Count" value={String(review?.reviewerCount ?? 0)} />
                  {review?.reviewQuality && <ReviewQualityChip quality={review.reviewQuality} />}
                </div>
                <Feedback text={review?.aiFeedback} />
              </section>

              <section className="rounded-lg border border-white/8 bg-gray-900 p-5">
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <h3 className="text-sm font-semibold text-white">Implementation Validation</h3>
                    <p className="text-xs text-gray-500 mt-1">Requirement coverage, missing work, and analyzed diff scope</p>
                  </div>
                  <ScorePill score={implementation?.score} />
                </div>

                {implementation?.diffTruncated && (
                  <div className="mt-4 rounded-lg border border-amber-500/25 bg-amber-500/10 p-4 flex items-start gap-3">
                    <AlertTriangle className="w-4 h-4 text-amber-300 mt-0.5 shrink-0" />
                    <p className="text-sm text-amber-100">
                      Diff was truncated before AI analysis. Some changed lines may not be represented in this feedback.
                    </p>
                  </div>
                )}

                <div className="mt-4 grid grid-cols-2 gap-3">
                  <BooleanCard label="Implementation valid" value={implementation?.isValid} />
                  <Metric label="Files Analyzed" value={String(implementation?.filesAnalyzed ?? 0)} />
                </div>

                <div className="mt-5">
                  <h4 className="text-xs font-semibold text-gray-400 uppercase tracking-wider">Coverage Areas</h4>
                  <div className="mt-3 space-y-2">
                    {(implementation?.coverageAreas ?? []).length > 0 ? (
                      implementation?.coverageAreas?.map((area, index) => (
                        <div key={`${area.requirement}-${index}`} className="flex items-start gap-3 rounded-lg bg-gray-800/70 border border-white/5 px-3 py-2">
                          {area.covered ? (
                            <CheckCircle2 className="w-4 h-4 text-green-400 mt-0.5 shrink-0" />
                          ) : (
                            <CircleAlert className="w-4 h-4 text-red-400 mt-0.5 shrink-0" />
                          )}
                          <span className="text-sm text-gray-300">{area.requirement}</span>
                        </div>
                      ))
                    ) : (
                      <p className="text-sm text-gray-500">No coverage areas reported.</p>
                    )}
                  </div>
                </div>

                <div className="mt-5">
                  <h4 className="text-xs font-semibold text-gray-400 uppercase tracking-wider">Missing Requirements</h4>
                  <div className="mt-3 space-y-2">
                    {(implementation?.missingRequirements ?? []).length > 0 ? (
                      implementation?.missingRequirements?.map((requirement, index) => (
                        <div key={`${requirement}-${index}`} className="rounded-lg bg-red-500/5 border border-red-500/20 px-3 py-2 text-sm text-red-100">
                          {requirement}
                        </div>
                      ))
                    ) : (
                      <p className="text-sm text-gray-500">No missing requirements reported.</p>
                    )}
                  </div>
                </div>

                <Feedback text={implementation?.aiFeedback} />
              </section>

              <div className="flex justify-end pb-4">
                <button
                  type="button"
                  onClick={onClose}
                  className="px-4 py-2 rounded-lg border border-white/10 bg-white/5 text-sm font-medium text-gray-300 hover:text-white hover:bg-white/10 transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500/70"
                >
                  Close
                </button>
              </div>
            </>
          )}
        </div>
      </aside>
    </div>
  );
}

function DrawerSkeleton() {
  return (
    <div className="space-y-4" aria-label="Loading issue details">
      {[0, 1, 2].map((item) => (
        <div key={item} className="rounded-lg border border-white/8 bg-gray-900 p-5 animate-pulse">
          <div className="h-4 w-40 bg-gray-800 rounded" />
          <div className="mt-4 h-3 w-full bg-gray-800 rounded" />
          <div className="mt-2 h-3 w-2/3 bg-gray-800 rounded" />
        </div>
      ))}
    </div>
  );
}

function Feedback({ text }: { readonly text?: string | null }) {
  return (
    <div className="mt-5 rounded-lg bg-gray-800/70 border border-white/5 p-4">
      <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider">AI Feedback</p>
      <p className="text-sm text-gray-300 mt-2 whitespace-pre-wrap">{text || "No AI feedback provided."}</p>
    </div>
  );
}

function BooleanCard({ label, value }: { readonly label: string; readonly value?: boolean | null }) {
  const known = typeof value === "boolean";
  return (
    <div className="rounded-lg bg-gray-800/70 border border-white/5 px-3 py-3">
      <p className="text-xs text-gray-500">{label}</p>
      <div className="mt-2 flex items-center gap-2">
        {known && value ? (
          <Check className="w-4 h-4 text-green-400" />
        ) : known ? (
          <X className="w-4 h-4 text-red-400" />
        ) : (
          <FileSearch className="w-4 h-4 text-gray-500" />
        )}
        <span className={`text-sm font-medium ${known ? (value ? "text-green-300" : "text-red-300") : "text-gray-500"}`}>
          {formatBool(value)}
        </span>
      </div>
    </div>
  );
}

function BooleanMark({ value }: { readonly value?: boolean | null }) {
  if (typeof value !== "boolean") return <span className="text-xs text-gray-600">-</span>;
  return (
    <span className={value ? "text-green-400" : "text-red-400"}>
      {value ? "Yes" : "No"}
    </span>
  );
}

function StatusBadge({ status }: { readonly status: IssueValidationSummary["status"] }) {
  const styles = {
    VALIDATED: "bg-green-500/10 text-green-300 border-green-500/25",
    FAILED: "bg-red-500/10 text-red-300 border-red-500/25",
    SKIPPED: "bg-gray-500/10 text-gray-300 border-gray-500/25",
  };

  return (
    <span className={`inline-flex items-center rounded-full border px-2.5 py-1 text-xs font-semibold ${styles[status]}`}>
      {status}
    </span>
  );
}

function ReviewQualityChip({ quality }: { readonly quality: NonNullable<IssueValidationSummary["reviewQuality"]> }) {
  const styles = {
    THOROUGH: "bg-green-500/10 text-green-300 border-green-500/25",
    SUFFICIENT: "bg-blue-500/10 text-blue-300 border-blue-500/25",
    MINIMAL: "bg-amber-500/10 text-amber-300 border-amber-500/25",
    INSUFFICIENT: "bg-red-500/10 text-red-300 border-red-500/25",
  };

  return (
    <span className={`inline-flex items-center rounded-full border px-2.5 py-1 text-xs font-semibold ${styles[quality]}`}>
      {quality}
    </span>
  );
}

function ScorePill({ score }: { readonly score?: number | null }) {
  return (
    <span className={`inline-flex items-center justify-center min-w-16 rounded-lg px-3 py-2 text-sm font-bold ${scoreBgClass(score)}`}>
      {formatScore(score)}
    </span>
  );
}

function ScoreText({ score, strong = false }: { readonly score?: number | null; readonly strong?: boolean }) {
  return (
    <span className={`${strong ? "font-semibold" : "font-medium"} ${scoreTextClass(score)}`}>
      {formatScore(score)}
    </span>
  );
}

function scoreTextClass(score?: number | null): string {
  if (typeof score !== "number") return "text-gray-600";
  if (score >= 80) return "text-green-400";
  if (score >= 60) return "text-amber-400";
  return "text-red-400";
}

function scoreBgClass(score?: number | null): string {
  if (typeof score !== "number") return "bg-gray-800 text-gray-500 border border-white/5";
  if (score >= 80) return "bg-green-500/10 text-green-300 border border-green-500/25";
  if (score >= 60) return "bg-amber-500/10 text-amber-300 border border-amber-500/25";
  return "bg-red-500/10 text-red-300 border border-red-500/25";
}

function formatScore(score?: number | null): string {
  return typeof score === "number" ? score.toFixed(1) : "-";
}

function formatBool(value?: boolean | null): string {
  if (typeof value !== "boolean") return "Unknown";
  return value ? "Yes" : "No";
}

function getFocusableElements(root: HTMLElement): HTMLElement[] {
  return Array.from(
    root.querySelectorAll<HTMLElement>(
      'a[href], button:not([disabled]), textarea:not([disabled]), input:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])'
    )
  ).filter((element) => !element.hasAttribute("disabled") && element.tabIndex !== -1);
}
