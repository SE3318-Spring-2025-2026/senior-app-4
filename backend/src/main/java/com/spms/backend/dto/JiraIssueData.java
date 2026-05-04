package com.spms.backend.dto;

/**
 * DTO for JIRA issue data fetched from Agile API
 * @param issueKey JIRA issue key (e.g., "SPM-42")
 * @param storyPoints story point estimate (customfield_10016)
 * @param assigneeEmail assignee email address
 */
public record JiraIssueData(
        String issueKey,
        Integer storyPoints,
        String assigneeEmail
) {
}
