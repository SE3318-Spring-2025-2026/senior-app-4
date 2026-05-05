package com.spms.backend.dto.response;

import java.time.Instant;

public record JobStatusResponse(
        String status,
        Data data
) {
    public record Data(
            Long jobId,
            Long sprintId,
            String jobStatus,
            String currentStep,
            Integer progressPercentage,
            String message,
            Integer issuesTotal,
            Integer issuesCompleted,
            Integer issuesFailed,
            String failureReason,
            Instant startedAt,
            Instant completedAt
    ) {
    }
}
