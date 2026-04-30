package com.spms.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public class CommitteeCreateRequest {
    @NotBlank(message = "Committee name is required")
    private String committeeName;

    private String description;

    public String getCommitteeName() {
        return committeeName;
    }

    public void setCommitteeName(String committeeName) {
        this.committeeName = committeeName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
