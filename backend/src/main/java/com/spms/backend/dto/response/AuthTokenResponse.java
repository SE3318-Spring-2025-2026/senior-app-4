package com.spms.backend.dto.response;

public record AuthTokenResponse(
    String token,
    String tokenType,
    long expiresIn
) {}