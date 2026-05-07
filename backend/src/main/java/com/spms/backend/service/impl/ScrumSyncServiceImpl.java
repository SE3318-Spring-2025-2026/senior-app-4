package com.spms.backend.service.impl;

import com.spms.backend.client.GithubApiClient;
import com.spms.backend.client.JiraApiClient;
import com.spms.backend.dto.JiraIssueData;
import com.spms.backend.dto.PrCheckResult;
import com.spms.backend.dto.response.ScrumSyncResponse;
import com.spms.backend.exception.SyncAlreadyRunningException;
import com.spms.backend.model.GithubIntegration;
import com.spms.backend.model.Group;
import com.spms.backend.model.JiraIntegration;
import com.spms.backend.model.Sprint;
import com.spms.backend.model.SprintIssueTracking;
import com.spms.backend.repository.GithubIntegrationRepository;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.JiraIntegrationRepository;
import com.spms.backend.repository.SprintIssueTrackingRepository;
import com.spms.backend.repository.SprintRepository;
import com.spms.backend.service.GithubDiscoveryService;
import com.spms.backend.service.ScrumSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ISSUE #395: Implementation of Scrum Sync Service
 * Orchestrates synchronization of JIRA and GitHub data for active sprint.
 */
@Service
public class ScrumSyncServiceImpl implements ScrumSyncService {

    private static final Logger logger = LoggerFactory.getLogger(ScrumSyncServiceImpl.class);

    private final AtomicBoolean syncInProgress = new AtomicBoolean(false);
    private final JiraApiClient jiraApiClient;
    private final GithubApiClient githubApiClient;
    private final GroupRepository groupRepository;
    private final JiraIntegrationRepository jiraIntegrationRepository;
    private final GithubIntegrationRepository githubIntegrationRepository;
    private final SprintIssueTrackingRepository sprintIssueTrackingRepository;
    private final SprintRepository sprintRepository;
    private final GithubDiscoveryService githubDiscoveryService;

    public ScrumSyncServiceImpl(JiraApiClient jiraApiClient,
                              GithubApiClient githubApiClient,
                              GroupRepository groupRepository,
                              JiraIntegrationRepository jiraIntegrationRepository,
                              GithubIntegrationRepository githubIntegrationRepository,
                              SprintIssueTrackingRepository sprintIssueTrackingRepository,
                              SprintRepository sprintRepository,
                              GithubDiscoveryService githubDiscoveryService) {
        this.jiraApiClient = jiraApiClient;
        this.githubApiClient = githubApiClient;
        this.groupRepository = groupRepository;
        this.jiraIntegrationRepository = jiraIntegrationRepository;
        this.githubIntegrationRepository = githubIntegrationRepository;
        this.sprintIssueTrackingRepository = sprintIssueTrackingRepository;
        this.sprintRepository = sprintRepository;
        this.githubDiscoveryService = githubDiscoveryService;
    }

    @Override
    public ScrumSyncResponse triggerSync() {
        if (!syncInProgress.compareAndSet(false, true)) {
            throw new SyncAlreadyRunningException("Another sync process is currently active");
        }

        String syncId = UUID.randomUUID().toString();
        Instant startedAt = Instant.now();

        logger.info("Sync triggered with ID: {}", syncId);

        // Execute async without blocking
        executeSyncAsync(syncId);

        return new ScrumSyncResponse(
            "STARTED",
            "Synchronization pipeline started",
            syncId,
            startedAt
        );
    }

