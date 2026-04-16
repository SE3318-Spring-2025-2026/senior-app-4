package com.spms.backend.dto.request;

public record UserUpdateRequest(
        String fullName,
        String email,
        String githubUsername,
        String role
) {}
