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
import com.spms.backend.service.EncryptionService;
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
    private final EncryptionService encryptionService;
    private final GithubDiscoveryService githubDiscoveryService;

    public ScrumSyncServiceImpl(JiraApiClient jiraApiClient,
                              GithubApiClient githubApiClient,
                              GroupRepository groupRepository,
                              JiraIntegrationRepository jiraIntegrationRepository,
                              GithubIntegrationRepository githubIntegrationRepository,
                              SprintIssueTrackingRepository sprintIssueTrackingRepository,
                              SprintRepository sprintRepository,
                              EncryptionService encryptionService,
                              GithubDiscoveryService githubDiscoveryService) {
        this.jiraApiClient = jiraApiClient;
        this.githubApiClient = githubApiClient;
        this.groupRepository = groupRepository;
        this.jiraIntegrationRepository = jiraIntegrationRepository;
        this.githubIntegrationRepository = githubIntegrationRepository;
        this.sprintIssueTrackingRepository = sprintIssueTrackingRepository;
        this.sprintRepository = sprintRepository;
        this.encryptionService = encryptionService;
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
    @Transactional
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
                logger.error("Failed to sync group: {}", group.getId(), e);
            }
        }

        logger.info("Sync pipeline execution completed");
    }

    protected void processSingleGroup(Group group, com.spms.backend.model.Sprint sprint) {
        logger.debug("Processing group: {} for sprint: {}", group.getId(), sprint.getId());

        // Phase 2: Fetch JIRA issues
        var jiraIntegration = jiraIntegrationRepository.findByGroup_Id(group.getId());
        if (jiraIntegration.isEmpty()) {
            logger.debug("No JIRA integration found for group: {}", group.getId());
            return;
        }

        var jira = jiraIntegration.get();

        if (jira.getApiKey() == null || jira.getApiKey().trim().isEmpty()) {
            logger.warn("No JIRA API key configured for group: {}", group.getId());
            return;
        }

        if (jira.getJiraEmail() == null || jira.getJiraEmail().trim().isEmpty()) {
            logger.warn("No JIRA email configured for group: {}", group.getId());
            return;
        }

        String decryptedJiraToken;
        try {
            decryptedJiraToken = encryptionService.decrypt(jira.getApiKey());
        } catch (Exception e) {
            logger.warn("Failed to decrypt JIRA credentials for group: {}", group.getId(), e);
            return;
        }

        List<JiraIssueData> jiraIssues;
        try {
            jiraIssues = jiraApiClient.fetchActiveSprintIssues(
                jira.getJiraSpaceUrl(),
                jira.getJiraEmail(),
                decryptedJiraToken,
                jira.getProjectKey()
            );
            logger.debug("Fetched {} JIRA issues for group: {}", jiraIssues.size(), group.getId());
        } catch (Exception e) {
            logger.error("Failed to fetch JIRA issues for group: {}", group.getId(), e);
            return;
        }

        // Phase 3 & 4: GitHub data fetch and merge/persist
        var githubIntegration = githubIntegrationRepository.findByGroup_Id(group.getId());
        if (githubIntegration.isEmpty()) {
            logger.debug("No GitHub integration found for group: {}", group.getId());
            return;
        }

        var github = githubIntegration.get();

        if (github.getGithubPatEncrypted() == null || github.getGithubPatEncrypted().trim().isEmpty()) {
            logger.warn("No GitHub PAT configured for group: {}", group.getId());
            return;
        }

        if (github.getOrganizationName() == null || github.getOrganizationName().trim().isEmpty()) {
            logger.warn("No GitHub organization name configured for group: {}", group.getId());
            return;
        }

        if (github.getRepositoryName() == null || github.getRepositoryName().trim().isEmpty()) {
            logger.warn("No GitHub repository name configured for group: {}", group.getId());
            return;
        }

        String decryptedGithubPat;
        try {
            decryptedGithubPat = encryptionService.decrypt(github.getGithubPatEncrypted());
        } catch (Exception e) {
            logger.warn("Failed to decrypt GitHub credentials for group: {}", group.getId(), e);
            return;
        }

        List<SprintIssueTracking> trackingLogs = new ArrayList<>();

        for (JiraIssueData jiraIssue : jiraIssues) {
            SprintIssueTracking log = sprintIssueTrackingRepository
                .findByGroup_IdAndSprint_IdAndIssueKey(group.getId(), sprint.getId(), jiraIssue.issueKey())
                .orElseGet(() -> new SprintIssueTracking(group, sprint, jiraIssue.issueKey()));

            log.setStoryPoints(jiraIssue.storyPoints());
            log.setDescription(jiraIssue.description());
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
                    } else {
                        Optional<PrCheckResult> anyPrResult = githubApiClient.findPrForBranch(
                            github.getOrganizationName(),
                            github.getRepositoryName(),
                            branchName.get(),
                            decryptedGithubPat
                        );

                        if (anyPrResult.isPresent()) {
                            log.setAssigneeGithubUsername(anyPrResult.get().authorGithubUsername());
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to fetch PR info for issue: {} in group: {}",
                    jiraIssue.issueKey(), group.getId(), e);
            }

            trackingLogs.add(log);
        }

        sprintIssueTrackingRepository.saveAll(trackingLogs);
        logger.info("Synced {} tracking records for group: {}", trackingLogs.size(), group.getId());
    }

    @Override
    public boolean isSyncRunning() {
        return syncInProgress.get();
    }
}
