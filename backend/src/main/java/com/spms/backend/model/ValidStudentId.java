package com.spms.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "valid_student_ids")
public class ValidStudentId {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "student_id", unique = true, nullable = false)
    private String studentId;

    @Column(name = "status")
    private String status = "valid";

    public ValidStudentId() {
    }

    public ValidStudentId(String studentId) {
        this.studentId = studentId;
        this.status = "valid";
    }

    public Long getId() {
        return id;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
