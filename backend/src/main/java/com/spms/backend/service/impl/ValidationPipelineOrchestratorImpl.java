package com.spms.backend.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spms.backend.client.GithubApiClient;
import com.spms.backend.client.OpenAiValidationClient;
import com.spms.backend.dto.request.SystemLogCreateRequestDto;
import com.spms.backend.model.*;
import com.spms.backend.repository.*;
import com.spms.backend.service.SystemLogService;
import com.spms.backend.service.ValidationPipelineOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ValidationPipelineOrchestratorImpl implements ValidationPipelineOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(ValidationPipelineOrchestratorImpl.class);

    private static final String STATUS_VALIDATED = "VALIDATED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_SKIPPED = "SKIPPED";

    private final ValidationJobRepository validationJobRepository;
    private final SprintIssueTrackingRepository sprintIssueTrackingRepository;
    private final IssueValidationResultRepository issueValidationResultRepository;
    private final GithubIntegrationRepository githubIntegrationRepository;
    private final ValidationConfigRepository validationConfigRepository;
    private final GithubApiClient githubApiClient;
    private final OpenAiValidationClient openAiValidationClient;
    private final SystemLogService systemLogService;
    private final ObjectMapper objectMapper;

    public ValidationPipelineOrchestratorImpl(
            ValidationJobRepository validationJobRepository,
            SprintIssueTrackingRepository sprintIssueTrackingRepository,
            IssueValidationResultRepository issueValidationResultRepository,
            GithubIntegrationRepository githubIntegrationRepository,
            ValidationConfigRepository validationConfigRepository,
            GithubApiClient githubApiClient,
            OpenAiValidationClient openAiValidationClient,
            SystemLogService systemLogService,
            ObjectMapper objectMapper) {
        this.validationJobRepository = validationJobRepository;
        this.sprintIssueTrackingRepository = sprintIssueTrackingRepository;
        this.issueValidationResultRepository = issueValidationResultRepository;
        this.githubIntegrationRepository = githubIntegrationRepository;
        this.validationConfigRepository = validationConfigRepository;
        this.githubApiClient = githubApiClient;
        this.openAiValidationClient = openAiValidationClient;
        this.systemLogService = systemLogService;
        this.objectMapper = objectMapper;
    }

    @Async
    @Override
    public void runAsync(ValidationJob job, boolean retryOnlyFailedIssues) {
        Long jobId = job.getJobId();
        logger.info("P7 pipeline starting. jobId={}, retry={}", jobId, retryOnlyFailedIssues);

        try {
            markJobInProgress(jobId);

            ValidationConfig config = validationConfigRepository.findById(1L)
                    .orElseThrow(() -> new IllegalStateException("ValidationConfig singleton not found"));

            List<SprintIssueTracking> issues = resolveIssues(job, retryOnlyFailedIssues);

            int completed = 0;
            int failed = 0;

            for (SprintIssueTracking sit : issues) {
                try {
                    processIssue(jobId, sit, config);
                    completed++;
                } catch (Exception ex) {
                    logger.warn("P7 issue failed. jobId={}, issueKey={}: {}", jobId, sit.getIssueKey(), ex.getMessage());
                    saveFailedIssue(jobId, sit, ex.getMessage());
                    failed++;
                }
                updateProgress(jobId, issues.size(), completed, failed);
            }

            finalizeJob(jobId, completed, failed);

        } catch (Exception ex) {
            logger.error("P7 pipeline fatal error. jobId={}: {}", jobId, ex.getMessage(), ex);
            markJobFailed(jobId, ex.getMessage());
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

    // 7.2 → 7.5: full pipeline for a single issue
    private void processIssue(Long jobId, SprintIssueTracking sit, ValidationConfig config) throws JsonProcessingException {
        IssueValidationResult result = new IssueValidationResult();
        ValidationJob jobRef = new ValidationJob();
        jobRef.setJobId(jobId);
        result.setJob(jobRef);
        result.setIssueKey(sit.getIssueKey());
        result.setAssignee(sit.getAssigneeGithubUsername());
        result.setEvaluatedAt(Instant.now());

        // 7.2 — fetch PR details
        if (sit.getPrNumber() == null) {
            result.setValidationStatus(STATUS_SKIPPED);
            saveResult(result);
            return;
        }

        long prNumber = sit.getPrNumber();
        result.setPrNumber(prNumber);
        result.setPrMerged(Boolean.TRUE.equals(sit.getPrMerged()));

        Optional<GithubIntegration> ghOpt = githubIntegrationRepository.findByGroup_Id(sit.getGroup().getId());
        if (ghOpt.isEmpty() || ghOpt.get().getGithubPatEncrypted() == null) {
            throw new IllegalStateException("No GitHub integration for group " + sit.getGroup().getId());
        }

        GithubIntegration gh = ghOpt.get();
        String pat = gh.getGithubPatEncrypted();
        String org = gh.getOrganizationName();
        String repo = gh.getRepositoryName();

        if (org == null || repo == null) {
            throw new IllegalStateException("GitHub org/repo not configured for group " + sit.getGroup().getId());
        }

        result.setPrUrl(String.format("https://github.com/%s/%s/pull/%d", org, repo, prNumber));

        // 7.3 — fetch file diffs and apply filters
        List<Map<String, Object>> files = githubApiClient.fetchPrFiles(org, repo, prNumber, pat);
        String filteredDiff = buildFilteredDiff(files, config);
        boolean diffTruncated = wasTruncated(files, filteredDiff, config.getMaxDiffLines());
        int filesAnalyzed = countAnalyzedFiles(files, config);

        // 7.4 — AI review verification
        List<Map<String, Object>> reviews = githubApiClient.fetchPrReviews(org, repo, prNumber, pat);
        List<Map<String, Object>> comments = githubApiClient.fetchPrReviewComments(org, repo, prNumber, pat);
        String reviewsJson = objectMapper.writeValueAsString(buildReviewPayload(reviews, comments));
        Map<String, Object> reviewResult = openAiValidationClient.verifyReview(config.getOpenaiModel(), reviewsJson);
        applyReviewResult(result, reviewResult);

        // 7.5 — AI implementation validation
        String issueDescription = "Issue: " + sit.getIssueKey();
        Map<String, Object> implResult = openAiValidationClient.validateImplementation(
                config.getOpenaiModel(), issueDescription, filteredDiff, filesAnalyzed, diffTruncated);
        applyImplResult(result, implResult, filesAnalyzed, diffTruncated);

        // 7.6 — compute composite score
        BigDecimal composite = computeComposite(result, config);
        result.setCompositeScore(composite);
        result.setValidationStatus(STATUS_VALIDATED);

        saveResult(result);
    }

    private String buildFilteredDiff(List<Map<String, Object>> files, ValidationConfig config) {
        List<String> patterns = config.getExcludedFilePatterns();
        StringBuilder sb = new StringBuilder();
        int lineCount = 0;
        int maxLines = config.getMaxDiffLines();

        for (Map<String, Object> file : files) {
            String filename = (String) file.getOrDefault("filename", "");
            if (isExcluded(filename, patterns)) continue;

            String patch = (String) file.get("patch");
            if (patch == null) continue;

            String[] lines = patch.split("\n");
            for (String line : lines) {
                if (lineCount >= maxLines) break;
                sb.append(line).append("\n");
                lineCount++;
            }
            if (lineCount >= maxLines) break;
        }
        return sb.toString();
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

    private boolean wasTruncated(List<Map<String, Object>> files, String filteredDiff, int maxLines) {
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
                    "body", r.getOrDefault("body", ""),
                    "state", r.getOrDefault("state", "")
            ));
        }
        for (Map<String, Object> c : comments) {
            Map<String, Object> userMap = (Map<String, Object>) c.get("user");
            payload.add(Map.of(
                    "reviewer", userMap != null ? userMap.getOrDefault("login", "") : "",
                    "body", c.getOrDefault("body", ""),
                    "state", "COMMENT"
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
        result.setReviewAiFeedback((String) ai.get("aiFeedback"));
    }

    private void applyImplResult(IssueValidationResult result, Map<String, Object> ai,
                                 int filesAnalyzed, boolean diffTruncated) throws JsonProcessingException {
        result.setImplScore(toBigDecimal(ai.get("score")));
        result.setImplIsValid(toBool(ai.get("isValid")));
        result.setImplAiFeedback((String) ai.get("aiFeedback"));
        result.setFilesAnalyzed(filesAnalyzed);
        result.setDiffTruncated(diffTruncated);

        Object missing = ai.get("missingRequirements");
        if (missing != null) {
            result.setImplMissingRequirements(objectMapper.writeValueAsString(missing));
        }
        Object coverage = ai.get("coverageAreas");
        if (coverage != null) {
            result.setImplCoverageAreas(objectMapper.writeValueAsString(coverage));
        }
    }

    private BigDecimal computeComposite(IssueValidationResult result, ValidationConfig config) {
        BigDecimal reviewScore = result.getReviewScore() != null ? result.getReviewScore() : BigDecimal.ZERO;
        BigDecimal implScore = result.getImplScore() != null ? result.getImplScore() : BigDecimal.ZERO;
        BigDecimal rw = BigDecimal.valueOf(config.getReviewWeight());
        BigDecimal iw = BigDecimal.valueOf(config.getImplementationWeight());
        return reviewScore.multiply(rw).add(implScore.multiply(iw))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveResult(IssueValidationResult result) {
        issueValidationResultRepository.save(result);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailedIssue(Long jobId, SprintIssueTracking sit, String reason) {
        IssueValidationResult result = new IssueValidationResult();
        ValidationJob jobRef = new ValidationJob();
        jobRef.setJobId(jobId);
        result.setJob(jobRef);
        result.setIssueKey(sit.getIssueKey());
        result.setAssignee(sit.getAssigneeGithubUsername());
        result.setValidationStatus(STATUS_FAILED);
        result.setReviewAiFeedback(reason);
        result.setEvaluatedAt(Instant.now());
        issueValidationResultRepository.save(result);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markJobInProgress(Long jobId) {
        validationJobRepository.findById(jobId).ifPresent(job -> {
            job.setJobStatus(ValidationJobStatus.IN_PROGRESS);
            job.setCurrentStep(ValidationJobStep.LOADING_CONTEXT);
            validationJobRepository.save(job);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateProgress(Long jobId, int total, int completed, int failed) {
        validationJobRepository.findById(jobId).ifPresent(job -> {
            int progress = total > 0 ? (completed + failed) * 100 / total : 100;
            job.setProgressPercentage(progress);
            job.setIssuesCompleted(completed);
            job.setIssuesFailed(failed);
            job.setCurrentStep(ValidationJobStep.STORING_RESULTS);
            validationJobRepository.save(job);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finalizeJob(Long jobId, int completed, int failed) {
        validationJobRepository.findById(jobId).ifPresent(job -> {
            if (failed == 0) {
                job.setJobStatus(ValidationJobStatus.COMPLETED);
            } else if (completed > 0) {
                job.setJobStatus(ValidationJobStatus.PARTIALLY_COMPLETED);
                job.setFailureReason(failed + " issue(s) could not be validated.");
            } else {
                job.setJobStatus(ValidationJobStatus.FAILED);
                job.setFailureReason("All issues failed validation.");
            }
            job.setProgressPercentage(100);
            job.setCompletedAt(Instant.now());
            validationJobRepository.save(job);
        });

        logD9(jobId, completed, failed);
        logger.info("P7 pipeline completed. jobId={}, completed={}, failed={}", jobId, completed, failed);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markJobFailed(Long jobId, String reason) {
        validationJobRepository.findById(jobId).ifPresent(job -> {
            job.setJobStatus(ValidationJobStatus.FAILED);
            job.setFailureReason(reason);
            job.setCompletedAt(Instant.now());
            validationJobRepository.save(job);
        });
        logD9(jobId, 0, -1);
    }

    private void logD9(Long jobId, int completed, int failed) {
        String eventType = failed < 0 ? "P7_VALIDATION_FAILED" : "P7_VALIDATION_COMPLETED";
        String message = "P7 pipeline finished. jobId=" + jobId
                + ", completed=" + completed + ", failed=" + Math.max(failed, 0);
        SystemLogCreateRequestDto req = new SystemLogCreateRequestDto();
        req.setEventType(eventType);
        req.setMessage(message);
        req.setStackTrace(null);
        systemLogService.logEventAsync(req);
    }

    // ── Type helpers ────────────────────────────────────────────────────

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return null;
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
