package com.spms.backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.Valid;
import java.util.List;

public class GradeSubmissionRequest {

    @Min(value = 0, message = "Grade must be at least 0")
    @Max(value = 100, message = "Grade must not exceed 100")
    private Double grade;

    private String feedback;

    @Valid
    private List<CriteriaScoreRequest> criteriaScores;

    public GradeSubmissionRequest() {}

    public Double getGrade() { return grade; }
    public void setGrade(Double grade) { this.grade = grade; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public List<CriteriaScoreRequest> getCriteriaScores() { return criteriaScores; }
    public void setCriteriaScores(List<CriteriaScoreRequest> criteriaScores) { this.criteriaScores = criteriaScores; }

    @AssertTrue(message = "Either grade or criteriaScores must be provided")
    public boolean isGradeOrCriteriaScoresProvided() {
        return grade != null || (criteriaScores != null && !criteriaScores.isEmpty());
    }
}
