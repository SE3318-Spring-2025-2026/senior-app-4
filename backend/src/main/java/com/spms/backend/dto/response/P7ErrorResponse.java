package com.spms.backend.dto.response;

public record P7ErrorResponse(
        String status,
        String message,
        String errorCode
) {
}
