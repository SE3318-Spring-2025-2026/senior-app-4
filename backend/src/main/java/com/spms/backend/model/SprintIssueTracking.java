package com.spms.backend.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "sprint_issue_tracking", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"group_id", "sprint_id", "issue_key"})
})
public class SprintIssueTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id", nullable = false)
    private Sprint sprint;

    @Column(name = "issue_key", nullable = false)
    private String issueKey;

    @Column(name = "story_points")
    private Integer storyPoints;

    @Column(name = "assignee_github_username")
    private String assigneeGithubUsername;

    @Column(name = "pr_number")
    private Long prNumber;

    @Column(name = "pr_merged")
    private Boolean prMerged;

    @Column(name = "issue_title", length = 500)
    private String issueTitle;

    @Column(name = "issue_description", columnDefinition = "TEXT")
    private String issueDescription;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    // Constructors
    public SprintIssueTracking() {
    }

    public SprintIssueTracking(Group group, Sprint sprint, String issueKey) {
        this.group = group;
        this.sprint = sprint;
        this.issueKey = issueKey;
        this.syncedAt = Instant.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public Sprint getSprint() {
        return sprint;
    }

    public void setSprint(Sprint sprint) {
        this.sprint = sprint;
    }

    public String getIssueKey() {
        return issueKey;
    }

    public void setIssueKey(String issueKey) {
        this.issueKey = issueKey;
    }

    public Integer getStoryPoints() {
        return storyPoints;
    }

    public void setStoryPoints(Integer storyPoints) {
        this.storyPoints = storyPoints;
    }

    public String getAssigneeGithubUsername() {
        return assigneeGithubUsername;
    }

    public void setAssigneeGithubUsername(String assigneeGithubUsername) {
        this.assigneeGithubUsername = assigneeGithubUsername;
    }

    public Long getPrNumber() {
        return prNumber;
    }

    public void setPrNumber(Long prNumber) {
        this.prNumber = prNumber;
    }

    public Boolean getPrMerged() {
        return prMerged;
    }

    public void setPrMerged(Boolean prMerged) {
        this.prMerged = prMerged;
    }

    public String getIssueTitle() {
        return issueTitle;
    }

    public void setIssueTitle(String issueTitle) {
        this.issueTitle = issueTitle;
    }

    public String getIssueDescription() {
        return issueDescription;
    }

    public void setIssueDescription(String issueDescription) {
        this.issueDescription = issueDescription;
    }

    public Instant getSyncedAt() {
        return syncedAt;
    }

    public void setSyncedAt(Instant syncedAt) {
        this.syncedAt = syncedAt;
    }
}
