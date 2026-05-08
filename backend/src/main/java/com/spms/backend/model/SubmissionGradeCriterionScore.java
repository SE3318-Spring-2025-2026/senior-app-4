package com.spms.backend.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "grade_criteria_scores", uniqueConstraints = {
        @UniqueConstraint(name = "uq_grade_criterion", columnNames = {"grade_id", "criterion_id"})
})
public class SubmissionGradeCriterionScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grade_id", nullable = false)
    private SubmissionGrade grade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criterion_id", nullable = false)
    private GradingCriteria criterion;

    @Column(name = "raw_score", nullable = false)
    private Double rawScore;

    @Column(name = "weighted_score", nullable = false)
    private Double weightedScore;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public SubmissionGrade getGrade() { return grade; }
    public void setGrade(SubmissionGrade grade) { this.grade = grade; }
    public GradingCriteria getCriterion() { return criterion; }
    public void setCriterion(GradingCriteria criterion) { this.criterion = criterion; }
    public Double getRawScore() { return rawScore; }
    public void setRawScore(Double rawScore) { this.rawScore = rawScore; }
    public Double getWeightedScore() { return weightedScore; }
    public void setWeightedScore(Double weightedScore) { this.weightedScore = weightedScore; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
