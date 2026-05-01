package com.spms.backend.dto.response;

import java.time.LocalDate;

/**
 * ISSUE #396: DTO for active sprint response
 * Returned by GET /api/v1/schedules/active-sprint
 */
public record ActiveSprintDto(
    Long sprintId,
    String sprintName,
    LocalDate startDate,
    LocalDate endDate,
    String status
) {}
