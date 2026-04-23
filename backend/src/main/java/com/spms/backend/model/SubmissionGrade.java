package com.spms.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "submission_grades")
public class SubmissionGrade {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "grade_id")
    private Long id;

    @Column(name = "submission_id", nullable = false)
    private Long submissionId;

    @Column(name = "reviewer_id", nullable = false)
    private Long reviewerId;

    @Transient
    private String professorName;

    @Column(name = "score", nullable = false)
    private Double score;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    @Column(name = "graded_at", nullable = false, updatable = false)
    private LocalDateTime gradedAt;

    public SubmissionGrade() {
        this.gradedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSubmissionId() { return submissionId; }
    public void setSubmissionId(Long submissionId) { this.submissionId = submissionId; }

    public Long getReviewerId() { return reviewerId; }
    public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }

    public String getProfessorName() { return professorName; }
    public void setProfessorName(String professorName) { this.professorName = professorName; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public LocalDateTime getGradedAt() { return gradedAt; }
    public void setGradedAt(LocalDateTime gradedAt) { this.gradedAt = gradedAt; }
}
