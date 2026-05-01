package com.spms.backend.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "group_committee_assignments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_committee_group",
                        columnNames = {"committee_id", "group_id"})
        },
        indexes = {
                @Index(name = "idx_gca_committee", columnList = "committee_id"),
                @Index(name = "idx_gca_group", columnList = "group_id"),
                @Index(name = "idx_gca_exam_date", columnList = "exam_date")
        }
)
public class GroupCommitteeAssignment {

    public static final String STATUS_ASSIGNED = "ASSIGNED";
    public static final String STATUS_SCHEDULED = "SCHEDULED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignment_id")
    private Long assignmentId;

    @ManyToOne
    @JoinColumn(name = "committee_id", nullable = false)
    private Committee committee;

    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false, insertable = false, updatable = false)
    private Group group;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "exam_date")
    private Instant examDate;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    @Column(name = "assigned_by")
    private Long assignedBy;

    public GroupCommitteeAssignment() {
    }

    public GroupCommitteeAssignment(Committee committee, Long groupId, String status, Long assignedBy) {
        this.committee = committee;
        this.groupId = groupId;
        this.status = status;
        this.assignedBy = assignedBy;
        this.assignedAt = Instant.now();
    }

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

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
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
