package com.spms.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "grade_criterion_scores")
public class GradeCriterionScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "grade_id", nullable = false)
    private Long gradeId;

    @Column(name = "criterion_id", nullable = false)
    private Long criterionId;

    @Column(name = "score", nullable = false)
    private Double score;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getGradeId() { return gradeId; }
    public void setGradeId(Long gradeId) { this.gradeId = gradeId; }

    public Long getCriterionId() { return criterionId; }
    public void setCriterionId(Long criterionId) { this.criterionId = criterionId; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
}
