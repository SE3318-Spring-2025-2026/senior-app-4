package com.spms.backend.dto.response;

import java.util.List;

public record JiraIssueDetailsResponse(
    int total,
    int fetched,
    List<JiraCleansedIssue> issues
) {}
