package com.spms.backend.service.impl;

import com.spms.backend.client.JiraApiClient;
import com.spms.backend.dto.request.JiraCallbackRequest;
import com.spms.backend.dto.request.JiraInitializeRequest;
import com.spms.backend.dto.request.JiraIssueDetailsRequest;
import com.spms.backend.dto.request.JiraIssueQueryRequest;
import com.spms.backend.dto.response.JiraCallbackResponse;
import com.spms.backend.dto.response.JiraCleansedIssue;
import com.spms.backend.dto.response.JiraInitializeResponse;
import com.spms.backend.dto.response.JiraIssueDetailsResponse;
import com.spms.backend.dto.response.JiraIssueQueryResponse;
import com.spms.backend.exception.JiraApiException;
import com.spms.backend.exception.NotFoundException;
import com.spms.backend.model.JiraIntegration;
import com.spms.backend.repository.JiraIntegrationRepository;
import com.spms.backend.service.JiraMetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class JiraMetricsServiceImpl implements JiraMetricsService {

    private static final Logger logger = LoggerFactory.getLogger(JiraMetricsServiceImpl.class);
    private static final int BATCH_SIZE = 100;

    private final JiraApiClient jiraApiClient;
    private final JiraIntegrationRepository jiraIntegrationRepository;

    public JiraMetricsServiceImpl(JiraApiClient jiraApiClient,
                                   JiraIntegrationRepository jiraIntegrationRepository) {
        this.jiraApiClient = jiraApiClient;
        this.jiraIntegrationRepository = jiraIntegrationRepository;
    }

    @Override
    public JiraCallbackResponse parseAndCleansePayload(JiraCallbackRequest request) {
        List<JiraCleansedIssue> cleansed = new ArrayList<>();
        for (Map<String, Object> raw : request.issues()) {
            cleansed.add(extractCleansedIssue(raw));
        }
        logger.info("Payload cleansed: {} issues processed", cleansed.size());
        return new JiraCallbackResponse(cleansed.size(), cleansed);
    }

    @Override
    public JiraIssueDetailsResponse fetchAndMergeIssueDetails(JiraIssueDetailsRequest request) {
        List<String> keys = request.issueKeys();
        List<JiraCleansedIssue> results = new ArrayList<>();

        int startAt = 0;
        while (startAt < keys.size()) {
            List<String> batch = keys.subList(startAt, Math.min(startAt + BATCH_SIZE, keys.size()));

            Map<String, Object> response;
            try {
                response = jiraApiClient.fetchIssuesBatch(
                        request.jiraSpaceUrl(), request.apiKey(), batch, 0, BATCH_SIZE);
            } catch (JiraApiException e) {
                logger.error("JIRA API error during batch fetch (startAt={}): {}", startAt, e.getMessage());
                throw e;
            }

            if (response == null) {
                throw new JiraApiException("Empty response from JIRA API during batch fetch");
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> issueList =
                    (List<Map<String, Object>>) response.getOrDefault("issues", Collections.emptyList());

            for (Map<String, Object> raw : issueList) {
                results.add(extractCleansedIssue(raw));
            }

            startAt += batch.size();
        }

        logger.info("Issue details fetched: {}/{} keys resolved", results.size(), keys.size());
        return new JiraIssueDetailsResponse(keys.size(), results.size(), results);
    }

    @Override
    public JiraInitializeResponse initializeConnection(JiraInitializeRequest request) {
        JiraIntegration integration = jiraIntegrationRepository
                .findByGroup_Id(request.groupId())
                .orElseThrow(() -> new NotFoundException(
                        "No JIRA integration found for group: " + request.groupId()));

        boolean connected = jiraApiClient.validateSpaceConnection(
                integration.getJiraSpaceUrl(),
                integration.getProjectKey(),
                integration.getApiKey());

        String message = connected
                ? "JIRA connection established successfully"
                : "JIRA connection failed — check credentials";

        logger.info("JIRA initialize for group {}: connected={}", request.groupId(), connected);

        return new JiraInitializeResponse(
                connected,
                integration.getJiraSpaceUrl(),
                integration.getProjectKey(),
                message);
    }

    @Override
    public JiraIssueQueryResponse queryIssuesByJql(JiraIssueQueryRequest request) {
        JiraIntegration integration = jiraIntegrationRepository
                .findByGroup_Id(request.groupId())
                .orElseThrow(() -> new NotFoundException(
                        "No JIRA integration found for group: " + request.groupId()));

        List<String> issueKeys;
        try {
            issueKeys = jiraApiClient.searchIssuesByJql(
                    integration.getJiraSpaceUrl(),
                    integration.getApiKey(),
                    request.jql());
        } catch (JiraApiException e) {
            logger.error("JQL query failed for group {}: {}", request.groupId(), e.getMessage());
            throw e;
        }

        logger.info("JQL query for group {} returned {} keys", request.groupId(), issueKeys.size());
        return new JiraIssueQueryResponse(issueKeys.size(), issueKeys);
    }

    // fields.assignee.displayName → assigneeName  (test kontratı)
    // fields.resolution.name     → status         (test kontratı)
    // fields.customfield_10004   → storyPoints     (test kontratı)
    private JiraCleansedIssue extractCleansedIssue(Map<String, Object> raw) {
        String key = (String) raw.getOrDefault("key", "UNKNOWN");

        @SuppressWarnings("unchecked")
        Map<String, Object> fields =
                (Map<String, Object>) raw.getOrDefault("fields", Collections.emptyMap());

        String assigneeName = "Unassigned";
        Object assigneeObj = fields.get("assignee");
        if (assigneeObj instanceof Map<?, ?> assigneeMap && assigneeMap.get("displayName") != null) {
            assigneeName = (String) assigneeMap.get("displayName");
        }

        String status = "Unresolved";
        Object resolutionObj = fields.get("resolution");
        if (resolutionObj instanceof Map<?, ?> resMap && resMap.get("name") != null) {
            status = (String) resMap.get("name");
        }

        int storyPoints = 0;
        Object sp = fields.get("customfield_10004");
        if (sp instanceof Number num) {
            storyPoints = num.intValue();
        }

        return new JiraCleansedIssue(key, assigneeName, status, storyPoints);
    }
}
