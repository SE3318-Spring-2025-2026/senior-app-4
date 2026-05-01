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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "committee_advisors", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"committee_id", "advisor_id"})
})
public class CommitteeAdvisor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "committee_advisor_id")
    private Long committeeAdvisorId;

    @ManyToOne
    @JoinColumn(name = "committee_id", nullable = false)
    private Committee committee;

    @ManyToOne
    @JoinColumn(name = "advisor_id", nullable = false)
    private User advisor;

    @Column(name = "role")
    private String role; // PRESIDENT, VICE_PRESIDENT, MEMBER

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "assigned_by")
    private Long assignedBy;

    // Constructors
    public CommitteeAdvisor() {
    }

    public CommitteeAdvisor(Committee committee, User advisor, String role, Long assignedBy) {
        this.committee = committee;
        this.advisor = advisor;
        this.role = role;
        this.assignedBy = assignedBy;
        this.assignedAt = Instant.now();
    }

    // Getters and Setters
    public Long getCommitteeAdvisorId() {
        return committeeAdvisorId;
    }

    public void setCommitteeAdvisorId(Long committeeAdvisorId) {
        this.committeeAdvisorId = committeeAdvisorId;
    }

    public Committee getCommittee() {
        return committee;
    }

    public void setCommittee(Committee committee) {
        this.committee = committee;
    }

    public User getAdvisor() {
        return advisor;
    }

    public void setAdvisor(User advisor) {
        this.advisor = advisor;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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
