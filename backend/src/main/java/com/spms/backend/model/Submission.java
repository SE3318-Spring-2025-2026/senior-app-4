package com.spms.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "deliverables")
public class Submission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "deliverable_type", nullable = false)
    private String deliverableType;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SubmissionStatus status;

    @Column(name = "committee_id")
    private Long committeeId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

    @Column(name = "final_grade")
    private Double finalGrade;

    @Column(name = "file_url", columnDefinition = "TEXT")
    private String fileUrl;

    // Revision fields for Issue #150
    @Column(name = "parent_submission_id")
    private Long parentSubmissionId;

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = java.time.LocalDateTime.now();
        }
    }

    public Submission() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public String getDeliverableType() { return deliverableType; }
    public void setDeliverableType(String deliverableType) { this.deliverableType = deliverableType; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public SubmissionStatus getStatus() { return status; }
    public void setStatus(SubmissionStatus status) { this.status = status; }

    public Long getCommitteeId() { return committeeId; }
    public void setCommitteeId(Long committeeId) { this.committeeId = committeeId; }

    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Double getFinalGrade() { return finalGrade; }
    public void setFinalGrade(Double finalGrade) { this.finalGrade = finalGrade; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public Long getParentSubmissionId() { return parentSubmissionId; }
    public void setParentSubmissionId(Long parentSubmissionId) { this.parentSubmissionId = parentSubmissionId; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
