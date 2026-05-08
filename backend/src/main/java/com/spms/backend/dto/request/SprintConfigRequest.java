package com.spms.backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record SprintConfigRequest(
        @NotBlank(message = "sprintName is required")
        String sprintName,

        @NotNull(message = "startDate is required")
        LocalDate startDate,

        @NotNull(message = "endDate is required")
        LocalDate endDate,

        @NotBlank(message = "status is required")
        String status,

        @NotNull(message = "requiredStoryPoints is required")
        @Min(value = 0, message = "requiredStoryPoints must be >= 0")
        Integer requiredStoryPoints
) {}
