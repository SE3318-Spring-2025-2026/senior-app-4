package com.spms.backend.controller;

import com.spms.backend.dto.request.StudentIdCreateRequest;
import com.spms.backend.dto.response.DeleteResponse;
import com.spms.backend.dto.response.StudentIdListResponse;
import com.spms.backend.dto.response.StudentIdResponse;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.exception.DuplicateUserException;
import com.spms.backend.repository.ValidStudentIdRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * D2 Data Store — Geçerli Öğrenci ID'leri CRUD endpoint'leri.
 *
 * Spec: /api/v1/student-ids
 */
@RestController
@RequestMapping("/api/v1/student-ids")
public class StudentIdController {

    private final ValidStudentIdRepository validStudentIdRepository;

    public StudentIdController(ValidStudentIdRepository validStudentIdRepository) {
        this.validStudentIdRepository = validStudentIdRepository;
    }

    // ── GET /api/v1/student-ids ──
    @GetMapping
    public ResponseEntity<StudentIdListResponse> getAllStudentIds() {
        List<Map<String, String>> data = validStudentIdRepository.findAll();
        return ResponseEntity.ok(new StudentIdListResponse(
                "Student IDs retrieved successfully.", data.size(), data
        ));
    }

    // ── POST /api/v1/student-ids ──
    @PostMapping
    public ResponseEntity<StudentIdResponse> addStudentId(
            @Valid @RequestBody StudentIdCreateRequest request
    ) {
        if (validStudentIdRepository.existsByStudentId(request.studentId())) {
            throw new DuplicateUserException("Student ID already exists.");
        }
        validStudentIdRepository.save(request.studentId());
        Map<String, String> data = Map.of(
                "studentId", request.studentId(),
                "status", "valid"
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new StudentIdResponse("Student ID added successfully.", data)
        );
    }

    // ── GET /api/v1/student-ids/{studentId} ──
    @GetMapping("/{studentId}")
    public ResponseEntity<StudentIdResponse> getStudentId(@PathVariable String studentId) {
        Map<String, String> data = validStudentIdRepository.findByStudentId(studentId);
        if (data == null) {
            throw new BadRequestException("Student ID not found.");
        }
        return ResponseEntity.ok(new StudentIdResponse(
                "Student ID retrieved successfully.", data
        ));
    }

    // ── DELETE /api/v1/student-ids/{studentId} ──
    @DeleteMapping("/{studentId}")
    public ResponseEntity<DeleteResponse> deleteStudentId(@PathVariable String studentId) {
        boolean deleted = validStudentIdRepository.deleteByStudentId(studentId);
        if (!deleted) {
            throw new BadRequestException("Student ID not found.");
        }
        return ResponseEntity.ok(new DeleteResponse("Resource deleted successfully."));
    }
}
