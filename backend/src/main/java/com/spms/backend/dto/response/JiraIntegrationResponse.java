package com.spms.backend.dto.response;

public record JiraIntegrationResponse(
        boolean success,
        JiraIntegrationData data
) {
    public record JiraIntegrationData(
            String status,
            String jiraSpaceUrl,
            String projectKey,
            String connectedAt,
            String message
    ) {
    }
}
