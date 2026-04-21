package com.spms.backend.dto.response;

public record GithubIntegrationResponse(
        boolean success,
        GithubIntegrationData data
) {
    public record GithubIntegrationData(
            String status,
            String organizationName,
            String connectedAt,
            String message
    ) {
    }
}
