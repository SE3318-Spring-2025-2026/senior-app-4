package com.spms.backend.dto.response;

import java.time.Instant;

public record GroupAssignmentDto(
        Long assignmentId,
        Long groupId,
        String groupName,
        String status,
        Instant examDate,
        Instant assignedAt) {}
