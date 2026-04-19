package com.spms.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class GradeSubmissionRequest {
    
    @NotNull(message = "Score cannot be null")
    @Min(value = 0, message = "Score must be at least 0")
    @Max(value = 100, message = "Score must not exceed 100")
    private Double score;
    
    private String comments;

    public GradeSubmissionRequest() {}

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
}
