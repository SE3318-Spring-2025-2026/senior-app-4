package com.spms.backend.dto.response;

public record OverrideAssignmentResponse(
        String status,
        String message,
        OverrideAssignmentData data
) {
    public record OverrideAssignmentData(
            Long teamId,
            Long previousAdvisorId,
            Long newAdvisorId
    ) {}
}
