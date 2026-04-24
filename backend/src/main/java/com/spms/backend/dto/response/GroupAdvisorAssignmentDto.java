package com.spms.backend.dto.response;

import java.time.Instant;

public record GroupAdvisorAssignmentDto(
    Long teamId,
    String teamName,
    String leaderName,
    Long advisorId,
    String advisorName,
    String status,
    Instant assignedAt,
    String assignmentType
) {}
