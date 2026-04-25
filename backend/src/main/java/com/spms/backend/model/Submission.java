package com.spms.backend.model;

import com.spms.backend.model.enums.DeliverableType;
import com.spms.backend.model.enums.SubmissionStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "deliverables")
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Enumerated(EnumType.STRING)
    @Column(name = "deliverable_type", nullable = false)
    private DeliverableType deliverableType;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "file_url", nullable = true)
    private String fileUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SubmissionStatus status;

    @Column(name = "committee_id")
    private Long committeeId;

    @Column(name = "final_grade")
    private Double finalGrade;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * ID of the parent (original) submission this record is a revision of.
     * Null for first-version submissions.
     * Column: parent_submission_id (already exists in deliverables table)
     */
    @Column(name = "parent_submission_id")
    private Long parentSubmissionId;

    /**
     * Revision version number. Default 1 for original submissions.
     * Auto-incremented (parent.version + 1) on each revision.
     * Column: version (already exists in deliverables table, default 1)
     */
    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.version == null) {
            this.version = 1;
        }
    }

    public Submission() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public DeliverableType getDeliverableType() { return deliverableType; }
    public void setDeliverableType(DeliverableType deliverableType) { this.deliverableType = deliverableType; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public SubmissionStatus getStatus() { return status; }
    public void setStatus(SubmissionStatus status) { this.status = status; }

    public Long getCommitteeId() { return committeeId; }
    public void setCommitteeId(Long committeeId) { this.committeeId = committeeId; }

    public Double getFinalGrade() { return finalGrade; }
    public void setFinalGrade(Double finalGrade) { this.finalGrade = finalGrade; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Long getParentSubmissionId() { return parentSubmissionId; }
    public void setParentSubmissionId(Long parentSubmissionId) { this.parentSubmissionId = parentSubmissionId; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
