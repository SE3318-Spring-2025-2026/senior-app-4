package com.spms.backend.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record AdvisorDecisionRequestDto(
        @NotNull(message = "Decision is required")
        @Pattern(regexp = "^(APPROVE|REJECT)$", message = "Decision must be 'APPROVE' or 'REJECT'")
        String decision,
        
        String reason
) {
    @AssertTrue(message = "Reason is required when decision is REJECT")
    public boolean isReasonValid() {
        if ("REJECT".equals(decision)) {
            return reason != null && !reason.isBlank();
        }
        return true;
    }
}
