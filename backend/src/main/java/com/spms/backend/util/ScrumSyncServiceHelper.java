package com.spms.backend.util;

import com.spms.backend.client.GithubApiClient;
import com.spms.backend.client.JiraApiClient;
import com.spms.backend.dto.JiraIssueData;
import com.spms.backend.dto.PrCheckResult;
import com.spms.backend.model.Group;
import com.spms.backend.model.SprintIssueTracking;
import com.spms.backend.repository.*;
import com.spms.backend.service.EncryptionService;
import com.spms.backend.service.GithubDiscoveryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
public class ScrumSyncServiceHelper {
    private static final Logger logger = LoggerFactory.getLogger(ScrumSyncServiceHelper.class);

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

    @Async
    @Transactional
    public void executeSyncAsync(String syncId) {
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

    private void executeSyncPipeline() {
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
        String decryptedJiraToken = encryptionService.decrypt(jira.getApiKey());

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
        String decryptedGithubPat = encryptionService.decrypt(github.getGithubPatEncrypted());

        sprintIssueTrackingRepository.deleteByGroup_IdAndSprint_Id(group.getId(), sprint.getId());

        List<SprintIssueTracking> trackingLogs = new ArrayList<>();

        for (JiraIssueData jiraIssue : jiraIssues) {
            SprintIssueTracking log = new SprintIssueTracking(group, sprint, jiraIssue.issueKey());
            log.setStoryPoints(jiraIssue.storyPoints());
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

        sprintIssueTrackingRepository.saveAll(trackingLogs);
        logger.info("Synced {} tracking records for group: {}", trackingLogs.size(), group.getId());
    }

    public AtomicBoolean getSyncInProgress() {
        return syncInProgress;
    }
}
