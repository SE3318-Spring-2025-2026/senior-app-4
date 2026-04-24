package com.spms.backend.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "student_id", unique = true)
    private String studentId;

    @Column(name = "github_username", unique = true)
    private String githubUsername;

    @Column(name = "role")
    private String role;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "requires_password_change")
    private boolean requiresPasswordChange;

    @Column(name = "current_advisee_count", nullable = false)
    private Integer currentAdviseeCount = 0;

    public User() {
    }

    public User(User other) {
        this.userId = other.userId;
        this.fullName = other.fullName;
        this.email = other.email;
        this.passwordHash = other.passwordHash;
        this.studentId = other.studentId;
        this.githubUsername = other.githubUsername;
        this.role = other.role;
        this.createdAt = other.createdAt;
        this.requiresPasswordChange = other.requiresPasswordChange;
        this.currentAdviseeCount = other.currentAdviseeCount;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getGithubUsername() {
        return githubUsername;
    }

    public void setGithubUsername(String githubUsername) {
        this.githubUsername = githubUsername;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isRequiresPasswordChange() {
        return requiresPasswordChange;
    }

    public void setRequiresPasswordChange(boolean requiresPasswordChange) {
        this.requiresPasswordChange = requiresPasswordChange;
    }

    public Integer getCurrentAdviseeCount() {
        return currentAdviseeCount;
    }

    public void setCurrentAdviseeCount(Integer currentAdviseeCount) {
        this.currentAdviseeCount = currentAdviseeCount;
    }
}
