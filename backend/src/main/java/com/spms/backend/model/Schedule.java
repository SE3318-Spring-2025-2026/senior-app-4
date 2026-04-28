package com.spms.backend.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "schedule")
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "group_formation_deadline", nullable = false)
    private Instant groupFormationDeadline;

    @Column(name = "advisor_assignment_deadline", nullable = false)
    private Instant advisorAssignmentDeadline;

    @Column(name = "proposal_revision_deadline")
    private Instant proposalRevisionDeadline;

    @Column(name = "grading_deadline")
    private Instant gradingDeadline;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public Schedule() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Instant getGroupFormationDeadline() { return groupFormationDeadline; }
    public void setGroupFormationDeadline(Instant groupFormationDeadline) {
        this.groupFormationDeadline = groupFormationDeadline;
    }

    public Instant getAdvisorAssignmentDeadline() { return advisorAssignmentDeadline; }
    public void setAdvisorAssignmentDeadline(Instant advisorAssignmentDeadline) {
        this.advisorAssignmentDeadline = advisorAssignmentDeadline;
    }

    public Instant getProposalRevisionDeadline() { return proposalRevisionDeadline; }
    public void setProposalRevisionDeadline(Instant proposalRevisionDeadline) {
        this.proposalRevisionDeadline = proposalRevisionDeadline;
    }

    public Instant getGradingDeadline() { return gradingDeadline; }
    public void setGradingDeadline(Instant gradingDeadline) {
        this.gradingDeadline = gradingDeadline;
    }

    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
