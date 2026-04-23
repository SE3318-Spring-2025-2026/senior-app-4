package com.spms.backend.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public class GradeItemDTO {
    
    private Long id;
    private Long professorId;
    private String professorName;
    private Double grade;
    private String feedback;
    private List<CriteriaScoreDTO> criteriaScores;
    private LocalDateTime gradedAt;

    public GradeItemDTO() {}

    public GradeItemDTO(Long id, Long professorId, String professorName, Double grade, String feedback, List<CriteriaScoreDTO> criteriaScores, LocalDateTime gradedAt) {
        this.id = id;
        this.professorId = professorId;
        this.professorName = professorName;
        this.grade = grade;
        this.feedback = feedback;
        this.criteriaScores = criteriaScores;
        this.gradedAt = gradedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProfessorId() { return professorId; }
    public void setProfessorId(Long professorId) { this.professorId = professorId; }

    public String getProfessorName() { return professorName; }
    public void setProfessorName(String professorName) { this.professorName = professorName; }

    public Double getGrade() { return grade; }
    public void setGrade(Double grade) { this.grade = grade; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public List<CriteriaScoreDTO> getCriteriaScores() { return criteriaScores; }
    public void setCriteriaScores(List<CriteriaScoreDTO> criteriaScores) { this.criteriaScores = criteriaScores; }

    public LocalDateTime getGradedAt() { return gradedAt; }
    public void setGradedAt(LocalDateTime gradedAt) { this.gradedAt = gradedAt; }
}
