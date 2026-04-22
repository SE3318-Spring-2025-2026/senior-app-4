package com.spms.backend.dto;

import com.spms.backend.model.enums.SubmissionStatus;

public class SubmissionResponse {
    private String message;
    private Long submissionId;
    private SubmissionStatus status;

    public SubmissionResponse(String message, Long submissionId, SubmissionStatus status) {
        this.message = message;
        this.submissionId = submissionId;
        this.status = status;
    }

    // Getters and Setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(Long submissionId) {
        this.submissionId = submissionId;
    }

    public SubmissionStatus getStatus() {
        return status;
    }

    public void setStatus(SubmissionStatus status) {
        this.status = status;
    }
}
