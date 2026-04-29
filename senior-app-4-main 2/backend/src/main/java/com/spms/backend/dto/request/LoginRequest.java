package com.spms.backend.dto.request;

public record LoginRequest(
        String email,
        String password
) {
}
