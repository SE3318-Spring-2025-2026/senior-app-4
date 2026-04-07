package com.spms.backend.model;

import java.time.Instant;

public class User {

    private Long userId;
    private String fullName;
    private String email;
    private String studentId;
    private String githubUsername;
    private String role;
    private Instant createdAt;

    public User() {
    }

    public User(User other) {
        this.userId = other.userId;
        this.fullName = other.fullName;
        this.email = other.email;
        this.studentId = other.studentId;
        this.githubUsername = other.githubUsername;
        this.role = other.role;
        this.createdAt = other.createdAt;
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
}
