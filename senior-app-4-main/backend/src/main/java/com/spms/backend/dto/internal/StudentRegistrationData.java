package com.spms.backend.dto.internal;

public record StudentRegistrationData(
        String studentId,
        String githubUsername,
        String accessToken
) {
}
