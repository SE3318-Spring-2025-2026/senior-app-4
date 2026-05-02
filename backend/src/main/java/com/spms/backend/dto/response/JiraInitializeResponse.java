package com.spms.backend.dto.response;

public record JiraInitializeResponse(
    boolean connected,
    String jiraSpaceUrl,
    String projectKey,
    String message
) {}
