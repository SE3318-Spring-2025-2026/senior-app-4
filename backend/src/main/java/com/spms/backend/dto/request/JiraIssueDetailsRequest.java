package com.spms.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record JiraIssueDetailsRequest(
    @NotBlank String jiraSpaceUrl,
    @NotBlank String apiKey,
    @NotNull  List<String> issueKeys
) {}
