package com.spms.backend.dto.response;

import java.time.LocalDateTime;

public class RevisionCreateResponseDto {
    private String status;
    private String message;
    private RevisionData data;

    public RevisionCreateResponseDto() {}

    public RevisionCreateResponseDto(String status, String message, RevisionData data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public RevisionData getData() { return data; }
    public void setData(RevisionData data) { this.data = data; }

    public static class RevisionData {
        private Long id;
        private Long parentSubmissionId;
        private Integer revisionNumber;
        private String status;
        private LocalDateTime submittedAt;

        public RevisionData() {}

        public RevisionData(Long id, Long parentSubmissionId, Integer revisionNumber, String status, LocalDateTime submittedAt) {
            this.id = id;
            this.parentSubmissionId = parentSubmissionId;
            this.revisionNumber = revisionNumber;
            this.status = status;
            this.submittedAt = submittedAt;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public Long getParentSubmissionId() { return parentSubmissionId; }
        public void setParentSubmissionId(Long parentSubmissionId) { this.parentSubmissionId = parentSubmissionId; }

        public Integer getRevisionNumber() { return revisionNumber; }
        public void setRevisionNumber(Integer revisionNumber) { this.revisionNumber = revisionNumber; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public LocalDateTime getSubmittedAt() { return submittedAt; }
        public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    }
}
