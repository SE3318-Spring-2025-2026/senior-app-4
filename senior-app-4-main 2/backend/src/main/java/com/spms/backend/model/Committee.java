package com.spms.backend.model;

import java.time.Instant;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "committees")
public class Committee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "committee_id")
    private Long committeeId;

    @Column(name = "committee_name", nullable = false)
    private String committeeName;

    @Column(name = "description")
    private String description;

    @Column(name = "status")
    private String status; // ACTIVE, INACTIVE, COMPLETED

    @Column(name = "created_by")
    private Long createdBy; // Coordinator userId

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "committee", cascade = CascadeType.ALL)
    private List<CommitteeAdvisor> advisors;

    @OneToMany(mappedBy = "committee", cascade = CascadeType.ALL)
    private List<CommitteeJury> juryMembers;

    @OneToMany(mappedBy = "committee", cascade = CascadeType.ALL)
    private List<GroupCommitteeAssignment> groupAssignments;

    // Constructors
    public Committee() {
    }

    public Committee(String committeeName, String description, String status, Long createdBy) {
        this.committeeName = committeeName;
        this.description = description;
        this.status = status;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public Long getCommitteeId() {
        return committeeId;
    }

    public void setCommitteeId(Long committeeId) {
        this.committeeId = committeeId;
    }

    public String getCommitteeName() {
        return committeeName;
    }

    public void setCommitteeName(String committeeName) {
        this.committeeName = committeeName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<CommitteeAdvisor> getAdvisors() {
        return advisors;
    }

    public void setAdvisors(List<CommitteeAdvisor> advisors) {
        this.advisors = advisors;
    }

    public List<CommitteeJury> getJuryMembers() {
        return juryMembers;
    }

    public void setJuryMembers(List<CommitteeJury> juryMembers) {
        this.juryMembers = juryMembers;
    }

    public List<GroupCommitteeAssignment> getGroupAssignments() {
        return groupAssignments;
    }

    public void setGroupAssignments(List<GroupCommitteeAssignment> groupAssignments) {
        this.groupAssignments = groupAssignments;
    }
}
