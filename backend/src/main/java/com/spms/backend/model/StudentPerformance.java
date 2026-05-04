package com.spms.backend.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "student_performances")
public class StudentPerformance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    private Integer assignedSp;
    private Integer accomplishedSp;
    @Column(name = "performance_ratio")
    private Double ratio;
    private Instant lastUpdatedAt;

    public StudentPerformance() {}

    public StudentPerformance(User user, Integer assignedSp, Integer accomplishedSp, Double ratio) {
        this.user = user;
        this.assignedSp = assignedSp;
        this.accomplishedSp = accomplishedSp;
        this.ratio = ratio;
        this.lastUpdatedAt = Instant.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Integer getAssignedSp() { return assignedSp; }
    public void setAssignedSp(Integer assignedSp) { this.assignedSp = assignedSp; }

    public Integer getAccomplishedSp() { return accomplishedSp; }
    public void setAccomplishedSp(Integer accomplishedSp) { this.accomplishedSp = accomplishedSp; }

    public Double getRatio() { return ratio; }
    public void setRatio(Double ratio) { this.ratio = ratio; }

    public Instant getLastUpdatedAt() { return lastUpdatedAt; }
    public void setLastUpdatedAt(Instant lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }
}
