package com.spms.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import com.spms.backend.model.enums.CommitteeStatus;

public class CommitteeCreateRequest {
    @NotBlank(message = "Committee name is required")
    private String committeeName;

    private String description;

    private CommitteeStatus status;

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

    public CommitteeStatus getStatus() {
        return status;
    }

    public void setStatus(CommitteeStatus status) {
        this.status = status;
    }
}
