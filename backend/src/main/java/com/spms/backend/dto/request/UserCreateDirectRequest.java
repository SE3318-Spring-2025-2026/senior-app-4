package com.spms.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserCreateDirectRequest(
        @NotBlank(message = "fullName is required.")
        String fullName,

        @NotBlank(message = "email is required.")
        @Email(message = "email must be a valid email address.")
        String email,

        String studentId,
        String githubUsername,

        @NotBlank(message = "role is required.")
        String role
) {}
