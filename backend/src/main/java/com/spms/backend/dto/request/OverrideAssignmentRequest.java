package com.spms.backend.dto.request;

import jakarta.validation.constraints.NotNull;

public record OverrideAssignmentRequest(
        @NotNull(message = "Team ID cannot be null")
        Long teamId,

        @NotNull(message = "Advisor ID cannot be null")
        Long advisorId,

        String reason
) {}
