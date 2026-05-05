package com.spms.backend.dto.response;

import java.time.Instant;

public record TriggerValidationResponse(
        String status,
        String message,
        Data data
) {
    public record Data(
            Long jobId,
            Long sprintId,
            Long teamId,
            Integer issueCount,
            String status,
            Instant createdAt
    ) {
    }
}
