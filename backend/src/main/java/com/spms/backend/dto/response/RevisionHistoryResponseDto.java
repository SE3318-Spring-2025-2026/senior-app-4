package com.spms.backend.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public class RevisionHistoryResponseDto {
    private String status;
    private List<RevisionHistoryData> data;

    public RevisionHistoryResponseDto() {}

    public RevisionHistoryResponseDto(String status, List<RevisionHistoryData> data) {
        this.status = status;
        this.data = data;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<RevisionHistoryData> getData() { return data; }
    public void setData(List<RevisionHistoryData> data) { this.data = data; }

    public static class RevisionHistoryData {
        private Long id;
        private Integer revisionNumber;
        private String status;
        private LocalDateTime submittedAt;
        private String description;

        public RevisionHistoryData() {}

        public RevisionHistoryData(Long id, Integer revisionNumber, String status, LocalDateTime submittedAt, String description) {
            this.id = id;
            this.revisionNumber = revisionNumber;
            this.status = status;
            this.submittedAt = submittedAt;
            this.description = description;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public Integer getRevisionNumber() { return revisionNumber; }
        public void setRevisionNumber(Integer revisionNumber) { this.revisionNumber = revisionNumber; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public LocalDateTime getSubmittedAt() { return submittedAt; }
        public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
