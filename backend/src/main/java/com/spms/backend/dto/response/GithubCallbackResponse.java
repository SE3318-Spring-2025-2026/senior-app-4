package com.spms.backend.dto.response;

public record GithubCallbackResponse(
        String message,
        String studentId,
        String githubUsername,
        String token
) {
}
