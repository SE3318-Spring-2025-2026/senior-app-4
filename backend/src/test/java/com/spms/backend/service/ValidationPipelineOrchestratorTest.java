package com.spms.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spms.backend.client.GithubApiClient;
import com.spms.backend.client.OpenAiCallResult;
import com.spms.backend.client.OpenAiValidationClient;
import com.spms.backend.dto.request.SystemLogCreateRequestDto;
import com.spms.backend.exception.P7ApiException;
import com.spms.backend.model.*;
import com.spms.backend.repository.*;
import com.spms.backend.service.impl.ValidationPipelineOrchestratorImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidationPipelineOrchestratorTest {

    @Mock private SprintIssueTrackingRepository sprintIssueTrackingRepository;
    @Mock private IssueValidationResultRepository issueValidationResultRepository;
    @Mock private GithubIntegrationRepository githubIntegrationRepository;
    @Mock private ValidationConfigRepository validationConfigRepository;
    @Mock private GithubApiClient githubApiClient;
    @Mock private OpenAiValidationClient openAiValidationClient;
    @Mock private SystemLogService systemLogService;
    @Mock private ValidationJobWriteService writeService;
    @Mock private EncryptionService encryptionService;

    private ValidationPipelineOrchestratorImpl orchestrator;
    private ObjectMapper objectMapper = new ObjectMapper();

    private ValidationJob job;
    private Sprint sprint;
    private Group group;
    private ValidationConfig config;

    @BeforeEach
    void setUp() {
        orchestrator = new ValidationPipelineOrchestratorImpl(
                sprintIssueTrackingRepository,
                issueValidationResultRepository,
                githubIntegrationRepository,
                validationConfigRepository,
                githubApiClient,
                openAiValidationClient,
                systemLogService,
                writeService,
                encryptionService,
                objectMapper);

        lenient().when(encryptionService.decrypt(anyString())).thenAnswer(inv -> inv.getArgument(0));

        sprint = new Sprint("Sprint-1", LocalDate.now().minusDays(7), LocalDate.now(), "COMPLETED");
        sprint.setId(1L);

        group = new Group();
        group.setId(10L);
        group.setGroupName("Team Alpha");
        group.setStatus(GroupStatus.FORMED);

        job = new ValidationJob();
        job.setJobId(99L);
        job.setSprint(sprint);
        job.setTeam(group);
        job.setJobStatus(ValidationJobStatus.QUEUED);
        job.setCurrentStep(ValidationJobStep.LOADING_CONTEXT);
        job.setIssuesTotal(1);
        job.setIssuesCompleted(0);
        job.setIssuesFailed(0);
        job.setProgressPercentage(0);
        job.setStartedAt(Instant.now());

        config = new ValidationConfig();
        config.setId(1L);
        config.setReviewWeight(40);
        config.setImplementationWeight(60);
        config.setOpenaiModel("gpt-4o");
        config.setMaxDiffLines(500);
        config.setExcludedFilePatterns(List.of("package-lock.json", "*.min.js"));
    }

    @Test
    void issueWithNoPrIsMarkedSkipped() {
        SprintIssueTracking sit = issueSit("PROJ-1", null);
        when(validationConfigRepository.findById(1L)).thenReturn(Optional.of(config));
        when(sprintIssueTrackingRepository.findBySprint_Id(1L)).thenReturn(List.of(sit));

        orchestrator.runAsync(job, false);

        ArgumentCaptor<IssueValidationResult> captor = ArgumentCaptor.forClass(IssueValidationResult.class);
        verify(writeService, atLeastOnce()).saveResult(captor.capture());
        IssueValidationResult saved = captor.getAllValues().stream()
                .filter(r -> "PROJ-1".equals(r.getIssueKey())).findFirst().orElseThrow();
        assertEquals("SKIPPED", saved.getValidationStatus());
        verify(githubApiClient, never()).fetchPrReviews(any(), any(), anyLong(), any());
    }

    @Test
    void issueWithMissingGithubIntegrationIsMarkedFailed() {
        SprintIssueTracking sit = issueSit("PROJ-2", 42L);
        when(validationConfigRepository.findById(1L)).thenReturn(Optional.of(config));
        when(sprintIssueTrackingRepository.findBySprint_Id(1L)).thenReturn(List.of(sit));
        when(githubIntegrationRepository.findByGroup_Id(10L)).thenReturn(Optional.empty());

        orchestrator.runAsync(job, false);

        verify(writeService, atLeastOnce()).saveFailedIssue(eq(99L), eq(sit), anyString());
    }

    @Test
    void successfulIssueIsMarkedValidatedWithCompositeScore() {
        SprintIssueTracking sit = issueSit("PROJ-3", 7L);

        GithubIntegration gh = new GithubIntegration();
        gh.setOrganizationName("my-org");
        gh.setRepositoryName("my-repo");
        gh.setGithubPatEncrypted("fake-pat");

        Map<String, Object> reviewAi = new HashMap<>();
        reviewAi.put("score", 80);
        reviewAi.put("reviewQuality", "SUFFICIENT");
        reviewAi.put("hasChangeRequests", true);
        reviewAi.put("hasSubstantiveComments", true);
        reviewAi.put("reviewerCount", 2);
        reviewAi.put("aiFeedback", "Good review.");

        Map<String, Object> implAi = new HashMap<>();
        implAi.put("score", 70);
        implAi.put("isValid", true);
        implAi.put("coverageAreas", List.of());
        implAi.put("missingRequirements", List.of());
        implAi.put("aiFeedback", "Mostly implemented.");
        implAi.put("filesAnalyzed", 3);
        implAi.put("diffTruncated", false);

        when(validationConfigRepository.findById(1L)).thenReturn(Optional.of(config));
        when(sprintIssueTrackingRepository.findBySprint_Id(1L)).thenReturn(List.of(sit));
        when(githubIntegrationRepository.findByGroup_Id(10L)).thenReturn(Optional.of(gh));
        when(githubApiClient.fetchPrFiles(any(), any(), anyLong(), any())).thenReturn(List.of());
        when(githubApiClient.fetchPrReviews(any(), any(), anyLong(), any())).thenReturn(List.of());
        when(githubApiClient.fetchPrReviewComments(any(), any(), anyLong(), any())).thenReturn(List.of());
        when(openAiValidationClient.verifyReview(any(), any())).thenReturn(new OpenAiCallResult(reviewAi, 200, 100));
        when(openAiValidationClient.validateImplementation(any(), any(), any(), anyInt(), anyBoolean())).thenReturn(new OpenAiCallResult(implAi, 200, 150));

        orchestrator.runAsync(job, false);

        ArgumentCaptor<IssueValidationResult> captor = ArgumentCaptor.forClass(IssueValidationResult.class);
        verify(writeService, atLeastOnce()).saveResult(captor.capture());
        IssueValidationResult saved = captor.getAllValues().stream()
                .filter(r -> "PROJ-3".equals(r.getIssueKey())).findFirst().orElseThrow();

        assertEquals("VALIDATED", saved.getValidationStatus());
        // composite = (80 * 40 + 70 * 60) / 100 = (3200 + 4200) / 100 = 74.00
        assertNotNull(saved.getCompositeScore());
        assertEquals(0, saved.getCompositeScore().compareTo(new java.math.BigDecimal("74.00")));
    }

    @Test
    void excludedFilesAreFilteredFromDiff() {
        SprintIssueTracking sit = issueSit("PROJ-4", 5L);

        GithubIntegration gh = new GithubIntegration();
        gh.setOrganizationName("org");
        gh.setRepositoryName("repo");
        gh.setGithubPatEncrypted("pat");

        Map<String, Object> includedFile = new HashMap<>();
        includedFile.put("filename", "src/Main.java");
        includedFile.put("patch", "+line1\n+line2");

        Map<String, Object> excludedFile = new HashMap<>();
        excludedFile.put("filename", "package-lock.json");
        excludedFile.put("patch", "+excluded");

        when(validationConfigRepository.findById(1L)).thenReturn(Optional.of(config));
        when(sprintIssueTrackingRepository.findBySprint_Id(1L)).thenReturn(List.of(sit));
        when(githubIntegrationRepository.findByGroup_Id(10L)).thenReturn(Optional.of(gh));
        when(githubApiClient.fetchPrFiles(any(), any(), anyLong(), any()))
                .thenReturn(List.of(includedFile, excludedFile));
        when(githubApiClient.fetchPrReviews(any(), any(), anyLong(), any())).thenReturn(List.of());
        when(githubApiClient.fetchPrReviewComments(any(), any(), anyLong(), any())).thenReturn(List.of());

        Map<String, Object> reviewAi = Map.of("score", 50, "reviewQuality", "MINIMAL",
                "hasChangeRequests", false, "hasSubstantiveComments", false,
                "reviewerCount", 1, "aiFeedback", "ok");
        Map<String, Object> implAi = new HashMap<>();
        implAi.put("score", 50);
        implAi.put("isValid", true);
        implAi.put("coverageAreas", List.of());
        implAi.put("missingRequirements", List.of());
        implAi.put("aiFeedback", "ok");
        implAi.put("filesAnalyzed", 1);
        implAi.put("diffTruncated", false);

        when(openAiValidationClient.verifyReview(any(), any())).thenReturn(new OpenAiCallResult(reviewAi, 200, 50));
        ArgumentCaptor<String> diffCaptor = ArgumentCaptor.forClass(String.class);
        when(openAiValidationClient.validateImplementation(any(), any(), diffCaptor.capture(), anyInt(), anyBoolean()))
                .thenReturn(new OpenAiCallResult(implAi, 200, 80));

        orchestrator.runAsync(job, false);

        String sentDiff = diffCaptor.getValue();
        assertFalse(sentDiff.contains("excluded"), "Excluded file patch must not be sent to AI");
        assertTrue(sentDiff.contains("line1"), "Included file patch must be sent to AI");
    }

    @Test
    void jobIsMarkedFailedWhenConfigIsMissing() {
        when(validationConfigRepository.findById(1L)).thenReturn(Optional.empty());

        orchestrator.runAsync(job, false);

        verify(writeService).markJobFailed(eq(99L), any());
    }

    @Test
    void d9LogIsWrittenOnCompletion() {
        when(validationConfigRepository.findById(1L)).thenReturn(Optional.of(config));
        when(sprintIssueTrackingRepository.findBySprint_Id(1L)).thenReturn(List.of());

        orchestrator.runAsync(job, false);

        ArgumentCaptor<SystemLogCreateRequestDto> captor = ArgumentCaptor.forClass(SystemLogCreateRequestDto.class);
        verify(systemLogService, atLeastOnce()).logEventAsync(captor.capture());
        boolean hasCompletedLog = captor.getAllValues().stream()
                .anyMatch(r -> r.getEventType().startsWith("P7_VALIDATION_"));
        assertTrue(hasCompletedLog, "D9 audit log must be written when pipeline finishes");
    }

    @Test
    void githubRateLimitCausesIssueFailedWithCorrectErrorCode() {
        SprintIssueTracking sit = issueSit("PROJ-5", 10L);

        GithubIntegration gh = new GithubIntegration();
        gh.setOrganizationName("org");
        gh.setRepositoryName("repo");
        gh.setGithubPatEncrypted("pat");

        when(validationConfigRepository.findById(1L)).thenReturn(Optional.of(config));
        when(sprintIssueTrackingRepository.findBySprint_Id(1L)).thenReturn(List.of(sit));
        when(githubIntegrationRepository.findByGroup_Id(10L)).thenReturn(Optional.of(gh));
        when(githubApiClient.fetchPrFiles(any(), any(), anyLong(), any()))
                .thenThrow(new P7ApiException(HttpStatus.BAD_GATEWAY, "GITHUB_RATE_LIMITED",
                        "GitHub API rate limited (HTTP 429)"));

        orchestrator.runAsync(job, false);

        verify(writeService).saveFailedIssue(eq(99L), eq(sit), eq("GITHUB_RATE_LIMITED"));
    }

    @Test
    void allSixCurrentStepsAreSetDuringSuccessfulRun() {
        SprintIssueTracking sit = issueSit("PROJ-6", 3L);

        GithubIntegration gh = new GithubIntegration();
        gh.setOrganizationName("org");
        gh.setRepositoryName("repo");
        gh.setGithubPatEncrypted("pat");

        Map<String, Object> reviewAi = Map.of("score", 60, "reviewQuality", "SUFFICIENT",
                "hasChangeRequests", false, "hasSubstantiveComments", true,
                "reviewerCount", 1, "aiFeedback", "ok");
        Map<String, Object> implAi = new HashMap<>();
        implAi.put("score", 60);
        implAi.put("isValid", true);
        implAi.put("coverageAreas", List.of());
        implAi.put("missingRequirements", List.of());
        implAi.put("aiFeedback", "ok");
        implAi.put("filesAnalyzed", 1);
        implAi.put("diffTruncated", false);

        when(validationConfigRepository.findById(1L)).thenReturn(Optional.of(config));
        when(sprintIssueTrackingRepository.findBySprint_Id(1L)).thenReturn(List.of(sit));
        when(githubIntegrationRepository.findByGroup_Id(10L)).thenReturn(Optional.of(gh));
        when(githubApiClient.fetchPrFiles(any(), any(), anyLong(), any())).thenReturn(List.of());
        when(githubApiClient.fetchPrReviews(any(), any(), anyLong(), any())).thenReturn(List.of());
        when(githubApiClient.fetchPrReviewComments(any(), any(), anyLong(), any())).thenReturn(List.of());
        when(openAiValidationClient.verifyReview(any(), any())).thenReturn(new OpenAiCallResult(reviewAi, 200, 60));
        when(openAiValidationClient.validateImplementation(any(), any(), any(), anyInt(), anyBoolean()))
                .thenReturn(new OpenAiCallResult(implAi, 200, 90));

        orchestrator.runAsync(job, false);

        ArgumentCaptor<ValidationJobStep> stepCaptor = ArgumentCaptor.forClass(ValidationJobStep.class);
        verify(writeService).markJobInProgress(99L);
        verify(writeService, atLeast(4)).updateStep(eq(99L), stepCaptor.capture());

        Set<ValidationJobStep> usedSteps = new HashSet<>(stepCaptor.getAllValues());
        assertTrue(usedSteps.contains(ValidationJobStep.FETCHING_PR_DETAILS));
        assertTrue(usedSteps.contains(ValidationJobStep.FETCHING_DIFFS));
        assertTrue(usedSteps.contains(ValidationJobStep.AI_REVIEW_VERIFICATION));
        assertTrue(usedSteps.contains(ValidationJobStep.AI_IMPLEMENTATION_VALIDATION));
        assertTrue(usedSteps.contains(ValidationJobStep.STORING_RESULTS));
    }

    @Test
    void d9SubprocessLogsAreWrittenPerIssue() {
        SprintIssueTracking sit1 = issueSit("PROJ-7", null);
        SprintIssueTracking sit2 = issueSit("PROJ-8", null);

        when(validationConfigRepository.findById(1L)).thenReturn(Optional.of(config));
        when(sprintIssueTrackingRepository.findBySprint_Id(1L)).thenReturn(List.of(sit1, sit2));

        orchestrator.runAsync(job, false);

        ArgumentCaptor<SystemLogCreateRequestDto> captor = ArgumentCaptor.forClass(SystemLogCreateRequestDto.class);
        verify(systemLogService, atLeast(3)).logEventAsync(captor.capture());

        long subprocessLogs = captor.getAllValues().stream()
                .filter(r -> "P7_SUBPROCESS".equals(r.getEventType()))
                .count();
        assertTrue(subprocessLogs >= 2, "Expected at least one D9 subprocess log per issue");
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private SprintIssueTracking issueSit(String issueKey, Long prNumber) {
        SprintIssueTracking sit = new SprintIssueTracking(group, sprint, issueKey);
        sit.setPrNumber(prNumber);
        sit.setPrMerged(prNumber != null);
        return sit;
    }
}
