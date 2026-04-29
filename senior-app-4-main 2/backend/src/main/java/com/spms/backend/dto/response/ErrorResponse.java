package com.spms.backend.dto.response;

public record ErrorResponse(
        String error,
        String message
) {
}
