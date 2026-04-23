package com.spms.backend.model;
import com.spms.backend.converter.EncryptionConverter;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "github_integrations")
public class GithubIntegration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false, unique = true)
    private Group group;

    @Column(name = "organization_name")
    private String organizationName;

    
    @Convert(converter = EncryptionConverter.class)
    @Column(name = "github_pat_encrypted")
    private String githubPatEncrypted;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GithubIntegrationStatus status = GithubIntegrationStatus.INACTIVE;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getGithubPatEncrypted() {
        return githubPatEncrypted;
    }

    public void setGithubPatEncrypted(String githubPatEncrypted) {
        this.githubPatEncrypted = githubPatEncrypted;
    }

    public GithubIntegrationStatus getStatus() {
        return status;
    }

    public void setStatus(GithubIntegrationStatus status) {
        this.status = status;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
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
}