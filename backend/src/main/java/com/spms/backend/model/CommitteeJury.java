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
@Table(name = "committee_jury_members", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"committee_id", "jury_user_id"})
})
public class CommitteeJury {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "jury_member_id")
    private Long juryMemberId;

    @ManyToOne
    @JoinColumn(name = "committee_id", nullable = false)
    private Committee committee;

    @ManyToOne
    @JoinColumn(name = "jury_user_id", nullable = false)
    private User juryMember;

    @Column(name = "jury_type")
    private String juryType; // INTERNAL, EXTERNAL, ADDITIONAL

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "assigned_by")
    private Long assignedBy;

    // Constructors
    public CommitteeJury() {
    }

    public CommitteeJury(Committee committee, User juryMember, String juryType, Long assignedBy) {
        this.committee = committee;
        this.juryMember = juryMember;
        this.juryType = juryType;
        this.assignedBy = assignedBy;
        this.assignedAt = Instant.now();
    }

    // Getters and Setters
    public Long getJuryMemberId() {
        return juryMemberId;
    }

    public void setJuryMemberId(Long juryMemberId) {
        this.juryMemberId = juryMemberId;
    }

    public Committee getCommittee() {
        return committee;
    }

    public void setCommittee(Committee committee) {
        this.committee = committee;
    }

    public User getJuryMember() {
        return juryMember;
    }

    public void setJuryMember(User juryMember) {
        this.juryMember = juryMember;
    }

    public String getJuryType() {
        return juryType;
    }

    public void setJuryType(String juryType) {
        this.juryType = juryType;
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
