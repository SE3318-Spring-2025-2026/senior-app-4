package com.spms.backend.dto.response;

import java.util.List;

public record JiraCallbackResponse(
    int total,
    List<JiraCleansedIssue> issues
) {}
