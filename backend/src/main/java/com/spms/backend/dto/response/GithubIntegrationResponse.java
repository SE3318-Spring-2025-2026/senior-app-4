package com.spms.backend.dto.response;

public record GithubIntegrationResponse(
        boolean success,
        GithubIntegrationData data
) {
    public record GithubIntegrationData(
            String status,
            String organizationName,
            String repositoryName,
            String connectedAt,
            String message
    ) {
    }
}