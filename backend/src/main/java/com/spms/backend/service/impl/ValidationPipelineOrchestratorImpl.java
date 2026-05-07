package com.spms.backend.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spms.backend.client.GithubApiClient;
import com.spms.backend.client.OpenAiCallResult;
import com.spms.backend.client.OpenAiValidationClient;
import com.spms.backend.dto.request.SystemLogCreateRequestDto;
import com.spms.backend.exception.P7ApiException;
import com.spms.backend.model.*;
import com.spms.backend.repository.*;
import com.spms.backend.service.SystemLogService;
import com.spms.backend.service.ValidationJobWriteService;
import com.spms.backend.service.ValidationPipelineOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ValidationPipelineOrchestratorImpl implements ValidationPipelineOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(ValidationPipelineOrchestratorImpl.class);

    private static final String STATUS_VALIDATED = "VALIDATED";
    private static final String STATUS_FAILED    = "FAILED";
    private static final String STATUS_SKIPPED   = "SKIPPED";

    private final SprintIssueTrackingRepository    sprintIssueTrackingRepository;
    private final IssueValidationResultRepository  issueValidationResultRepository;
    private final GithubIntegrationRepository      githubIntegrationRepository;
    private final ValidationConfigRepository       validationConfigRepository;
    private final GithubApiClient                  githubApiClient;
    private final OpenAiValidationClient           openAiValidationClient;
    private final SystemLogService                 systemLogService;
    private final ValidationJobWriteService        writeService;
    private final ObjectMapper                     objectMapper;

    public ValidationPipelineOrchestratorImpl(
            SprintIssueTrackingRepository sprintIssueTrackingRepository,
            IssueValidationResultRepository issueValidationResultRepository,
            GithubIntegrationRepository githubIntegrationRepository,
            ValidationConfigRepository validationConfigRepository,
            GithubApiClient githubApiClient,
            OpenAiValidationClient openAiValidationClient,
            SystemLogService systemLogService,
            ValidationJobWriteService writeService,
            ObjectMapper objectMapper) {
        this.sprintIssueTrackingRepository   = sprintIssueTrackingRepository;
        this.issueValidationResultRepository = issueValidationResultRepository;
        this.githubIntegrationRepository     = githubIntegrationRepository;
        this.validationConfigRepository      = validationConfigRepository;
        this.githubApiClient                 = githubApiClient;
        this.openAiValidationClient          = openAiValidationClient;
        this.systemLogService                = systemLogService;
        this.writeService                    = writeService;
        this.objectMapper                    = objectMapper;
    }

    @Async
    @Override
    public void runAsync(ValidationJob job, boolean retryOnlyFailedIssues) {
        Long jobId      = job.getJobId();
        Long sprintId   = job.getSprint() != null   ? job.getSprint().getId()       : null;
        Long teamId     = job.getTeam()   != null   ? job.getTeam().getId()         : null;
        Long parentJobId = job.getParentJob() != null ? job.getParentJob().getJobId() : null;

        logger.info("P7 pipeline starting. jobId={}, retry={}", jobId, retryOnlyFailedIssues);

        try {
            writeService.markJobInProgress(jobId);

            ValidationConfig config = validationConfigRepository.findById(1L)
                    .orElseThrow(() -> new IllegalStateException("ValidationConfig singleton not found"));

            List<SprintIssueTracking> issues = resolveIssues(job, retryOnlyFailedIssues);

            int completed = 0;
            int failed    = 0;

            for (SprintIssueTracking sit : issues) {
                String errorCode = null;
                try {
                    processIssue(jobId, sprintId, teamId, parentJobId, sit, config);
                    completed++;
                } catch (P7ApiException ex) {
                    errorCode = ex.getErrorCode();
                    logger.warn("P7 issue failed [{}]. jobId={}, issueKey={}", errorCode, jobId, sit.getIssueKey());
                    writeService.saveFailedIssue(jobId, sit, errorCode);
                    failed++;
                } catch (Exception ex) {
                    errorCode = "INTERNAL_ERROR";
                    logger.warn("P7 issue failed [INTERNAL_ERROR]. jobId={}, issueKey={}: {}", jobId, sit.getIssueKey(), ex.getMessage());
                    writeService.saveFailedIssue(jobId, sit, errorCode);
                    failed++;
                }
                writeService.updateProgress(jobId, issues.size(), completed, failed);
            }

            writeService.finalizeJob(jobId, completed, failed);
            logD9JobComplete(jobId, parentJobId, sprintId, teamId, completed, failed);
            logger.info("P7 pipeline completed. jobId={}, completed={}, failed={}", jobId, completed, failed);

        } catch (Exception ex) {
            logger.error("P7 pipeline fatal error. jobId={}: {}", jobId, ex.getMessage(), ex);
            writeService.markJobFailed(jobId, ex.getMessage());
            logD9JobComplete(jobId, parentJobId, sprintId, teamId, 0, -1);
        }
    }

    private List<SprintIssueTracking> resolveIssues(ValidationJob job, boolean retryOnlyFailedIssues) {
        List<SprintIssueTracking> all = sprintIssueTrackingRepository.findBySprint_Id(job.getSprint().getId());

        if (job.getTeam() != null) {
            Long teamId = job.getTeam().getId();
            all = all.stream().filter(s -> s.getGroup().getId().equals(teamId)).collect(Collectors.toList());
        }

        if (retryOnlyFailedIssues && job.getParentJob() != null) {
            Set<String> failedKeys = issueValidationResultRepository
                    .findByJob_JobIdAndValidationStatus(job.getParentJob().getJobId(), STATUS_FAILED)
                    .stream().map(IssueValidationResult::getIssueKey).collect(Collectors.toSet());
            all = all.stream().filter(s -> failedKeys.contains(s.getIssueKey())).collect(Collectors.toList());
        }

        return all;
    }

    // 7.2 → 7.6: full pipeline for a single issue
    private void processIssue(Long jobId, Long sprintId, Long teamId, Long parentJobId,
                               SprintIssueTracking sit, ValidationConfig config) throws JsonProcessingException {
        String issueKey = sit.getIssueKey();

        IssueValidationResult result = new IssueValidationResult();
        ValidationJob jobRef = new ValidationJob();
        jobRef.setJobId(jobId);
        result.setJob(jobRef);
        result.setIssueKey(issueKey);
        result.setIssueTitle(sit.getIssueTitle());
        result.setIssueDescription(sit.getIssueDescription());
        result.setAssignee(sit.getAssigneeGithubUsername());
        result.setEvaluatedAt(Instant.now());
        result.setSprintId(sprintId);
        result.setTeamId(teamId);

        // 7.2 — fetch PR details
        writeService.updateStep(jobId, ValidationJobStep.FETCHING_PR_DETAILS);
        long stepStart = System.currentTimeMillis();

        if (sit.getPrNumber() == null) {
            result.setValidationStatus(STATUS_SKIPPED);
            writeService.saveResult(result);
            logD9Subprocess(jobId, parentJobId, sprintId, teamId, issueKey, "7.2",
                    elapsed(stepStart), "SKIPPED", null, null);
            return;
        }

        long prNumber = sit.getPrNumber();
        result.setPrNumber(prNumber);
        result.setPrMerged(Boolean.TRUE.equals(sit.getPrMerged()));

        Optional<GithubIntegration> ghOpt = githubIntegrationRepository.findByGroup_Id(sit.getGroup().getId());
        if (ghOpt.isEmpty() || ghOpt.get().getGithubPatEncrypted() == null) {
            throw new IllegalStateException("No GitHub integration for group " + sit.getGroup().getId());
        }

        GithubIntegration gh  = ghOpt.get();
        // EncryptionConverter already decrypts githubPatEncrypted on entity load — use directly
        String pat  = gh.getGithubPatEncrypted();
        String org  = gh.getOrganizationName();
        String repo = gh.getRepositoryName();

        if (org == null || repo == null) {
            throw new IllegalStateException("GitHub org/repo not configured for group " + sit.getGroup().getId());
        }

        result.setPrUrl(String.format("https://github.com/%s/%s/pull/%d", org, repo, prNumber));
        logD9Subprocess(jobId, parentJobId, sprintId, teamId, issueKey, "7.2",
                elapsed(stepStart), "SUCCESS", null,
                githubMeta(200, 0));

        // 7.3 — fetch file diffs and apply filters
        writeService.updateStep(jobId, ValidationJobStep.FETCHING_DIFFS);
        stepStart = System.currentTimeMillis();
        List<Map<String, Object>> files;
        try {
            files = githubApiClient.fetchPrFiles(org, repo, prNumber, pat);
        } catch (P7ApiException ex) {
            logD9Subprocess(jobId, parentJobId, sprintId, teamId, issueKey, "7.3",
                    elapsed(stepStart), "FAILED", ex.getErrorCode(),
                    githubMeta(429, 0));
            throw ex;
        }
        int preTruncationLines = countTotalDiffLines(files, config);
        String filteredDiff    = buildFilteredDiff(files, config);
        boolean diffTruncated  = wasTruncated(filteredDiff, config.getMaxDiffLines());
        int filesAnalyzed      = countAnalyzedFiles(files, config);
        logD9Subprocess(jobId, parentJobId, sprintId, teamId, issueKey, "7.3",
                elapsed(stepStart), "SUCCESS", null,
                githubMetaWithLines(200, 0, preTruncationLines));

        // 7.4 — AI review verification (1 retry, each attempt logged to D9)
        writeService.updateStep(jobId, ValidationJobStep.AI_REVIEW_VERIFICATION);
        List<Map<String, Object>> reviews  = githubApiClient.fetchPrReviews(org, repo, prNumber, pat);
        List<Map<String, Object>> comments = githubApiClient.fetchPrReviewComments(org, repo, prNumber, pat);
        String reviewsJson = objectMapper.writeValueAsString(buildReviewPayload(reviews, comments));

        OpenAiCallResult reviewAiResult = callOpenAiWithD9Retry(
                jobId, parentJobId, sprintId, teamId, issueKey, "7.4",
                () -> openAiValidationClient.verifyReview(config.getOpenaiModel(), reviewsJson));
        applyReviewResult(result, reviewAiResult.parsed());

        // 7.5 — AI implementation validation (1 retry, each attempt logged to D9)
        writeService.updateStep(jobId, ValidationJobStep.AI_IMPLEMENTATION_VALIDATION);
        String issueDescription = sit.getIssueDescription() != null
                ? sit.getIssueTitle() + "\n\n" + sit.getIssueDescription()
                : (sit.getIssueTitle() != null ? sit.getIssueTitle() : issueKey);
        OpenAiCallResult implAiResult = callOpenAiWithD9Retry(
                jobId, parentJobId, sprintId, teamId, issueKey, "7.5",
                () -> openAiValidationClient.validateImplementation(
                        config.getOpenaiModel(), issueDescription, filteredDiff, filesAnalyzed, diffTruncated));
        applyImplResult(result, implAiResult.parsed(), filesAnalyzed, diffTruncated);

        // 7.6 — compute composite score and persist
        writeService.updateStep(jobId, ValidationJobStep.STORING_RESULTS);
        result.setCompositeScore(computeComposite(result, config));
        result.setValidationStatus(STATUS_VALIDATED);
        writeService.saveResult(result);
    }

    /**
     * Calls the OpenAI supplier once; on failure retries once more.
     * Each attempt (success or failure) is logged independently to D9,
     * with externalCallMeta.retryCount reflecting the attempt index.
     */
    private OpenAiCallResult callOpenAiWithD9Retry(
            Long jobId, Long parentJobId, Long sprintId, Long teamId,
            String issueKey, String subProcess,
            OpenAiSupplier supplier) {

        P7ApiException lastEx = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            long start = System.currentTimeMillis();
            try {
                OpenAiCallResult r = supplier.call();
                logD9Subprocess(jobId, parentJobId, sprintId, teamId, issueKey, subProcess,
                        elapsed(start), "SUCCESS", null,
                        openAiMeta(r.httpStatus(), r.tokenCount(), attempt));
                return r;
            } catch (P7ApiException ex) {
                logD9Subprocess(jobId, parentJobId, sprintId, teamId, issueKey, subProcess,
                        elapsed(start), attempt < 1 ? "RETRY" : "FAILED", ex.getErrorCode(),
                        openAiMeta(500, 0, attempt));
                lastEx = ex;
            }
        }
        throw lastEx;
    }

    @FunctionalInterface
    private interface OpenAiSupplier {
        OpenAiCallResult call();
    }

    // ── Diff helpers ──────────────────────────────────────────────────────

    private String buildFilteredDiff(List<Map<String, Object>> files, ValidationConfig config) {
        List<String> patterns = config.getExcludedFilePatterns();
        StringBuilder sb = new StringBuilder();
        int lineCount = 0;
        int maxLines  = config.getMaxDiffLines();

        for (Map<String, Object> file : files) {
            String filename = (String) file.getOrDefault("filename", "");
            if (isExcluded(filename, patterns)) continue;
            String patch = (String) file.get("patch");
            if (patch == null) continue;
            for (String line : patch.split("\n")) {
                if (lineCount >= maxLines) break;
                sb.append(line).append("\n");
                lineCount++;
            }
            if (lineCount >= maxLines) break;
        }
        return sb.toString();
    }

    private int countTotalDiffLines(List<Map<String, Object>> files, ValidationConfig config) {
        List<String> patterns = config.getExcludedFilePatterns();
        int total = 0;
        for (Map<String, Object> file : files) {
            String filename = (String) file.getOrDefault("filename", "");
            if (isExcluded(filename, patterns)) continue;
            String patch = (String) file.get("patch");
            if (patch != null) total += patch.split("\n").length;
        }
        return total;
    }

    private boolean isExcluded(String filename, List<String> patterns) {
        for (String pattern : patterns) {
            if (pattern.startsWith("*")) {
                if (filename.endsWith(pattern.substring(1))) return true;
            } else if (filename.equals(pattern) || filename.endsWith("/" + pattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean wasTruncated(String filteredDiff, int maxLines) {
        return filteredDiff.split("\n").length >= maxLines;
    }

    private int countAnalyzedFiles(List<Map<String, Object>> files, ValidationConfig config) {
        List<String> patterns = config.getExcludedFilePatterns();
        return (int) files.stream()
                .filter(f -> !isExcluded((String) f.getOrDefault("filename", ""), patterns))
                .filter(f -> f.get("patch") != null)
                .count();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> buildReviewPayload(List<Map<String, Object>> reviews,
                                                          List<Map<String, Object>> comments) {
        List<Map<String, Object>> payload = new ArrayList<>();
        for (Map<String, Object> r : reviews) {
            Map<String, Object> userMap = (Map<String, Object>) r.get("user");
            payload.add(Map.of(
                    "reviewer", userMap != null ? userMap.getOrDefault("login", "") : "",
                    "body",     r.getOrDefault("body", ""),
                    "state",    r.getOrDefault("state", "")
            ));
        }
        for (Map<String, Object> c : comments) {
            Map<String, Object> userMap = (Map<String, Object>) c.get("user");
            payload.add(Map.of(
                    "reviewer", userMap != null ? userMap.getOrDefault("login", "") : "",
                    "body",     c.getOrDefault("body", ""),
                    "state",    "COMMENT"
            ));
        }
        return payload;
    }

    private void applyReviewResult(IssueValidationResult result, Map<String, Object> ai) {
        result.setReviewScore(toBigDecimal(ai.get("score")));
        result.setReviewQuality((String) ai.getOrDefault("reviewQuality", "INSUFFICIENT"));
        result.setReviewerCount(toInt(ai.get("reviewerCount")));
        result.setHasChangeRequests(toBool(ai.get("hasChangeRequests")));
        result.setHasSubstantiveComments(toBool(ai.get("hasSubstantiveComments")));
        Boolean substantive = result.getHasSubstantiveComments();
        Integer reviewers = result.getReviewerCount();
        result.setHasReview((substantive != null && substantive)
                || (reviewers != null && reviewers > 0)
                || (result.getReviewScore() != null && result.getReviewScore().compareTo(BigDecimal.ZERO) > 0));
        result.setReviewAiFeedback((String) ai.get("aiFeedback"));
    }

    private void applyImplResult(IssueValidationResult result, Map<String, Object> ai,
                                 int filesAnalyzed, boolean diffTruncated) throws JsonProcessingException {
        result.setImplScore(toBigDecimal(ai.get("score")));
        result.setImplIsValid(toBool(ai.get("isValid")));
        result.setImplAiFeedback((String) ai.get("aiFeedback"));
        result.setFilesAnalyzed(filesAnalyzed);
        result.setDiffTruncated(diffTruncated);

        Object missing  = ai.get("missingRequirements");
        if (missing  != null) result.setImplMissingRequirements(objectMapper.writeValueAsString(missing));
        Object coverage = ai.get("coverageAreas");
        if (coverage != null) result.setImplCoverageAreas(objectMapper.writeValueAsString(coverage));
    }

    private BigDecimal computeComposite(IssueValidationResult result, ValidationConfig config) {
        BigDecimal rScore = result.getReviewScore() != null ? result.getReviewScore() : BigDecimal.ZERO;
        BigDecimal iScore = result.getImplScore()   != null ? result.getImplScore()   : BigDecimal.ZERO;
        return rScore.multiply(BigDecimal.valueOf(config.getReviewWeight()))
                .add(iScore.multiply(BigDecimal.valueOf(config.getImplementationWeight())))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    // ── D9 Audit Logging ──────────────────────────────────────────────────

    private void logD9Subprocess(Long jobId, Long parentJobId, Long sprintId, Long teamId,
                                  String issueKey, String subProcess, long durationMs,
                                  String outcome, String errorCode,
                                  Map<String, Object> externalCallMeta) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("jobId",            jobId);
        payload.put("parentJobId",      parentJobId);
        payload.put("sprintId",         sprintId);
        payload.put("teamId",           teamId);
        payload.put("issueKey",         issueKey);
        payload.put("subProcess",       subProcess);
        payload.put("durationMs",       durationMs);
        payload.put("outcome",          outcome);
        payload.put("errorCode",        errorCode);
        payload.put("externalCallMeta", externalCallMeta);

        String message;
        try {
            message = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            message = "{\"jobId\":" + jobId + ",\"subProcess\":\"" + subProcess + "\",\"outcome\":\"" + outcome + "\"}";
        }

        SystemLogCreateRequestDto req = new SystemLogCreateRequestDto();
        req.setEventType("P7_SUBPROCESS");
        req.setMessage(message);
        req.setStackTrace(null);
        systemLogService.logEventAsync(req);
    }

    private void logD9JobComplete(Long jobId, Long parentJobId, Long sprintId, Long teamId,
                                   int completed, int failed) {
        String outcome   = failed < 0 ? "FAILED" : (failed == 0 ? "COMPLETED" : "PARTIALLY_COMPLETED");
        String eventType = failed < 0 ? "P7_VALIDATION_FAILED" : "P7_VALIDATION_COMPLETED";

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("jobId",      jobId);
        payload.put("parentJobId", parentJobId);
        payload.put("sprintId",   sprintId);
        payload.put("teamId",     teamId);
        payload.put("outcome",    outcome);
        payload.put("completed",  completed);
        payload.put("failed",     Math.max(failed, 0));

        String message;
        try {
            message = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            message = "{\"jobId\":" + jobId + ",\"outcome\":\"" + outcome + "\"}";
        }

        SystemLogCreateRequestDto req = new SystemLogCreateRequestDto();
        req.setEventType(eventType);
        req.setMessage(message);
        req.setStackTrace(null);
        systemLogService.logEventAsync(req);
    }

    // ── externalCallMeta builders ─────────────────────────────────────────

    private Map<String, Object> githubMeta(int httpStatus, int retryCount) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("provider",   "github");
        m.put("httpStatus", httpStatus);
        m.put("retryCount", retryCount);
        return m;
    }

    private Map<String, Object> githubMetaWithLines(int httpStatus, int retryCount, int preTruncationLines) {
        Map<String, Object> m = githubMeta(httpStatus, retryCount);
        m.put("preTruncationLines", preTruncationLines);
        return m;
    }

    private Map<String, Object> openAiMeta(int httpStatus, int tokenCount, int retryCount) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("provider",   "openai");
        m.put("httpStatus", httpStatus);
        m.put("tokenCount", tokenCount);
        m.put("retryCount", retryCount);
        return m;
    }

    // ── Type helpers ──────────────────────────────────────────────────────

    private long elapsed(long startMs) { return System.currentTimeMillis() - startMs; }

    private BigDecimal toBigDecimal(Object val) {
        if (val instanceof Number n) return BigDecimal.valueOf(n.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        return null;
    }

    private Integer toInt(Object val) {
        if (val instanceof Number n) return n.intValue();
        return null;
    }

    private Boolean toBool(Object val) {
        if (val instanceof Boolean b) return b;
        return null;
    }
}
