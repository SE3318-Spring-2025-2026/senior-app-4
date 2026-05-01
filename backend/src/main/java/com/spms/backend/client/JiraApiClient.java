package com.spms.backend.client;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class JiraApiClient {

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
}
