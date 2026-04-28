package com.spms.backend.model;

import com.spms.backend.model.enums.DeliverableType;
import com.spms.backend.model.enums.GradingType;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "grading_criteria")
public class GradingCriteria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "deliverable_type", nullable = false, length = 50)
    private DeliverableType deliverableType;

    @Enumerated(EnumType.STRING)
    @Column(name = "grading_type", length = 20)
    private GradingType gradingType;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "weight", nullable = false)
    private Double weight;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public DeliverableType getDeliverableType() { return deliverableType; }
    public void setDeliverableType(DeliverableType deliverableType) { this.deliverableType = deliverableType; }

    public GradingType getGradingType() { return gradingType; }
    public void setGradingType(GradingType gradingType) { this.gradingType = gradingType; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
}
