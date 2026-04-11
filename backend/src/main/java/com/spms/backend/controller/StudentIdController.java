package com.spms.backend.controller;

import com.spms.backend.dto.request.StudentIdCreateRequest;
import com.spms.backend.dto.response.DeleteResponse;
import com.spms.backend.dto.response.StudentIdListResponse;
import com.spms.backend.dto.response.StudentIdResponse;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.exception.DuplicateUserException;
import com.spms.backend.model.ValidStudentId;
import com.spms.backend.repository.ValidStudentIdRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "Student ID Store")
@RestController
@RequestMapping("/api/v1/student-ids")
public class StudentIdController {

    private final ValidStudentIdRepository validStudentIdRepository;

    public StudentIdController(ValidStudentIdRepository validStudentIdRepository) {
        this.validStudentIdRepository = validStudentIdRepository;
    }

    @Operation(summary = "List all valid student IDs")
    @GetMapping
    public ResponseEntity<StudentIdListResponse> getAllStudentIds() {
        List<Map<String, String>> data = validStudentIdRepository.findAll().stream()
                .map(e -> Map.of("studentId", e.getStudentId(), "status", e.getStatus()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(new StudentIdListResponse(
                "Student IDs retrieved successfully.", data.size(), data
        ));
    }

    @Operation(summary = "Add a single valid student ID")
    @PostMapping
    public ResponseEntity<StudentIdResponse> addStudentId(
            @Valid @RequestBody StudentIdCreateRequest request
    ) {
        if (validStudentIdRepository.existsByStudentId(request.studentId())) {
            throw new DuplicateUserException("Student ID already exists.");
        }
        validStudentIdRepository.saveStudentId(request.studentId());
        Map<String, String> data = Map.of(
                "studentId", request.studentId(),
                "status", "valid"
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new StudentIdResponse("Student ID added successfully.", data)
        );
    }

    @Operation(summary = "Get a student ID by value")
    @GetMapping("/{studentId}")
    public ResponseEntity<StudentIdResponse> getStudentId(@PathVariable String studentId) {
        ValidStudentId entity = validStudentIdRepository.findByStudentId(studentId)
                .orElseThrow(() -> new BadRequestException("Student ID not found."));
        Map<String, String> data = Map.of(
                "studentId", entity.getStudentId(),
                "status", entity.getStatus()
        );
        return ResponseEntity.ok(new StudentIdResponse(
                "Student ID retrieved successfully.", data
        ));
    }

    @Operation(summary = "Delete a student ID by value")
    @DeleteMapping("/{studentId}")
    public ResponseEntity<DeleteResponse> deleteStudentId(@PathVariable String studentId) {
        boolean deleted = validStudentIdRepository.deleteByStudentId(studentId);
        if (!deleted) {
            throw new BadRequestException("Student ID not found.");
        }
        return ResponseEntity.ok(new DeleteResponse("Resource deleted successfully."));
    }
}
