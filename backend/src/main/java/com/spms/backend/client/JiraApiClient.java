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

import java.util.List;
import java.util.Map;

@Component
public class JiraApiClient {

    private static final Logger logger = LoggerFactory.getLogger(JiraApiClient.class);

    private final RestClient restClient;

    public JiraApiClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public boolean validateSpaceConnection(String jiraSpaceUrl, String projectKey, String apiKey) {
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

            if (StringUtils.hasText(apiKey)) {
                request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim());
            }

            request.retrieve().toBodilessEntity();
            return true;
        } catch (RestClientException exception) {
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
}
