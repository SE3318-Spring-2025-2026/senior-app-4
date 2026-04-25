package com.spms.backend.dto.response;

import java.time.LocalDateTime;

public class RevisionCreateResponseDto {

    private String status;
    private String message;
    private Data data;

    public RevisionCreateResponseDto(String status, String message, Data data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public Data getData() { return data; }

    public static class Data {
        private Long id;
        private Long parentSubmissionId;
        private Integer revisionNumber;
        private String status;
        private LocalDateTime submittedAt;

        public Data(Long id, Long parentSubmissionId, Integer revisionNumber,
                    String status, LocalDateTime submittedAt) {
            this.id = id;
            this.parentSubmissionId = parentSubmissionId;
            this.revisionNumber = revisionNumber;
            this.status = status;
            this.submittedAt = submittedAt;
        }

        public Long getId() { return id; }
        public Long getParentSubmissionId() { return parentSubmissionId; }
        public Integer getRevisionNumber() { return revisionNumber; }
        public String getStatus() { return status; }
        public LocalDateTime getSubmittedAt() { return submittedAt; }
    }
}
