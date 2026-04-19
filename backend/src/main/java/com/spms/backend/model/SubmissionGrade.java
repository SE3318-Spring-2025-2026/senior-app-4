package com.spms.backend.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "submission_grades")
public class SubmissionGrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "grade_id")
    private Long gradeId;

    @Column(name = "submission_id", nullable = false)
    private Long submissionId;

    @Column(name = "reviewer_id", nullable = false)
    private Long reviewerId;

    @Column(name = "score", nullable = false)
    private Double score;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    @Column(name = "graded_at", nullable = false)
    private Instant gradedAt;

    public SubmissionGrade() {
        this.gradedAt = Instant.now();
    }

    public SubmissionGrade(Long submissionId, Long reviewerId, Double score, String comments) {
        this.submissionId = submissionId;
        this.reviewerId = reviewerId;
        this.score = score;
        this.comments = comments;
        this.gradedAt = Instant.now();
    }

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
}