    @Async
    protected void executeSyncAsync(String syncId) {
        try {
            logger.info("Executing sync pipeline for syncId: {}", syncId);
            executeSyncPipeline();
            logger.info("Sync completed successfully for syncId: {}", syncId);
        } catch (Exception e) {
            logger.error("Sync failed for syncId: {}", syncId, e);
        } finally {
            syncInProgress.set(false);
        }
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void executeSyncPipeline() {
        logger.debug("Phase 1: Searching for active sprint context");
        var activeSprint = sprintRepository.findActiveSprintByDate(java.time.LocalDate.now());
        if (activeSprint.isEmpty()) {
            logger.warn("No active sprint found, skipping sync");
            return;
        }

        var sprint = activeSprint.get();
        var allGroups = groupRepository.findAll();

        logger.debug("Found {} groups to sync", allGroups.size());

        for (var group : allGroups) {
            try {
                processSingleGroup(group, sprint);
            } catch (Exception e) {
                logger.error("Failed to sync group: {} — {}: {}", group.getId(),
                        e.getClass().getSimpleName(), e.getMessage(), e);
            }
        }

        logger.info("Sync pipeline execution completed");
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    protected void processSingleGroup(Group group, com.spms.backend.model.Sprint sprint) {
        logger.info("SYNC-TRACE group={} sprint={}", group.getId(), sprint.getId());

        // Phase 2: Fetch JIRA issues
        var jiraIntegration = jiraIntegrationRepository.findByGroup_Id(group.getId());
        if (jiraIntegration.isEmpty()) {
            logger.info("SYNC-TRACE group={} — no JIRA integration, skipping", group.getId());
            return;
        }

        var jira = jiraIntegration.get();

        // jira_email was added in V9 migration — integrations created before that have NULL here.
        // Re-saving the integration via the bind endpoint populates it; skip until then.
        if (jira.getJiraEmail() == null || jira.getApiKey() == null) {
            logger.warn("SYNC-TRACE group={} — jiraEmail={} apiKeyNull={}, skipping",
                    group.getId(), jira.getJiraEmail(), jira.getApiKey() == null);
            return;
        }

        // EncryptionConverter already decrypts apiKey on entity load — use directly
        String decryptedJiraToken = jira.getApiKey();

        List<JiraIssueData> jiraIssues;
        try {
            jiraIssues = jiraApiClient.fetchActiveSprintIssues(
                jira.getJiraSpaceUrl(),
                jira.getJiraEmail(),
                decryptedJiraToken,
                jira.getProjectKey()
            );
            logger.info("SYNC-TRACE group={} — fetched {} JIRA issues", group.getId(), jiraIssues.size());
        } catch (Exception e) {
            logger.error("SYNC-TRACE group={} — JIRA fetch failed: {}", group.getId(), e.getMessage());
            return;
        }

        // Phase 3 & 4: GitHub data fetch and merge/persist
        var githubIntegration = githubIntegrationRepository.findByGroup_Id(group.getId());
        if (githubIntegration.isEmpty()) {
            logger.info("SYNC-TRACE group={} — no GitHub integration, skipping", group.getId());
            return;
        }

        var github = githubIntegration.get();
        // EncryptionConverter already decrypts githubPatEncrypted on entity load — use directly
        String decryptedGithubPat = github.getGithubPatEncrypted();

        logger.info("SYNC-TRACE group={} — deleting old records for sprint={}", group.getId(), sprint.getId());
        sprintIssueTrackingRepository.deleteByGroup_IdAndSprint_Id(group.getId(), sprint.getId());
        logger.info("SYNC-TRACE group={} — delete done, building {} tracking logs", group.getId(), jiraIssues.size());

        List<SprintIssueTracking> trackingLogs = new ArrayList<>();

        for (JiraIssueData jiraIssue : jiraIssues) {
            SprintIssueTracking log = new SprintIssueTracking(group, sprint, jiraIssue.issueKey());
            log.setStoryPoints(jiraIssue.storyPoints());
            log.setIssueTitle(jiraIssue.title());
            log.setIssueDescription(jiraIssue.description());
            log.setSyncedAt(Instant.now());

            try {
                Optional<String> branchName = githubDiscoveryService.findBranchForIssueKey(
                    group.getId(),
                    jiraIssue.issueKey(),
                    github.getRepositoryName()
                );

                if (branchName.isPresent()) {
                    Optional<PrCheckResult> prResult = githubApiClient.findMergedPrForBranch(
                        github.getOrganizationName(),
                        github.getRepositoryName(),
                        branchName.get(),
                        decryptedGithubPat
                    );

                    if (prResult.isPresent()) {
                        PrCheckResult pr = prResult.get();
                        log.setPrNumber(pr.prNumber());
                        log.setPrMerged(pr.merged());
                        log.setAssigneeGithubUsername(pr.authorGithubUsername());
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to fetch PR info for issue: {} in group: {}",
                    jiraIssue.issueKey(), group.getId(), e);
            }

            trackingLogs.add(log);
        }

        logger.info("SYNC-TRACE group={} — saving {} tracking logs", group.getId(), trackingLogs.size());
        sprintIssueTrackingRepository.saveAll(trackingLogs);
        logger.info("SYNC-TRACE group={} — saveAll done", group.getId());
    }

    @Override
    public boolean isSyncRunning() {
        return syncInProgress.get();
    }
}
