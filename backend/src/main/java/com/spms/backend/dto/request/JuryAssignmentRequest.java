package com.spms.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class JuryAssignmentRequest {
    @NotNull(message = "Jury Member ID is required")
    private Long juryMemberId;

    @NotBlank(message = "Jury Type is required (e.g., INTERNAL, EXTERNAL, ADDITIONAL)")
    private String juryType;

    public Long getJuryMemberId() {
        return juryMemberId;
    }

    public void setJuryMemberId(Long juryMemberId) {
        this.juryMemberId = juryMemberId;
    }

    public String getJuryType() {
        return juryType;
    }

    public void setJuryType(String juryType) {
        this.juryType = juryType;
    }
}
