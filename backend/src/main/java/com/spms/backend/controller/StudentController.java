package com.spms.backend.controller;

import com.spms.backend.dto.request.StudentValidationRequest;
import com.spms.backend.dto.response.StudentValidationResponse;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.repository.ValidStudentIdRepository;
import com.spms.backend.service.StudentRegistrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Student Authentication + CSV Upload endpoints.
 *
 * POST /api/v1/students/validate    — ID doğrulama
 * POST /api/v1/students/ids/upload  — CSV ile toplu ID yükleme
 */
@RestController
@RequestMapping("/api/v1/students")
public class StudentController {

    private final StudentRegistrationService studentRegistrationService;
    private final ValidStudentIdRepository validStudentIdRepository;

    public StudentController(StudentRegistrationService studentRegistrationService,
                             ValidStudentIdRepository validStudentIdRepository) {
        this.studentRegistrationService = studentRegistrationService;
        this.validStudentIdRepository = validStudentIdRepository;
    }

    // ── POST /api/v1/students/validate ──
    @PostMapping("/validate")
    public ResponseEntity<StudentValidationResponse> validateStudent(
            @RequestBody StudentValidationRequest request
    ) {
        boolean exists = studentRegistrationService.validateStudent(request.getStudentId());

        if (exists) {
            return ResponseEntity.ok(new StudentValidationResponse(true));
        }

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new StudentValidationResponse(false));
    }

    // ── POST /api/v1/students/ids/upload ──
    @PostMapping("/ids/upload")
    public ResponseEntity<Map<String, Object>> uploadStudentIds(
            @RequestParam("file") MultipartFile file
    ) {
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty.");
        }

        List<String> validLines = new ArrayList<>();
        int invalidCount = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;

                // Basit doğrulama: sadece rakamlardan oluşmalı, en az 5 karakter
                if (trimmed.matches("\\d{11}")) {
                    validLines.add(trimmed);
                } else {
                    invalidCount++;
                }
            }
        } catch (Exception e) {
            throw new BadRequestException("Failed to process file.");
        }

        // Geçerli ID'leri veritabanına kaydet
        for (String studentId : validLines) {
            if (!validStudentIdRepository.existsByStudentId(studentId)) {
                validStudentIdRepository.save(studentId);
            }
        }

        return ResponseEntity.ok(Map.of(
                "message", "Student ID list uploaded successfully.",
                "totalRecords", validLines.size() + invalidCount,
                "validRecords", validLines.size(),
                "invalidRecords", invalidCount
        ));
    }
}
