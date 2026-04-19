package com.spms.backend.dto.response;

import java.time.Instant;

public class GradeSubmissionResponse {
    
    private Long gradeId;
    private Long submissionId;
    private Long reviewerId;
    private Double score;
    private String comments;
    private Instant gradedAt;
    private Double calculatedAverage;
    private Boolean isGradingComplete;

    public GradeSubmissionResponse() {}

    public Long getGradeId() { return gradeId; }
    public void setGradeId(Long gradeId) { this.gradeId = gradeId; }

    public Long getSubmissionId() { return submissionId; }
    public void setSubmissionId(Long submissionId) { this.submissionId = submissionId; }

    public Long getReviewerId() { return reviewerId; }
    public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public Instant getGradedAt() { return gradedAt; }
    public void setGradedAt(Instant gradedAt) { this.gradedAt = gradedAt; }

    public Double getCalculatedAverage() { return calculatedAverage; }
    public void setCalculatedAverage(Double calculatedAverage) { this.calculatedAverage = calculatedAverage; }

    public Boolean getIsGradingComplete() { return isGradingComplete; }
    public void setIsGradingComplete(Boolean isGradingComplete) { this.isGradingComplete = isGradingComplete; }
}
