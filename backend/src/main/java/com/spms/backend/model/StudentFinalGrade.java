package com.spms.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "student_final_grades", uniqueConstraints = {
        @UniqueConstraint(name = "uq_student_final_grade", columnNames = {"group_id", "user_id"})
})
public class StudentFinalGrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "final_grade", precision = 6, scale = 2)
    private BigDecimal finalGrade;

    @Column(name = "sp_ratio", precision = 5, scale = 4)
    private BigDecimal spRatio;

    @Column(name = "published", nullable = false)
    private boolean published;

    @Column(name = "computed_at")
    private Instant computedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Group getGroup() { return group; }
    public void setGroup(Group group) { this.group = group; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public BigDecimal getFinalGrade() { return finalGrade; }
    public void setFinalGrade(BigDecimal finalGrade) { this.finalGrade = finalGrade; }
    public BigDecimal getSpRatio() { return spRatio; }
    public void setSpRatio(BigDecimal spRatio) { this.spRatio = spRatio; }
    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
    public Instant getComputedAt() { return computedAt; }
    public void setComputedAt(Instant computedAt) { this.computedAt = computedAt; }
}
