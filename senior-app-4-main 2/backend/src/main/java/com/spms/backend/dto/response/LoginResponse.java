package com.spms.backend.dto.response;

public record LoginResponse(
        String message,
        String token,
        String tokenType,
        long expiresIn,
        String role,
        boolean requiresPasswordChange
) {
}
