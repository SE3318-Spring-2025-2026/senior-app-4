package com.spms.backend.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public class CriteriaScoreRequest {

    @NotNull(message = "criterionId is required")
    private Long criterionId;

    @DecimalMin(value = "0.0", message = "score must be >= 0")
    @DecimalMax(value = "100.0", message = "score must be <= 100")
    private Double score;

    private String grade;

    public Long getCriterionId() { return criterionId; }
    public void setCriterionId(Long criterionId) { this.criterionId = criterionId; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
}
