package com.spms.backend.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public class RevisionHistoryResponseDto {

    private String status;
    private List<Item> data;

    public RevisionHistoryResponseDto(String status, List<Item> data) {
        this.status = status;
        this.data = data;
    }

    public String getStatus() { return status; }
    public List<Item> getData() { return data; }

    public static class Item {
        private Long id;
        private Integer revisionNumber;
        private String status;
        private LocalDateTime submittedAt;
        private String description;

        public Item(Long id, Integer revisionNumber, String status,
                    LocalDateTime submittedAt, String description) {
            this.id = id;
            this.revisionNumber = revisionNumber;
            this.status = status;
            this.submittedAt = submittedAt;
            this.description = description;
        }

        public Long getId() { return id; }
        public Integer getRevisionNumber() { return revisionNumber; }
        public String getStatus() { return status; }
        public LocalDateTime getSubmittedAt() { return submittedAt; }
        public String getDescription() { return description; }
    }
}
