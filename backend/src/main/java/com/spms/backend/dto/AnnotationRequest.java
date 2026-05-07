package com.spms.backend.dto;

public class AnnotationRequest {

    private Long criterionId;
    private String selectedText;
    private Integer startOffset;
    private Integer endOffset;
    private String comment;
    private String grade;

    public Long getCriterionId() { return criterionId; }
    public void setCriterionId(Long criterionId) { this.criterionId = criterionId; }

    public String getSelectedText() { return selectedText; }
    public void setSelectedText(String selectedText) { this.selectedText = selectedText; }

    public Integer getStartOffset() { return startOffset; }
    public void setStartOffset(Integer startOffset) { this.startOffset = startOffset; }

    public Integer getEndOffset() { return endOffset; }
    public void setEndOffset(Integer endOffset) { this.endOffset = endOffset; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
}
