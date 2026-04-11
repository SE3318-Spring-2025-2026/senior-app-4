package com.spms.backend.dto.response;

import com.spms.backend.model.User;

public record UserResponse(
        String message,
        UserData data
) {
    public record UserData(
            Long userId,
            String fullName,
            String email,
            String studentId,
            String githubUsername,
            String role,
            String createdAt
    ) {}

    public static UserResponse from(String message, User user) {
        return new UserResponse(message, new UserData(
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getStudentId(),
                user.getGithubUsername(),
                user.getRole(),
                user.getCreatedAt() != null ? user.getCreatedAt().toString() : null
        ));
    }
}
