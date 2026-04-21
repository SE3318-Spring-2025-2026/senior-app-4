package com.spms.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record AdvisorDecisionRequestDto(
        @NotNull(message = "Status is required")
        @Pattern(regexp = "^(?i)(approved|rejected)$", message = "Status must be 'approved' or 'rejected'")
        String status,
        
        String reason
) {}
