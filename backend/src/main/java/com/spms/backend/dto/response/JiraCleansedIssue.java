package com.spms.backend.dto.response;

public record JiraCleansedIssue(
    String issueKey,
    String assigneeName,
    String status,
    int storyPoints
) {}
