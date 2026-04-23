package com.spms.backend.dto.response;

public class CoordinatorStudentResponseDto {
    private Long id;
    private String fullName;
    private String email;
    private String studentId;
    private Long groupId;
    private String groupName;

    public CoordinatorStudentResponseDto() {}

    public CoordinatorStudentResponseDto(Long id, String fullName, String email, String studentId, Long groupId, String groupName) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.studentId = studentId;
        this.groupId = groupId;
        this.groupName = groupName;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
}
