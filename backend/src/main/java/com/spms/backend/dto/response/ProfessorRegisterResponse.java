package com.spms.backend.dto.response;

public record ProfessorRegisterResponse(
        String message,
        Long userId,
        String role
) {
}
