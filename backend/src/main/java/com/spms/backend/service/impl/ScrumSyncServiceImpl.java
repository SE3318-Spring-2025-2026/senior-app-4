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
import com.spms.backend.repository.UserRepository;
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
import java.util.concurrent.CompletableFuture;

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
    private final UserRepository userRepository;

    public ScrumSyncServiceImpl(JiraApiClient jiraApiClient,
                              GithubApiClient githubApiClient,
                              GroupRepository groupRepository,
                              JiraIntegrationRepository jiraIntegrationRepository,
                              GithubIntegrationRepository githubIntegrationRepository,
                              SprintIssueTrackingRepository sprintIssueTrackingRepository,
                              SprintRepository sprintRepository,
                              GithubDiscoveryService githubDiscoveryService,
                              UserRepository userRepository) {
        this.jiraApiClient = jiraApiClient;
        this.githubApiClient = githubApiClient;
        this.groupRepository = groupRepository;
        this.jiraIntegrationRepository = jiraIntegrationRepository;
        this.githubIntegrationRepository = githubIntegrationRepository;
        this.sprintIssueTrackingRepository = sprintIssueTrackingRepository;
        this.sprintRepository = sprintRepository;
        this.githubDiscoveryService = githubDiscoveryService;
        this.userRepository = userRepository;
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
        CompletableFuture.runAsync(() -> executeSyncAsync(syncId));

        return new ScrumSyncResponse(
            "STARTED",
            "Synchronization pipeline started",
            syncId,
            startedAt
        );
    }

    @Override
    public ScrumSyncResponse triggerSyncForGroup(Long groupId) {
        if (!syncInProgress.compareAndSet(false, true)) {
            logger.warn("Group Scrum sync trigger rejected - synchronization already in progress");
            throw new SyncAlreadyRunningException("A synchronization process is currently running.");
        }

        String syncId = UUID.randomUUID().toString();
        logger.info("Triggering async Scrum synchronization for group {} with ID: {}", groupId, syncId);

        // Run sync asynchronously
        CompletableFuture.runAsync(() -> executeSyncForGroup(groupId));

        return new ScrumSyncResponse(
            "ACCEPTED",
            "Group Scrum synchronization started.",
            syncId,
            Instant.now()
        );
    }

    /**
     * Executes sync only for a single group.
     */
    protected void executeSyncForGroup(Long groupId) {
        try {
            logger.info("Executing Scrum sync pipeline for group: {}", groupId);

            logger.debug("Phase 1: Searching for active sprint context");
            var activeSprint = sprintRepository.findActiveSprintByDate(java.time.LocalDate.now());
            if (activeSprint.isEmpty()) {
                logger.warn("No active sprint found, skipping sync for group {}", groupId);
                return;
            }

            var sprint = activeSprint.get();
            var groupOpt = groupRepository.findById(groupId);
            
            if (groupOpt.isEmpty()) {
                logger.warn("Group not found with id: {}", groupId);
                return;
            }

            try {
                processSingleGroup(groupOpt.get(), sprint);
            } catch (Exception e) {
                logger.error("Failed to sync group: {}", groupId, e);
            }

            logger.info("Sync pipeline execution completed for group {}", groupId);
        } catch (Exception e) {
            logger.error("Critical error in group sync pipeline execution", e);
        } finally {
            syncInProgress.set(false);
            logger.debug("Group Sync lock released");
        }
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
                logger.error("Failed to sync group: {}", group.getId(), e);
            }
        }

        logger.info("Sync pipeline execution completed");
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    protected void processSingleGroup(Group group, com.spms.backend.model.Sprint sprint) {
        logger.debug("Processing group: {} for sprint: {}", group.getId(), sprint.getId());

        // Phase 2: Fetch JIRA issues
        var jiraIntegration = jiraIntegrationRepository.findByGroup_Id(group.getId());
        if (jiraIntegration.isEmpty()) {
            logger.debug("No JIRA integration found for group: {}", group.getId());
            return;
        }

        var jira = jiraIntegration.get();

        // jira_email was added in V9 migration — integrations created before that have NULL here.
        // Re-saving the integration via the bind endpoint populates it; skip until then.
        if (jira.getJiraEmail() == null || jira.getApiKey() == null) {
            logger.warn("Skipping JIRA sync for group {}: email or API key is null. "
                    + "Re-save the JIRA integration for this group to fix.", group.getId());
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
        // EncryptionConverter already decrypts githubPatEncrypted on entity load — use directly
        String decryptedGithubPat = github.getGithubPatEncrypted();

        sprintIssueTrackingRepository.deleteByGroup_IdAndSprint_Id(group.getId(), sprint.getId());

        List<SprintIssueTracking> trackingLogs = new ArrayList<>();

        for (JiraIssueData jiraIssue : jiraIssues) {
            SprintIssueTracking log = new SprintIssueTracking(group, sprint, jiraIssue.issueKey());
            log.setStoryPoints(jiraIssue.storyPoints());
            log.setIssueTitle(jiraIssue.title());
            log.setIssueDescription(jiraIssue.description());
            log.setSyncedAt(Instant.now());
            if (jiraIssue.assigneeEmail() != null && !jiraIssue.assigneeEmail().isBlank()) {
                userRepository.findByEmail(jiraIssue.assigneeEmail())
                        .map(user -> user.getGithubUsername())
                        .filter(username -> username != null && !username.isBlank())
                        .ifPresent(log::setAssigneeGithubUsername);
            }

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
                        if (log.getAssigneeGithubUsername() == null || log.getAssigneeGithubUsername().isBlank()) {
                            log.setAssigneeGithubUsername(pr.authorGithubUsername());
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
