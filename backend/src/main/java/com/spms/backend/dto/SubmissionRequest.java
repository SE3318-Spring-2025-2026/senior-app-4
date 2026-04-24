package com.spms.backend.dto;

import com.spms.backend.model.enums.DeliverableType;

public class SubmissionRequest {
    private Long groupId;
    private DeliverableType type;
    private String content;

    // Default constructor
    public SubmissionRequest() {}

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public DeliverableType getType() {
        return type;
    }

    public void setType(DeliverableType type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
