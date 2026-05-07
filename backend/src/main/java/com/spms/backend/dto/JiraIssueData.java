package com.spms.backend.dto;

/**
 * DTO for JIRA issue data fetched from Agile API
 * @param issueKey JIRA issue key (e.g., "SPM-42")
 * @param storyPoints story point estimate (customfield_10016)
 * @param assigneeEmail assignee email address
 * @param title issue summary/title from JIRA
 * @param description issue description body from JIRA
 */
public record JiraIssueData(
        String issueKey,
        Integer storyPoints,
        String assigneeEmail,
        String title,
        String description
) {
}
