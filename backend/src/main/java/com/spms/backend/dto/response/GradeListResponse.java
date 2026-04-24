package com.spms.backend.dto.response;

import java.util.List;

public class GradeListResponse {
    
    private String status;
    private GradeListData data;

    public GradeListResponse(String status, GradeListData data) {
        this.status = status;
        this.data = data;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public GradeListData getData() { return data; }
    public void setData(GradeListData data) { this.data = data; }

    public static class GradeListData {
        private Double averageGrade;
        private Integer gradeCount;
        private Integer totalCommitteeMembers;
        private Boolean isGradingComplete;
        private List<GradeItemDTO> grades;

        public GradeListData() {}

        public GradeListData(Double averageGrade, Integer gradeCount, Integer totalCommitteeMembers, Boolean isGradingComplete, List<GradeItemDTO> grades) {
            this.averageGrade = averageGrade;
            this.gradeCount = gradeCount;
            this.totalCommitteeMembers = totalCommitteeMembers;
            this.isGradingComplete = isGradingComplete;
            this.grades = grades;
        }

        public Double getAverageGrade() { return averageGrade; }
        public void setAverageGrade(Double averageGrade) { this.averageGrade = averageGrade; }

        public Integer getGradeCount() { return gradeCount; }
        public void setGradeCount(Integer gradeCount) { this.gradeCount = gradeCount; }

        public Integer getTotalCommitteeMembers() { return totalCommitteeMembers; }
        public void setTotalCommitteeMembers(Integer totalCommitteeMembers) { this.totalCommitteeMembers = totalCommitteeMembers; }

        public Boolean getIsGradingComplete() { return isGradingComplete; }
        public void setIsGradingComplete(Boolean isGradingComplete) { this.isGradingComplete = isGradingComplete; }

        public List<GradeItemDTO> getGrades() { return grades; }
        public void setGrades(List<GradeItemDTO> grades) { this.grades = grades; }
    }
}
