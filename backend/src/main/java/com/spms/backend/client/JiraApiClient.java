package com.spms.backend.client;

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
import java.util.regex.Pattern;
import java.nio.charset.StandardCharsets;

@Component
public class JiraApiClient {

    private static final Logger logger = LoggerFactory.getLogger(JiraApiClient.class);
    private static final Pattern ISSUE_KEY_PATTERN = Pattern.compile("^[A-Z]+-[0-9]+$");

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

        try {
            RestClient.RequestHeadersSpec<?> request = restClient.get()
                    .uri(validationUrl)
                    .accept(MediaType.APPLICATION_JSON);

            if (StringUtils.hasText(email) && StringUtils.hasText(apiKey)) {
                String auth = Base64.getEncoder().encodeToString(
                    (email.trim() + ":" + apiKey.trim()).getBytes(StandardCharsets.UTF_8));
                request = request.header(HttpHeaders.AUTHORIZATION, "Basic " + auth);
            }

            request.retrieve().toBodilessEntity();
            return true;
        } catch (RestClientException exception) {
            return false;
        }
    }

    public Map<String, Object> fetchIssuesBatch(
            String jiraBaseUrl,
            String email,
            String apiKey,
            List<String> issueKeys,
            int startAt,
            int maxResults) {

        validateIssueKeys(issueKeys);
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
            String auth = Base64.getEncoder().encodeToString(
                (email.trim() + ":" + apiKey.trim()).getBytes(StandardCharsets.UTF_8));
            return restClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + auth)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (RestClientException e) {
            logger.error("JIRA batch fetch failed for keys {}: {}", issueKeys, e.getMessage());
            throw new JiraApiException("JIRA API returned error: " + e.getMessage());
        }
    }

    public List<String> searchIssuesByJql(String jiraBaseUrl, String email, String apiKey, String jql) {
        List<String> allKeys = new ArrayList<>();
        int startAt = 0;
        int maxResults = 100;
        int total;

        String url = UriComponentsBuilder
                .fromUriString(jiraBaseUrl.trim())
                .pathSegment("rest", "api", "2", "search")
                .build()
                .toUriString();

        String auth = Base64.getEncoder().encodeToString(
            (email.trim() + ":" + apiKey.trim()).getBytes(StandardCharsets.UTF_8));

        do {
            String requestBody = String.format(
                    "{\"jql\":\"%s\",\"startAt\":%d,\"maxResults\":%d,\"fields\":[\"summary\"]}",
                    jql.replace("\\", "\\\\").replace("\"", "\\\""), startAt, maxResults);

            Map<String, Object> response;
            try {
                response = restClient.post()
                        .uri(url)
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .body(requestBody)
                        .retrieve()
                        .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            } catch (RestClientException e) {
                logger.error("JIRA JQL search failed (startAt={}): {}", startAt, e.getMessage());
                throw new JiraApiException("JIRA API returned error: " + e.getMessage());
            }

            if (response == null) {
                throw new JiraApiException("Empty response from JIRA API");
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

    private void validateIssueKeys(List<String> issueKeys) {
        if (issueKeys == null || issueKeys.isEmpty()) {
            throw new JiraApiException("Issue keys list cannot be empty");
        }
        for (String key : issueKeys) {
            if (!ISSUE_KEY_PATTERN.matcher(key).matches()) {
                throw new JiraApiException("Invalid issue key format: " + key);
            }
        }
    }
}
