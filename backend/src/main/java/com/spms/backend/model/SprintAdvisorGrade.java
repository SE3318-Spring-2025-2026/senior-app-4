package com.spms.backend.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "sprint_advisor_grades", uniqueConstraints = {
        @UniqueConstraint(name = "uq_sprint_advisor_grade", columnNames = {"group_id", "sprint_id", "advisor_id"})
})
public class SprintAdvisorGrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id", nullable = false)
    private Sprint sprint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "advisor_id", nullable = false)
    private User advisor;

    @Column(name = "scrum_grade", nullable = false, length = 1)
    private String scrumGrade;

    @Column(name = "code_review_grade", nullable = false, length = 1)
    private String codeReviewGrade;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Group getGroup() { return group; }
    public void setGroup(Group group) { this.group = group; }
    public Sprint getSprint() { return sprint; }
    public void setSprint(Sprint sprint) { this.sprint = sprint; }
    public User getAdvisor() { return advisor; }
    public void setAdvisor(User advisor) { this.advisor = advisor; }
    public String getScrumGrade() { return scrumGrade; }
    public void setScrumGrade(String scrumGrade) { this.scrumGrade = scrumGrade; }
    public String getCodeReviewGrade() { return codeReviewGrade; }
    public void setCodeReviewGrade(String codeReviewGrade) { this.codeReviewGrade = codeReviewGrade; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
