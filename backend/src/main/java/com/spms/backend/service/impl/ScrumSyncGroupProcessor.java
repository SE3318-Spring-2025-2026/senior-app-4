package com.spms.backend.service.impl;

import com.spms.backend.client.GithubApiClient;
import com.spms.backend.client.JiraApiClient;
import com.spms.backend.dto.JiraIssueData;
import com.spms.backend.dto.PrCheckResult;
import com.spms.backend.model.Group;
import com.spms.backend.model.Sprint;
import com.spms.backend.model.SprintIssueTracking;
import com.spms.backend.repository.GithubIntegrationRepository;
import com.spms.backend.repository.JiraIntegrationRepository;
import com.spms.backend.repository.SprintIssueTrackingRepository;
import com.spms.backend.service.GithubDiscoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Separate bean so that @Transactional(REQUIRES_NEW) is applied via Spring proxy
 * — self-calls within ScrumSyncServiceImpl would bypass it.
 */
@Service
public class ScrumSyncGroupProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ScrumSyncGroupProcessor.class);

    private final JiraApiClient jiraApiClient;
    private final GithubApiClient githubApiClient;
    private final JiraIntegrationRepository jiraIntegrationRepository;
    private final GithubIntegrationRepository githubIntegrationRepository;
    private final SprintIssueTrackingRepository sprintIssueTrackingRepository;
    private final GithubDiscoveryService githubDiscoveryService;

    public ScrumSyncGroupProcessor(JiraApiClient jiraApiClient,
                                   GithubApiClient githubApiClient,
                                   JiraIntegrationRepository jiraIntegrationRepository,
                                   GithubIntegrationRepository githubIntegrationRepository,
                                   SprintIssueTrackingRepository sprintIssueTrackingRepository,
                                   GithubDiscoveryService githubDiscoveryService) {
        this.jiraApiClient = jiraApiClient;
        this.githubApiClient = githubApiClient;
        this.jiraIntegrationRepository = jiraIntegrationRepository;
        this.githubIntegrationRepository = githubIntegrationRepository;
        this.sprintIssueTrackingRepository = sprintIssueTrackingRepository;
        this.githubDiscoveryService = githubDiscoveryService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processGroup(Group group, Sprint sprint) {
        logger.debug("Processing group: {} for sprint: {}", group.getId(), sprint.getId());

        var jiraOpt = jiraIntegrationRepository.findByGroup_Id(group.getId());
        if (jiraOpt.isEmpty()) {
            logger.debug("No JIRA integration found for group: {}", group.getId());
            return;
        }

        var jira = jiraOpt.get();
        // EncryptionConverter decrypts on JPA read — plain text here
        String jiraToken = jira.getApiKey();

        List<JiraIssueData> jiraIssues;
        try {
            jiraIssues = jiraApiClient.fetchActiveSprintIssues(
                    jira.getJiraSpaceUrl(),
                    jira.getJiraEmail(),
                    jiraToken,
                    jira.getProjectKey()
            );
            logger.debug("Fetched {} JIRA issues for group: {}", jiraIssues.size(), group.getId());
        } catch (Exception e) {
            logger.error("Failed to fetch JIRA issues for group: {}", group.getId(), e);
            return;
        }

        var githubOpt = githubIntegrationRepository.findByGroup_Id(group.getId());
        if (githubOpt.isEmpty()) {
            logger.debug("No GitHub integration found for group: {}", group.getId());
            return;
        }

        var github = githubOpt.get();
        // EncryptionConverter decrypts on JPA read — plain text here
        String githubPat = github.getGithubPatEncrypted();

        sprintIssueTrackingRepository.deleteByGroup_IdAndSprint_Id(group.getId(), sprint.getId());

        List<SprintIssueTracking> logs = new ArrayList<>();

        for (JiraIssueData issue : jiraIssues) {
            SprintIssueTracking log = new SprintIssueTracking(group, sprint, issue.issueKey());
            log.setStoryPoints(issue.storyPoints());
            log.setSyncedAt(Instant.now());

            try {
                Optional<String> branch = githubDiscoveryService.findBranchForIssueKey(
                        group.getId(), issue.issueKey(), github.getRepositoryName());

                if (branch.isPresent()) {
                    Optional<PrCheckResult> pr = githubApiClient.findMergedPrForBranch(
                            github.getOrganizationName(),
                            github.getRepositoryName(),
                            branch.get(),
                            githubPat);

                    pr.ifPresent(result -> {
                        log.setPrNumber(result.prNumber());
                        log.setPrMerged(result.merged());
                        log.setAssigneeGithubUsername(result.authorGithubUsername());
                    });
                }
            } catch (Exception e) {
                logger.warn("Failed to fetch PR info for issue {} in group {}: {}",
                        issue.issueKey(), group.getId(), e.getMessage());
            }

            logs.add(log);
        }

        sprintIssueTrackingRepository.saveAll(logs);
        logger.info("Synced {} tracking records for group: {}", logs.size(), group.getId());
    }
}
