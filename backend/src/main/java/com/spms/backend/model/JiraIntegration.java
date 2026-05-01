package com.spms.backend.model;

import com.spms.backend.converter.EncryptionConverter;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "jira_integrations")
public class JiraIntegration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false, unique = true)
    private Group group;

    @Column(name = "jira_space_url", nullable = false)
    private String jiraSpaceUrl;

    @Convert(converter = EncryptionConverter.class)
    @Column(name = "api_key")
    private String apiKey;

    @Column(name = "project_key", nullable = false)
    private String projectKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JiraIntegrationStatus status = JiraIntegrationStatus.ACTIVE;

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

    public String getJiraSpaceUrl() {
        return jiraSpaceUrl;
    }

    public void setJiraSpaceUrl(String jiraSpaceUrl) {
        this.jiraSpaceUrl = jiraSpaceUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public void setProjectKey(String projectKey) {
        this.projectKey = projectKey;
    }

    public JiraIntegrationStatus getStatus() {
        return status;
    }

    public void setStatus(JiraIntegrationStatus status) {
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
