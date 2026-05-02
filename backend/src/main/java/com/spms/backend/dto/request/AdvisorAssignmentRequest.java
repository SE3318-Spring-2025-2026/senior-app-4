package com.spms.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AdvisorAssignmentRequest {
    @NotNull(message = "Advisor ID is required")
    private Long advisorId;

    @NotBlank(message = "Role is required (e.g., PRESIDENT, VICE_PRESIDENT, MEMBER)")
    private String role;

    public Long getAdvisorId() {
        return advisorId;
    }

    public void setAdvisorId(Long advisorId) {
        this.advisorId = advisorId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
