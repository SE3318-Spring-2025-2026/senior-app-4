package com.spms.backend.model;

import com.spms.backend.model.enums.DeliverableType;
import com.spms.backend.model.enums.SubmissionStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "submissions")
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Enumerated(EnumType.STRING)
    @Column(name = "deliverable_type", nullable = false)
    private DeliverableType deliverableType;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SubmissionStatus status;

    @Column(name = "committee_id")
    private Long committeeId;

    @Column(name = "final_grade")
    private Double finalGrade;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Submission() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    
    public DeliverableType getDeliverableType() { return deliverableType; }
    public void setDeliverableType(DeliverableType deliverableType) { this.deliverableType = deliverableType; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public SubmissionStatus getStatus() { return status; }
    public void setStatus(SubmissionStatus status) { this.status = status; }
    
    public Long getCommitteeId() { return committeeId; }
    public void setCommitteeId(Long committeeId) { this.committeeId = committeeId; }

    public Double getFinalGrade() { return finalGrade; }
    public void setFinalGrade(Double finalGrade) { this.finalGrade = finalGrade; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
