package com.spms.backend.dto.response;

public record UserCreateResponse(
        String message,
        Long userId,
        String studentId,
        String githubUsername,
        String role
) {
}
