package com.spms.backend.dto.response;

public class PerStudentSummaryDto {
    private String githubUsername;
    private Integer completedStoryPoints;
    private Integer totalAssignedStoryPoints;

    public PerStudentSummaryDto() {}

    public PerStudentSummaryDto(String githubUsername, Integer completedStoryPoints, Integer totalAssignedStoryPoints) {
        this.githubUsername = githubUsername;
        this.completedStoryPoints = completedStoryPoints;
        this.totalAssignedStoryPoints = totalAssignedStoryPoints;
    }

    public String getGithubUsername() { return githubUsername; }
    public void setGithubUsername(String githubUsername) { this.githubUsername = githubUsername; }

    public Integer getCompletedStoryPoints() { return completedStoryPoints; }
    public void setCompletedStoryPoints(Integer completedStoryPoints) { this.completedStoryPoints = completedStoryPoints; }

    public Integer getTotalAssignedStoryPoints() { return totalAssignedStoryPoints; }
    public void setTotalAssignedStoryPoints(Integer totalAssignedStoryPoints) { this.totalAssignedStoryPoints = totalAssignedStoryPoints; }
}
