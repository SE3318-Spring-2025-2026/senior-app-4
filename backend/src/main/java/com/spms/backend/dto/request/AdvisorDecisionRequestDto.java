package com.spms.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record AdvisorDecisionRequestDto(
        @NotNull(message = "Decision is required")
        @Pattern(regexp = "^(?i)(approve|reject|approved|rejected)$", message = "Decision must be 'APPROVE' or 'REJECT'")
        String decision,
        
        String reason
) {}
