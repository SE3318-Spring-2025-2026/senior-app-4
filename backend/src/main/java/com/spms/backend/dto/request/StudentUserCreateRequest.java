package com.spms.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record StudentUserCreateRequest(
        @NotBlank(message = "studentId is required.")
        String studentId,
        @NotBlank(message = "githubUsername is required.")
        String githubUsername,
        @NotBlank(message = "accessToken is required.")
        String accessToken
) {
}
