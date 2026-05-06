package com.spms.backend.dto.response;

import java.time.Instant;
import java.util.List;

public class GroupTrackingDetailResponse {
    private Long groupId;
    private String groupName;
    private Long sprintId;
    private Instant syncedAt;
    private List<IssueTrackingDto> issues;

    public GroupTrackingDetailResponse() {}

    public GroupTrackingDetailResponse(Long groupId, String groupName, Long sprintId, Instant syncedAt, List<IssueTrackingDto> issues) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.sprintId = sprintId;
        this.syncedAt = syncedAt;
        this.issues = issues;
    }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public Long getSprintId() { return sprintId; }
    public void setSprintId(Long sprintId) { this.sprintId = sprintId; }

    public Instant getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Instant syncedAt) { this.syncedAt = syncedAt; }

    public List<IssueTrackingDto> getIssues() { return issues; }
    public void setIssues(List<IssueTrackingDto> issues) { this.issues = issues; }

    public static class IssueTrackingDto {
        private String issueKey;
        private Integer storyPoints;
        private String assigneeGithubUsername;
        private Long prNumber;
        private Boolean prMerged;

        public IssueTrackingDto() {}

        public IssueTrackingDto(String issueKey, Integer storyPoints, String assigneeGithubUsername, Long prNumber, Boolean prMerged) {
            this.issueKey = issueKey;
            this.storyPoints = storyPoints;
            this.assigneeGithubUsername = assigneeGithubUsername;
            this.prNumber = prNumber;
            this.prMerged = prMerged;
        }

        public String getIssueKey() { return issueKey; }
        public void setIssueKey(String issueKey) { this.issueKey = issueKey; }

        public Integer getStoryPoints() { return storyPoints; }
        public void setStoryPoints(Integer storyPoints) { this.storyPoints = storyPoints; }

        public String getAssigneeGithubUsername() { return assigneeGithubUsername; }
        public void setAssigneeGithubUsername(String assigneeGithubUsername) { this.assigneeGithubUsername = assigneeGithubUsername; }

        public Long getPrNumber() { return prNumber; }
        public void setPrNumber(Long prNumber) { this.prNumber = prNumber; }

        public Boolean getPrMerged() { return prMerged; }
        public void setPrMerged(Boolean prMerged) { this.prMerged = prMerged; }
    }
}
