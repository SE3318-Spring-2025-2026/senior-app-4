package com.spms.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record JiraBindingRequest(
        @NotBlank(message = "jiraSpaceUrl is required.")
        String jiraSpaceUrl,
        @NotBlank(message = "email is required.")
        String email,
        String apiKey,
        @NotBlank(message = "projectKey is required.")
        String projectKey
) {
}
