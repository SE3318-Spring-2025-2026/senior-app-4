package com.spms.backend.dto.response;

public record IntegrationsTestResponse(
        IntegrationStatus github,
        IntegrationStatus jira
) {
    public record IntegrationStatus(boolean connected, String message) {}
}