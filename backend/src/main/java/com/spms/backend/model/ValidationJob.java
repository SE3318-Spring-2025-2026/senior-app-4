package com.spms.backend.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "validation_jobs")
public class ValidationJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "job_id")
    private Long jobId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_job_id")
    private ValidationJob parentJob;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id", nullable = false)
    private Sprint sprint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Group team;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_status", nullable = false)
    private ValidationJobStatus jobStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_step")
    private ValidationJobStep currentStep;

    @Column(name = "progress_percentage", nullable = false)
    private Integer progressPercentage = 0;

    @Column(name = "issues_total", nullable = false)
    private Integer issuesTotal = 0;

    @Column(name = "issues_completed", nullable = false)
    private Integer issuesCompleted = 0;

    @Column(name = "issues_failed", nullable = false)
    private Integer issuesFailed = 0;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public ValidationJob getParentJob() { return parentJob; }
    public void setParentJob(ValidationJob parentJob) { this.parentJob = parentJob; }
    public Sprint getSprint() { return sprint; }
    public void setSprint(Sprint sprint) { this.sprint = sprint; }
    public Group getTeam() { return team; }
    public void setTeam(Group team) { this.team = team; }
    public ValidationJobStatus getJobStatus() { return jobStatus; }
    public void setJobStatus(ValidationJobStatus jobStatus) { this.jobStatus = jobStatus; }
    public ValidationJobStep getCurrentStep() { return currentStep; }
    public void setCurrentStep(ValidationJobStep currentStep) { this.currentStep = currentStep; }
    public Integer getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(Integer progressPercentage) { this.progressPercentage = progressPercentage; }
    public Integer getIssuesTotal() { return issuesTotal; }
    public void setIssuesTotal(Integer issuesTotal) { this.issuesTotal = issuesTotal; }
    public Integer getIssuesCompleted() { return issuesCompleted; }
    public void setIssuesCompleted(Integer issuesCompleted) { this.issuesCompleted = issuesCompleted; }
    public Integer getIssuesFailed() { return issuesFailed; }
    public void setIssuesFailed(Integer issuesFailed) { this.issuesFailed = issuesFailed; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
