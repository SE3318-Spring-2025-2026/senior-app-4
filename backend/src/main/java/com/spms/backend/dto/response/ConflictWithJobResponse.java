package com.spms.backend.dto.response;

public record ConflictWithJobResponse(
        String error,
        String message,
        Long existingJobId
) {
}
