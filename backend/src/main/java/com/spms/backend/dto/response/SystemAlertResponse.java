package com.spms.backend.dto.response;

import java.util.List;

public class SystemAlertResponse {

    private Long alertId;
    private String alertType;
    private String message;
    private String deadlineDate;
    private String currentDate;
    private long daysOverdue;
    private List<AffectedGroup> affectedGroups;
    private String suggestedAction;
    private String createdAt;

    public SystemAlertResponse() {}

    public SystemAlertResponse(Long alertId,
                               String alertType,
                               String message,
                               String deadlineDate,
                               String currentDate,
                               long daysOverdue,
                               List<AffectedGroup> affectedGroups,
                               String suggestedAction,
                               String createdAt) {
        this.alertId = alertId;
        this.alertType = alertType;
        this.message = message;
        this.deadlineDate = deadlineDate;
        this.currentDate = currentDate;
        this.daysOverdue = daysOverdue;
        this.affectedGroups = affectedGroups;
        this.suggestedAction = suggestedAction;
        this.createdAt = createdAt;
    }

    public Long getAlertId() { return alertId; }
    public void setAlertId(Long alertId) { this.alertId = alertId; }

    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getDeadlineDate() { return deadlineDate; }
    public void setDeadlineDate(String deadlineDate) { this.deadlineDate = deadlineDate; }

    public String getCurrentDate() { return currentDate; }
    public void setCurrentDate(String currentDate) { this.currentDate = currentDate; }

    public long getDaysOverdue() { return daysOverdue; }
    public void setDaysOverdue(long daysOverdue) { this.daysOverdue = daysOverdue; }

    public List<AffectedGroup> getAffectedGroups() { return affectedGroups; }
    public void setAffectedGroups(List<AffectedGroup> affectedGroups) {
        this.affectedGroups = affectedGroups;
    }

    public String getSuggestedAction() { return suggestedAction; }
    public void setSuggestedAction(String suggestedAction) { this.suggestedAction = suggestedAction; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    // ── İç sınıf: Etkilenen grup bilgisi ─────────────────────────────────
    public static class AffectedGroup {
        private Long groupId;
        private String groupName;
        private int memberCount;

        public AffectedGroup() {}

        public AffectedGroup(Long groupId, String groupName, int memberCount) {
            this.groupId = groupId;
            this.groupName = groupName;
            this.memberCount = memberCount;
        }

        public Long getGroupId() { return groupId; }
        public void setGroupId(Long groupId) { this.groupId = groupId; }

        public String getGroupName() { return groupName; }
        public void setGroupName(String groupName) { this.groupName = groupName; }

        public int getMemberCount() { return memberCount; }
        public void setMemberCount(int memberCount) { this.memberCount = memberCount; }
    }
}
