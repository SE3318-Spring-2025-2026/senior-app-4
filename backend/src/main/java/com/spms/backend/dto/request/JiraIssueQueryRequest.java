package com.spms.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record JiraIssueQueryRequest(
    @NotNull  Long   groupId,
    @NotBlank String jql
) {}
