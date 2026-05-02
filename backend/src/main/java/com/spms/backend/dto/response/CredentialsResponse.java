package com.spms.backend.dto.response;

public record CredentialsResponse(
        String integrationType,
        String token,
        String projectKey,
        String organizationName
) {
}
