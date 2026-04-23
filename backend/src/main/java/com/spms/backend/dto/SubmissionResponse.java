package com.spms.backend.dto;
import com.spms.backend.model.enums.DeliverableType;
import com.spms.backend.model.enums.SubmissionStatus;
import java.time.LocalDateTime;

public class SubmissionResponse {
    private String status;
    private String message;
    private SubmissionData data;

    public SubmissionResponse(String status, String message, SubmissionData data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public static class SubmissionData {
        private Long id;
        private Long teamId;
        private DeliverableType deliverableType;
        private SubmissionStatus status;
        private Long assignedCommitteeId;
        private LocalDateTime submittedAt;

        public SubmissionData(Long id, Long teamId, DeliverableType deliverableType, 
                              SubmissionStatus status, Long assignedCommitteeId, LocalDateTime submittedAt) {
            this.id = id;
            this.teamId = teamId;
            this.deliverableType = deliverableType;
            this.status = status;
            this.assignedCommitteeId = assignedCommitteeId;
            this.submittedAt = submittedAt;
        }

        public Long getId() { return id; }
        public Long getTeamId() { return teamId; }
        public DeliverableType getDeliverableType() { return deliverableType; }
        public SubmissionStatus getStatus() { return status; }
        public Long getAssignedCommitteeId() { return assignedCommitteeId; }
        public LocalDateTime getSubmittedAt() { return submittedAt; }
    }

    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public SubmissionData getData() { return data; }
}
