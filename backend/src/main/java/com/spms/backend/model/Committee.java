package com.spms.backend.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


import org.hibernate.annotations.BatchSize;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.spms.backend.model.enums.CommitteeStatus;

@Entity
@Table(name = "committees", indexes = {
    @Index(name = "idx_committee_id", columnList = "committee_id"),
    @Index(name = "idx_committee_status", columnList = "status"),
    @Index(name = "idx_committee_created_by", columnList = "created_by")
})
public class Committee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "committee_id")
    private Long committeeId;

    @NotBlank(message = "{committee.name.required}")
    @Size(min = 3, max = 100, message = "{committee.name.size}")
    @Pattern(regexp = "^[a-zA-Z0-9 ]+$", message = "{committee.name.invalid}")
    @Column(name = "committee_name", nullable = false)
    private String committeeName;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CommitteeStatus status; // ACTIVE, INACTIVE, COMPLETED

    @Column(name = "created_by")
    private Long createdBy; // Coordinator userId

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Integer version;

    @OneToMany(mappedBy = "committee", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20) // YENİ: Verileri 20'şerli paketler halinde hızlıca getirir
    private List<CommitteeAdvisor> advisors = new ArrayList<>();

    @OneToMany(mappedBy = "committee", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20) // YENİ
    private List<CommitteeJury> juryMembers = new ArrayList<>();

    @OneToMany(mappedBy = "committee")
    @BatchSize(size = 20) // YENİ
    private List<GroupCommitteeAssignment> groupAssignments = new ArrayList<>();

    // Constructors
    public Committee() {
    }

    public Committee(String committeeName, String description, CommitteeStatus status, Long createdBy) {
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

    public CommitteeStatus getStatus() {
        return status;
    }

    public void setStatus(CommitteeStatus status) {
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

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}