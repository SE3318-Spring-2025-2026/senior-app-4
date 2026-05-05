package com.spms.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spms.backend.client.GithubApiClient;
import com.spms.backend.client.OpenAiValidationClient;
import com.spms.backend.dto.request.SystemLogCreateRequestDto;
import com.spms.backend.model.*;
import com.spms.backend.repository.*;
import com.spms.backend.service.impl.ValidationPipelineOrchestratorImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidationPipelineOrchestratorTest {

    @Mock private ValidationJobRepository validationJobRepository;
    @Mock private SprintIssueTrackingRepository sprintIssueTrackingRepository;
    @Mock private IssueValidationResultRepository issueValidationResultRepository;
    @Mock private GithubIntegrationRepository githubIntegrationRepository;
    @Mock private ValidationConfigRepository validationConfigRepository;
    @Mock private GithubApiClient githubApiClient;
    @Mock private OpenAiValidationClient openAiValidationClient;
    @Mock private SystemLogService systemLogService;

    @InjectMocks
    private ValidationPipelineOrchestratorImpl orchestrator;

    private ObjectMapper objectMapper = new ObjectMapper();

    private ValidationJob job;
    private Sprint sprint;
    private Group group;
    private ValidationConfig config;

    @BeforeEach
    void setUp() {
        orchestrator = new ValidationPipelineOrchestratorImpl(
                validationJobRepository, sprintIssueTrackingRepository,
                issueValidationResultRepository, githubIntegrationRepository,
                validationConfigRepository, githubApiClient, openAiValidationClient,
                systemLogService, objectMapper);

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
        when(validationJobRepository.findById(99L)).thenReturn(Optional.of(job));
        when(validationConfigRepository.findById(1L)).thenReturn(Optional.of(config));
        when(sprintIssueTrackingRepository.findBySprint_Id(1L)).thenReturn(List.of(sit));
        when(issueValidationResultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        orchestrator.runAsync(job, false);

        ArgumentCaptor<IssueValidationResult> captor = ArgumentCaptor.forClass(IssueValidationResult.class);
        verify(issueValidationResultRepository, atLeastOnce()).save(captor.capture());
        IssueValidationResult saved = captor.getAllValues().stream()
                .filter(r -> "PROJ-1".equals(r.getIssueKey())).findFirst().orElseThrow();
        assertEquals("SKIPPED", saved.getValidationStatus());
        verify(githubApiClient, never()).fetchPrReviews(any(), any(), anyLong(), any());
    }

    @Test
    void issueWithMissingGithubIntegrationIsMarkedFailed() {
        SprintIssueTracking sit = issueSit("PROJ-2", 42L);
        when(validationJobRepository.findById(99L)).thenReturn(Optional.of(job));
        when(validationConfigRepository.findById(1L)).thenReturn(Optional.of(config));
        when(sprintIssueTrackingRepository.findBySprint_Id(1L)).thenReturn(List.of(sit));
        when(githubIntegrationRepository.findByGroup_Id(10L)).thenReturn(Optional.empty());
        when(issueValidationResultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        orchestrator.runAsync(job, false);

        ArgumentCaptor<IssueValidationResult> captor = ArgumentCaptor.forClass(IssueValidationResult.class);
        verify(issueValidationResultRepository, atLeastOnce()).save(captor.capture());
        IssueValidationResult saved = captor.getAllValues().stream()
                .filter(r -> "PROJ-2".equals(r.getIssueKey())).findFirst().orElseThrow();
        assertEquals("FAILED", saved.getValidationStatus());
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

        when(validationJobRepository.findById(99L)).thenReturn(Optional.of(job));
        when(validationConfigRepository.findById(1L)).thenReturn(Optional.of(config));
        when(sprintIssueTrackingRepository.findBySprint_Id(1L)).thenReturn(List.of(sit));
        when(githubIntegrationRepository.findByGroup_Id(10L)).thenReturn(Optional.of(gh));
        when(githubApiClient.fetchPrFiles(any(), any(), anyLong(), any())).thenReturn(List.of());
        when(githubApiClient.fetchPrReviews(any(), any(), anyLong(), any())).thenReturn(List.of());
        when(githubApiClient.fetchPrReviewComments(any(), any(), anyLong(), any())).thenReturn(List.of());
        when(openAiValidationClient.verifyReview(any(), any())).thenReturn(reviewAi);
        when(openAiValidationClient.validateImplementation(any(), any(), any(), anyInt(), anyBoolean())).thenReturn(implAi);
        when(issueValidationResultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        orchestrator.runAsync(job, false);

        ArgumentCaptor<IssueValidationResult> captor = ArgumentCaptor.forClass(IssueValidationResult.class);
        verify(issueValidationResultRepository, atLeastOnce()).save(captor.capture());
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

        when(validationJobRepository.findById(99L)).thenReturn(Optional.of(job));
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

        when(openAiValidationClient.verifyReview(any(), any())).thenReturn(reviewAi);
        ArgumentCaptor<String> diffCaptor = ArgumentCaptor.forClass(String.class);
        when(openAiValidationClient.validateImplementation(any(), any(), diffCaptor.capture(), anyInt(), anyBoolean()))
                .thenReturn(implAi);
        when(issueValidationResultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        orchestrator.runAsync(job, false);

        String sentDiff = diffCaptor.getValue();
        assertFalse(sentDiff.contains("excluded"), "Excluded file patch must not be sent to AI");
        assertTrue(sentDiff.contains("line1"), "Included file patch must be sent to AI");
    }

    @Test
    void jobIsMarkedFailedWhenConfigIsMissing() {
        when(validationJobRepository.findById(99L)).thenReturn(Optional.of(job));
        when(validationConfigRepository.findById(1L)).thenReturn(Optional.empty());

        orchestrator.runAsync(job, false);

        ArgumentCaptor<ValidationJob> captor = ArgumentCaptor.forClass(ValidationJob.class);
        verify(validationJobRepository, atLeastOnce()).save(captor.capture());
        ValidationJob saved = captor.getAllValues().stream()
                .filter(j -> j.getJobStatus() == ValidationJobStatus.FAILED)
                .findFirst().orElse(null);
        assertNotNull(saved, "Job must be marked FAILED when config is missing");
    }

    @Test
    void d9LogIsWrittenOnCompletion() {
        when(validationJobRepository.findById(99L)).thenReturn(Optional.of(job));
        when(validationConfigRepository.findById(1L)).thenReturn(Optional.of(config));
        when(sprintIssueTrackingRepository.findBySprint_Id(1L)).thenReturn(List.of());

        orchestrator.runAsync(job, false);

        ArgumentCaptor<SystemLogCreateRequestDto> captor = ArgumentCaptor.forClass(SystemLogCreateRequestDto.class);
        verify(systemLogService, atLeastOnce()).logEventAsync(captor.capture());
        boolean hasCompletedLog = captor.getAllValues().stream()
                .anyMatch(r -> r.getEventType().startsWith("P7_VALIDATION_"));
        assertTrue(hasCompletedLog, "D9 audit log must be written when pipeline finishes");
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private SprintIssueTracking issueSit(String issueKey, Long prNumber) {
        SprintIssueTracking sit = new SprintIssueTracking(group, sprint, issueKey);
        sit.setPrNumber(prNumber);
        sit.setPrMerged(prNumber != null);
        return sit;
    }
}
