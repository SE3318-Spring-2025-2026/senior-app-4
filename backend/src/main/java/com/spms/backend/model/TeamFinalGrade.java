package com.spms.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "team_final_grades")
public class TeamFinalGrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false, unique = true)
    private Group group;

    @Column(name = "team_grade", precision = 6, scale = 2)
    private BigDecimal teamGrade;

    @Column(name = "published", nullable = false)
    private boolean published;

    @Column(name = "computed_at")
    private Instant computedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Group getGroup() { return group; }
    public void setGroup(Group group) { this.group = group; }
    public BigDecimal getTeamGrade() { return teamGrade; }
    public void setTeamGrade(BigDecimal teamGrade) { this.teamGrade = teamGrade; }
    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
    public Instant getComputedAt() { return computedAt; }
    public void setComputedAt(Instant computedAt) { this.computedAt = computedAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }
}
