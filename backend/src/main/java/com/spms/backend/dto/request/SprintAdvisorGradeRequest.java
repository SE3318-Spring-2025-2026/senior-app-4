package com.spms.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SprintAdvisorGradeRequest(
        @NotNull Long groupId,
        @NotNull Long sprintId,
        @NotBlank String scrumGrade,
        @NotBlank String codeReviewGrade
) {}
