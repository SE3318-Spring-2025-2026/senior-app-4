package com.spms.backend.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class GroupSprintSummaryResponse {

    private ActiveSprintInfo activeSprint;
    private List<GroupSummaryDto> groups;

    public GroupSprintSummaryResponse() {}

    public GroupSprintSummaryResponse(ActiveSprintInfo activeSprint, List<GroupSummaryDto> groups) {
        this.activeSprint = activeSprint;
        this.groups = groups;
    }

    public ActiveSprintInfo getActiveSprint() { return activeSprint; }
    public void setActiveSprint(ActiveSprintInfo activeSprint) { this.activeSprint = activeSprint; }

    public List<GroupSummaryDto> getGroups() { return groups; }
    public void setGroups(List<GroupSummaryDto> groups) { this.groups = groups; }

    public static class ActiveSprintInfo {
        private Long sprintId;
        private String sprintName;
        private LocalDate startDate;
        private LocalDate endDate;
        private Long daysRemaining;

        public ActiveSprintInfo() {}

        public ActiveSprintInfo(Long sprintId, String sprintName, LocalDate startDate, LocalDate endDate, Long daysRemaining) {
            this.sprintId = sprintId;
            this.sprintName = sprintName;
            this.startDate = startDate;
            this.endDate = endDate;
            this.daysRemaining = daysRemaining;
        }

        public Long getSprintId() { return sprintId; }
        public void setSprintId(Long sprintId) { this.sprintId = sprintId; }

        public String getSprintName() { return sprintName; }
        public void setSprintName(String sprintName) { this.sprintName = sprintName; }

        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

        public Long getDaysRemaining() { return daysRemaining; }
        public void setDaysRemaining(Long daysRemaining) { this.daysRemaining = daysRemaining; }
    }

    public static class GroupSummaryDto {
        private Long groupId;
        private String groupName;
        private Integer totalIssues;
        private Integer mergedPRCount;
        private Instant syncedAt;
        private List<PerStudentSummaryDto> perStudentSummary;

        public GroupSummaryDto() {}

        public GroupSummaryDto(Long groupId, String groupName, Integer totalIssues, Integer mergedPRCount, Instant syncedAt, List<PerStudentSummaryDto> perStudentSummary) {
            this.groupId = groupId;
            this.groupName = groupName;
            this.totalIssues = totalIssues;
            this.mergedPRCount = mergedPRCount;
            this.syncedAt = syncedAt;
            this.perStudentSummary = perStudentSummary;
        }

        public Long getGroupId() { return groupId; }
        public void setGroupId(Long groupId) { this.groupId = groupId; }

        public String getGroupName() { return groupName; }
        public void setGroupName(String groupName) { this.groupName = groupName; }

        public Integer getTotalIssues() { return totalIssues; }
        public void setTotalIssues(Integer totalIssues) { this.totalIssues = totalIssues; }

        public Integer getMergedPRCount() { return mergedPRCount; }
        public void setMergedPRCount(Integer mergedPRCount) { this.mergedPRCount = mergedPRCount; }

        public Instant getSyncedAt() { return syncedAt; }
        public void setSyncedAt(Instant syncedAt) { this.syncedAt = syncedAt; }

        public List<PerStudentSummaryDto> getPerStudentSummary() { return perStudentSummary; }
        public void setPerStudentSummary(List<PerStudentSummaryDto> perStudentSummary) { this.perStudentSummary = perStudentSummary; }
    }
}
