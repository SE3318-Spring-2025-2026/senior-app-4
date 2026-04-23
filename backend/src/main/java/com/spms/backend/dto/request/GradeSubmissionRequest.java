package com.spms.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class GradeSubmissionRequest {
    
    @NotNull(message = "Grade cannot be null")
    @Min(value = 0, message = "Grade must be at least 0")
    @Max(value = 100, message = "Grade must not exceed 100")
    private Double grade;
    
    private String comments;

    public GradeSubmissionRequest() {}

    public Double getGrade() { return grade; }
    public void setGrade(Double grade) { this.grade = grade; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
}
