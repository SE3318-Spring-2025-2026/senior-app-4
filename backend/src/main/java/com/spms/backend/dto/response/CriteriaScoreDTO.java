package com.spms.backend.dto.response;

public class CriteriaScoreDTO {
    private Long criteriaId;
    private String criteriaName;
    private Double score;

    public CriteriaScoreDTO() {}

    public CriteriaScoreDTO(Long criteriaId, String criteriaName, Double score) {
        this.criteriaId = criteriaId;
        this.criteriaName = criteriaName;
        this.score = score;
    }

    public Long getCriteriaId() { return criteriaId; }
    public void setCriteriaId(Long criteriaId) { this.criteriaId = criteriaId; }

    public String getCriteriaName() { return criteriaName; }
    public void setCriteriaName(String criteriaName) { this.criteriaName = criteriaName; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
}
