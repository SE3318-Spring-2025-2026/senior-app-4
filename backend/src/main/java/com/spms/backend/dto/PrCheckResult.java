package com.spms.backend.dto;

/**
 * DTO for GitHub PR check result
 * @param prNumber GitHub PR number
 * @param merged true if PR is merged, false if still open
 * @param authorGithubUsername GitHub username of PR author
 */
public record PrCheckResult(
        Long prNumber,
        boolean merged,
        String authorGithubUsername
) {
}
