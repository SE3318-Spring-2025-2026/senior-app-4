package com.spms.backend.client;

import com.spms.backend.dto.JiraIssueData;
import com.spms.backend.exception.JiraApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class JiraApiClient {

    private static final Logger logger = LoggerFactory.getLogger(JiraApiClient.class);

    private final RestClient restClient;

    public JiraApiClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public boolean validateSpaceConnection(String jiraSpaceUrl, String projectKey, String email, String apiKey) {
        String normalizedBaseUrl = jiraSpaceUrl != null ? jiraSpaceUrl.trim() : "";
        String normalizedProjectKey = projectKey != null ? projectKey.trim() : "";

        String validationUrl = UriComponentsBuilder
                .fromUriString(normalizedBaseUrl)
                .pathSegment("rest", "api", "3", "project", normalizedProjectKey)
                .build()
                .toUriString();

        logger.info("JIRA validation → URL: {}, email: {}", validationUrl, email);
        try {
            RestClient.RequestHeadersSpec<?> request = restClient.get()
                    .uri(validationUrl)
                    .accept(MediaType.APPLICATION_JSON);

            if (StringUtils.hasText(email) && StringUtils.hasText(apiKey)) {
                request = request.header(HttpHeaders.AUTHORIZATION, buildBasicAuthHeader(email, apiKey));
            } else if (StringUtils.hasText(apiKey)) {
                request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim());
            }

            request.retrieve().toBodilessEntity();
            logger.info("JIRA validation → SUCCESS");
            return true;
        } catch (RestClientException exception) {
            logger.warn("JIRA validation → FAILED: {}", exception.getMessage());
            return false;
        }
    }

    public Map<String, Object> fetchIssuesBatch(
            String jiraBaseUrl,
            String apiKey,
            List<String> issueKeys,
            int startAt,
            int maxResults) {

        String jql = "issueKey in (" + String.join(",", issueKeys) + ")";

        String url = UriComponentsBuilder
                .fromUriString(jiraBaseUrl.trim())
                .pathSegment("rest", "api", "2", "search")
                .queryParam("jql", jql)
                .queryParam("fields", "summary,assignee,resolution,customfield_10004")
                .queryParam("startAt", startAt)
                .queryParam("maxResults", maxResults)
                .build()
                .toUriString();

        try {
            return restClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (RestClientException e) {
            logger.error("JIRA batch fetch failed for keys {}: {}", issueKeys, e.getMessage());
            throw new JiraApiException("JIRA API returned error: " + e.getMessage());
        }
    }

    public List<String> searchIssuesByJql(String jiraBaseUrl, String apiKey, String jql) {
        List<String> allKeys = new ArrayList<>();
        int startAt = 0;
        int maxResults = 100;
        int total;

        String url = UriComponentsBuilder
                .fromUriString(jiraBaseUrl.trim())
                .pathSegment("rest", "api", "2", "search")
                .build()
                .toUriString();

        do {
            String requestBody = String.format(
                    "{\"jql\":\"%s\",\"startAt\":%d,\"maxResults\":%d,\"fields\":[\"summary\"]}",
                    jql.replace("\\", "\\\\").replace("\"", "\\\""), startAt, maxResults);

            Map<String, Object> response;
            try {
                response = restClient.post()
                        .uri(url)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .body(requestBody)
                        .retrieve()
                        .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            } catch (RestClientException e) {
                logger.error("JIRA JQL search failed (startAt={}): {}", startAt, e.getMessage());
                throw new JiraApiException("JIRA API returned error: " + e.getMessage());
            }

            total = ((Number) response.getOrDefault("total", 0)).intValue();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> issues =
                    (List<Map<String, Object>>) response.getOrDefault("issues", Collections.emptyList());

            for (Map<String, Object> issue : issues) {
                allKeys.add((String) issue.getOrDefault("key", "UNKNOWN"));
            }

            startAt += issues.size();

            if (issues.isEmpty()) {
                break;
            }

        } while (startAt < total);

        logger.info("JQL query fetched {} / {} issues", allKeys.size(), total);
        return allKeys;
    }

    /**
     * Fetch active sprint issues from JIRA Agile API
     * @param jiraSpaceUrl JIRA base URL
     * @param email JIRA account email
     * @param apiToken JIRA API token
     * @param projectKey JIRA project key
     * @return List of JiraIssueData
     */
    public List<JiraIssueData> fetchActiveSprintIssues(String jiraSpaceUrl, String email, String apiToken, String projectKey) {
        List<JiraIssueData> allIssues = new ArrayList<>();

        try {
            // Step 1: Get board ID
            String boardUrl = UriComponentsBuilder
                    .fromUriString(jiraSpaceUrl.trim())
                    .pathSegment("rest", "agile", "1.0", "board")
                    .queryParam("projectKeyOrId", projectKey.trim())
                    .build()
                    .toUriString();

            Map<String, Object> boardResponse = restClient.get()
                    .uri(boardUrl)
                    .header(HttpHeaders.AUTHORIZATION, buildBasicAuthHeader(email, apiToken))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (boardResponse == null) {
                logger.warn("Board lookup returned null for project: {}", projectKey);
                return allIssues;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> boardValues = (List<Map<String, Object>>) boardResponse.get("values");
            if (boardValues == null || boardValues.isEmpty()) {
                logger.warn("No boards found for project: {}", projectKey);
                return allIssues;
            }

            Number boardIdNum = (Number) boardValues.get(0).get("id");
            Long boardId = boardIdNum.longValue();

            // Step 2: Get active sprint ID
            String sprintUrl = UriComponentsBuilder
                    .fromUriString(jiraSpaceUrl.trim())
                    .pathSegment("rest", "agile", "1.0", "board", boardId.toString(), "sprint")
                    .queryParam("state", "active")
                    .build()
                    .toUriString();

            Map<String, Object> sprintResponse = restClient.get()
                    .uri(sprintUrl)
                    .header(HttpHeaders.AUTHORIZATION, buildBasicAuthHeader(email, apiToken))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (sprintResponse == null) {
                logger.warn("Sprint lookup returned null for board: {}", boardId);
                return allIssues;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> sprintValues = (List<Map<String, Object>>) sprintResponse.get("values");
            if (sprintValues == null || sprintValues.isEmpty()) {
                logger.warn("No active sprints found for board: {}", boardId);
                return allIssues;
            }

            Number sprintIdNum = (Number) sprintValues.get(0).get("id");
            Long sprintId = sprintIdNum.longValue();

            // Step 3: Fetch issues from active sprint (paginated)
            int startAt = 0;
            int maxResults = 100;
            int total;

            do {
                String issueUrl = UriComponentsBuilder
                        .fromUriString(jiraSpaceUrl.trim())
                        .pathSegment("rest", "agile", "1.0", "sprint", sprintId.toString(), "issue")
                        .queryParam("startAt", startAt)
                        .queryParam("maxResults", maxResults)
                        .build()
                        .toUriString();

                Map<String, Object> issueResponse = restClient.get()
                        .uri(issueUrl)
                        .header(HttpHeaders.AUTHORIZATION, buildBasicAuthHeader(email, apiToken))
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .body(new ParameterizedTypeReference<Map<String, Object>>() {});

                if (issueResponse == null) {
                    break;
                }

                total = ((Number) issueResponse.getOrDefault("total", 0)).intValue();

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> issues = (List<Map<String, Object>>) issueResponse.getOrDefault("issues", Collections.emptyList());

                for (Map<String, Object> issue : issues) {
                    String issueKey = (String) issue.get("key");

                    @SuppressWarnings("unchecked")
                    Map<String, Object> fields = (Map<String, Object>) issue.get("fields");
                    if (fields == null) continue;

                    Integer storyPoints = null;
                    Object spValue = fields.get("customfield_10016");
                    if (spValue instanceof Number) {
                        storyPoints = ((Number) spValue).intValue();
                    }

                    String assigneeEmail = null;
                    @SuppressWarnings("unchecked")
                    Map<String, Object> assignee = (Map<String, Object>) fields.get("assignee");
                    if (assignee != null) {
                        assigneeEmail = (String) assignee.get("emailAddress");
                    }

                    String description = extractDescription(fields);

                    allIssues.add(new JiraIssueData(issueKey, storyPoints, assigneeEmail, description));
                }

                startAt += issues.size();

                if (issues.isEmpty()) {
                    break;
                }

            } while (startAt < total);

            logger.info("Fetched {} issues from JIRA sprint {}", allIssues.size(), sprintId);

        } catch (RestClientException e) {
            logger.error("JIRA fetch active sprint issues failed: {}", e.getMessage());
            throw new JiraApiException("JIRA API returned error: " + e.getMessage());
        }

        return allIssues;
    }

    private String extractDescription(Map<String, Object> fields) {
        Object description = fields.get("description");
        if (description == null) {
            return null;
        }

        if (description instanceof String) {
            return (String) description;
        }

        if (description instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> descObj = (Map<String, Object>) description;
            Object content = descObj.get("content");

            if (content instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> contentList = (List<Map<String, Object>>) content;

                if (!contentList.isEmpty()) {
                    Map<String, Object> firstBlock = contentList.get(0);
                    Object blockContent = firstBlock.get("content");

                    if (blockContent instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> blockContentList = (List<Map<String, Object>>) blockContent;

                        if (!blockContentList.isEmpty()) {
                            Object text = blockContentList.get(0).get("text");
                            if (text instanceof String) {
                                return (String) text;
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    private String buildBasicAuthHeader(String email, String apiToken) {
        String credentials = email.trim() + ":" + apiToken.trim();
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
    }
}
