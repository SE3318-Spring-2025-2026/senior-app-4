package com.spms.backend.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "grades")
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "submission_id", nullable = false)
    private Long submissionId;

    @Column(name = "professor_id", nullable = false)
    private Long professorId;

    @Column(name = "score", nullable = false)
    private Double score;

    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "graded_at")
    private Instant gradedAt;

    public Grade() {
        this.gradedAt = Instant.now();
    }

    public Grade(Long submissionId, Long professorId, Double score, String feedback) {
        this.submissionId = submissionId;
        this.professorId = professorId;
        this.score = score;
        this.feedback = feedback;
        this.gradedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSubmissionId() { return submissionId; }
    public void setSubmissionId(Long submissionId) { this.submissionId = submissionId; }

    public Long getProfessorId() { return professorId; }
    public void setProfessorId(Long professorId) { this.professorId = professorId; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public Instant getGradedAt() { return gradedAt; }
    public void setGradedAt(Instant gradedAt) { this.gradedAt = gradedAt; }
}
