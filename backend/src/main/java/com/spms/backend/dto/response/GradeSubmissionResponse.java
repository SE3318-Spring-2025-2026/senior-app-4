package com.spms.backend.dto.response;

public class GradeSubmissionResponse {

    private String status;
    private String message;
    private GradeData data;

    public static class GradeData {
        private Long gradeId;
        private Boolean isGradingComplete;

        public GradeData(Long gradeId, Boolean isGradingComplete) {
            this.gradeId = gradeId;
            this.isGradingComplete = isGradingComplete;
        }

        public Long getGradeId() {
            return gradeId;
        }

        public void setGradeId(Long gradeId) {
            this.gradeId = gradeId;
        }

        public Boolean getIsGradingComplete() {
            return isGradingComplete;
        }

        public void setIsGradingComplete(Boolean isGradingComplete) {
            this.isGradingComplete = isGradingComplete;
        }
    }

    public GradeSubmissionResponse() {
    }

    public GradeSubmissionResponse(String status, String message, Long gradeId, Boolean isGradingComplete) {
        this.status = status;
        this.message = message;
        this.data = new GradeData(gradeId, isGradingComplete);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public GradeData getData() {
        return data;
    }

    public void setData(GradeData data) {
        this.data = data;
    }
}
