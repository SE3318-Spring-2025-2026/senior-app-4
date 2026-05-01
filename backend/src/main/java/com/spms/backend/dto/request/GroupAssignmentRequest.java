package com.spms.backend.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * P5.4 input — assign a group to a committee.
 * {@code examDate} is optional until the assignment moves to SCHEDULED.
 */
public record GroupAssignmentRequest(
        @NotNull(message = "groupId is required.") Long groupId,
        Instant examDate
) {}
