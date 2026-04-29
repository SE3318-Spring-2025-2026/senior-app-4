package com.spms.backend.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "group_committee_assignments")
public class GroupCommitteeAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignment_id")
    private Long assignmentId;

    @ManyToOne
    @JoinColumn(name = "committee_id", nullable = false)
    private Committee committee;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "status")
    private String status; // ASSIGNED, SCHEDULED, COMPLETED, CANCELLED

    @Column(name = "exam_date")
    private Instant examDate;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "assigned_by")
    private Long assignedBy;

    // Constructors
    public GroupCommitteeAssignment() {
    }

    public GroupCommitteeAssignment(Committee committee, Long groupId, String status, Long assignedBy) {
        this.committee = committee;
        this.groupId = groupId;
        this.status = status;
        this.assignedBy = assignedBy;
        this.assignedAt = Instant.now();
    }

    // Getters and Setters
    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public Committee getCommittee() {
        return committee;
    }

    public void setCommittee(Committee committee) {
        this.committee = committee;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getExamDate() {
        return examDate;
    }

    public void setExamDate(Instant examDate) {
        this.examDate = examDate;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(Instant assignedAt) {
        this.assignedAt = assignedAt;
    }

    public Long getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(Long assignedBy) {
        this.assignedBy = assignedBy;
    }
}
