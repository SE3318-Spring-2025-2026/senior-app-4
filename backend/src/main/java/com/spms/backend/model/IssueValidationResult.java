package com.spms.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "issue_validation_results", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"job_id", "issue_key"})
})
public class IssueValidationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private ValidationJob job;

    @Column(name = "issue_key", nullable = false, length = 64)
    private String issueKey;

    @Column(name = "issue_title")
    private String issueTitle;

    @Column(name = "issue_description", columnDefinition = "TEXT")
    private String issueDescription;

    @Column(name = "has_review")
    private Boolean hasReview;

    @Column(name = "assignee")
    private String assignee;

    @Column(name = "pr_number")
    private Long prNumber;

    @Column(name = "pr_url")
    private String prUrl;

    @Column(name = "pr_merged")
    private Boolean prMerged;

    // 7.4 Review Verification
    @Column(name = "review_score", precision = 5, scale = 2)
    private BigDecimal reviewScore;

    @Column(name = "review_quality", length = 16)
    private String reviewQuality;

    @Column(name = "reviewer_count")
    private Integer reviewerCount;

    @Column(name = "has_change_requests")
    private Boolean hasChangeRequests;

    @Column(name = "has_substantive_comments")
    private Boolean hasSubstantiveComments;

    @Column(name = "review_ai_feedback", columnDefinition = "TEXT")
    private String reviewAiFeedback;

    // 7.5 Implementation Validation
    @Column(name = "impl_score", precision = 5, scale = 2)
    private BigDecimal implScore;

    @Column(name = "impl_is_valid")
    private Boolean implIsValid;

    @Column(name = "impl_missing_requirements", columnDefinition = "TEXT")
    private String implMissingRequirements;

    @Column(name = "impl_coverage_areas", columnDefinition = "TEXT")
    private String implCoverageAreas;

    @Column(name = "impl_ai_feedback", columnDefinition = "TEXT")
    private String implAiFeedback;

    @Column(name = "files_analyzed")
    private Integer filesAnalyzed;

    @Column(name = "diff_truncated")
    private Boolean diffTruncated;

    @Column(name = "composite_score", precision = 5, scale = 2)
    private BigDecimal compositeScore;

    @Column(name = "validation_status", nullable = false, length = 16)
    private String validationStatus;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;

    @Column(name = "sprint_id")
    private Long sprintId;

    @Column(name = "team_id")
    private Long teamId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ValidationJob getJob() { return job; }
    public void setJob(ValidationJob job) { this.job = job; }
    public String getIssueKey() { return issueKey; }
    public void setIssueKey(String issueKey) { this.issueKey = issueKey; }
    public String getIssueTitle() { return issueTitle; }
    public void setIssueTitle(String issueTitle) { this.issueTitle = issueTitle; }
    public String getAssignee() { return assignee; }
    public void setAssignee(String assignee) { this.assignee = assignee; }
    public Long getPrNumber() { return prNumber; }
    public void setPrNumber(Long prNumber) { this.prNumber = prNumber; }
    public String getPrUrl() { return prUrl; }
    public void setPrUrl(String prUrl) { this.prUrl = prUrl; }
    public Boolean getPrMerged() { return prMerged; }
    public void setPrMerged(Boolean prMerged) { this.prMerged = prMerged; }
    public BigDecimal getReviewScore() { return reviewScore; }
    public void setReviewScore(BigDecimal reviewScore) { this.reviewScore = reviewScore; }
    public String getReviewQuality() { return reviewQuality; }
    public void setReviewQuality(String reviewQuality) { this.reviewQuality = reviewQuality; }
    public Integer getReviewerCount() { return reviewerCount; }
    public void setReviewerCount(Integer reviewerCount) { this.reviewerCount = reviewerCount; }
    public Boolean getHasChangeRequests() { return hasChangeRequests; }
    public void setHasChangeRequests(Boolean hasChangeRequests) { this.hasChangeRequests = hasChangeRequests; }
    public Boolean getHasSubstantiveComments() { return hasSubstantiveComments; }
    public void setHasSubstantiveComments(Boolean hasSubstantiveComments) { this.hasSubstantiveComments = hasSubstantiveComments; }
    public String getReviewAiFeedback() { return reviewAiFeedback; }
    public void setReviewAiFeedback(String reviewAiFeedback) { this.reviewAiFeedback = reviewAiFeedback; }
    public BigDecimal getImplScore() { return implScore; }
    public void setImplScore(BigDecimal implScore) { this.implScore = implScore; }
    public Boolean getImplIsValid() { return implIsValid; }
    public void setImplIsValid(Boolean implIsValid) { this.implIsValid = implIsValid; }
    public String getImplMissingRequirements() { return implMissingRequirements; }
    public void setImplMissingRequirements(String implMissingRequirements) { this.implMissingRequirements = implMissingRequirements; }
    public String getImplCoverageAreas() { return implCoverageAreas; }
    public void setImplCoverageAreas(String implCoverageAreas) { this.implCoverageAreas = implCoverageAreas; }
    public String getImplAiFeedback() { return implAiFeedback; }
    public void setImplAiFeedback(String implAiFeedback) { this.implAiFeedback = implAiFeedback; }
    public Integer getFilesAnalyzed() { return filesAnalyzed; }
    public void setFilesAnalyzed(Integer filesAnalyzed) { this.filesAnalyzed = filesAnalyzed; }
    public Boolean getDiffTruncated() { return diffTruncated; }
    public void setDiffTruncated(Boolean diffTruncated) { this.diffTruncated = diffTruncated; }
    public BigDecimal getCompositeScore() { return compositeScore; }
    public void setCompositeScore(BigDecimal compositeScore) { this.compositeScore = compositeScore; }
    public String getValidationStatus() { return validationStatus; }
    public void setValidationStatus(String validationStatus) { this.validationStatus = validationStatus; }
    public Instant getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(Instant evaluatedAt) { this.evaluatedAt = evaluatedAt; }
    public Long getSprintId() { return sprintId; }
    public void setSprintId(Long sprintId) { this.sprintId = sprintId; }
    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public String getIssueDescription() { return issueDescription; }
    public void setIssueDescription(String issueDescription) { this.issueDescription = issueDescription; }
    public Boolean getHasReview() { return hasReview; }
    public void setHasReview(Boolean hasReview) { this.hasReview = hasReview; }
}
