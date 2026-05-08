package com.spms.backend.dto.response;

import java.time.Instant;

public record SprintAdvisorGradeResponse(
        Long id,
        Long groupId,
        Long sprintId,
        Long advisorId,
        String scrumGrade,
        String codeReviewGrade,
        Instant updatedAt
) {}
