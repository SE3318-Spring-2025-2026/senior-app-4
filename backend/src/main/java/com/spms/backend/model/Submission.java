package com.spms.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "submissions")
public class Submission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_submission_id")
    private Long parentSubmissionId;

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SubmissionStatus status;

    // #todo: Add other necessary fields (e.g. groupId, fileUrl, timestamps) in later processes.

    public Submission() {}

    public Submission(Long parentSubmissionId, Integer version, SubmissionStatus status) {
        this.parentSubmissionId = parentSubmissionId;
        this.version = version;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getParentSubmissionId() { return parentSubmissionId; }
    public void setParentSubmissionId(Long parentSubmissionId) { this.parentSubmissionId = parentSubmissionId; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public SubmissionStatus getStatus() { return status; }
    public void setStatus(SubmissionStatus status) { this.status = status; }
}
