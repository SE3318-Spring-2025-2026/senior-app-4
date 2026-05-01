package com.spms.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

/**
 * P5.4 input — drive the assignment state machine.
 * {@code examDate} is required when transitioning to SCHEDULED.
 */
public record AssignmentStatusUpdateRequest(
        @NotBlank(message = "status is required.") String status,
        Instant examDate
) {}
