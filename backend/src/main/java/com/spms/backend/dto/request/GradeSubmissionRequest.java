package com.spms.backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;

public class GradeSubmissionRequest {

    @Min(value = 0, message = "Grade must be at least 0")
    @Max(value = 100, message = "Grade must not exceed 100")
    private Double grade;

    private String feedback;

    private List<CriterionScoreRequest> criterionScores;

    public GradeSubmissionRequest() {}

    public Double getGrade() { return grade; }
    public void setGrade(Double grade) { this.grade = grade; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public List<CriterionScoreRequest> getCriterionScores() { return criterionScores; }
    public void setCriterionScores(List<CriterionScoreRequest> criterionScores) { this.criterionScores = criterionScores; }
}
