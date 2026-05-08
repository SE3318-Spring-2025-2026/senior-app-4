package com.spms.backend.dto.response;

import java.time.Instant;
import java.time.LocalDate;

public record SprintConfigResponse(
        Long id,
        String sprintName,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        Integer requiredStoryPoints,
        Instant createdAt,
        Instant updatedAt
) {}
