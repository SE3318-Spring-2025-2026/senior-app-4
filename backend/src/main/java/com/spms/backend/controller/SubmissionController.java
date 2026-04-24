package com.spms.backend.controller;

import com.spms.backend.dto.SubmissionResponse;
import com.spms.backend.dto.response.ErrorResponse;
import com.spms.backend.dto.response.GradeListResponse;
import com.spms.backend.dto.response.SubmissionListResponse;
import com.spms.backend.dto.request.GradeSubmissionRequest;
import com.spms.backend.dto.response.GradeSubmissionResponse;
import com.spms.backend.model.enums.DeliverableType;
import com.spms.backend.service.SubmissionService;
import com.spms.backend.service.SubmissionGradeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final SubmissionGradeService gradeService;

    public SubmissionController(SubmissionService submissionService, SubmissionGradeService gradeService) {
        this.submissionService = submissionService;
        this.gradeService = gradeService;
    }

    @GetMapping
    public ResponseEntity<?> listSubmissions(
            Pageable pageable,
            @RequestAttribute("jwt_userId") Object userId,
            @RequestAttribute("jwt_role") Object role) {
        try {
            Long currentUserId = Long.valueOf(userId.toString());
            String userRole = role.toString();
            SubmissionListResponse response = submissionService.listSubmissions(currentUserId, userRole, pageable);
            return ResponseEntity.ok(response);
        } catch (com.spms.backend.exception.ForbiddenException e) {
            return new ResponseEntity<>(new ErrorResponse("Forbidden", e.getMessage()), HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            return new ResponseEntity<>(new ErrorResponse("Internal Server Error", e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<?> createSubmission(
            @RequestPart("teamId") String teamId,
            @RequestPart("deliverableType") String deliverableType,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "description", required = false) String description,
            @RequestAttribute("jwt_userId") Object userId) {
        
        try {
            Long groupId = Long.valueOf(teamId);
            DeliverableType type = DeliverableType.valueOf(deliverableType);
            Long callerId = Long.valueOf(userId.toString());

            String content = description != null ? description : "";
            String fileName = file.getOriginalFilename();
            SubmissionResponse response = submissionService.submit(groupId, type, content, fileName, callerId);
            return new ResponseEntity<>(response, HttpStatus.CREATED);

        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(new ErrorResponse("Bad Request", "Invalid input: " + e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (com.spms.backend.exception.BadRequestException e) {
            return new ResponseEntity<>(new ErrorResponse("Bad Request", e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (com.spms.backend.exception.ForbiddenException e) {
            return new ResponseEntity<>(new ErrorResponse("Forbidden", e.getMessage()), HttpStatus.FORBIDDEN);
        } catch (com.spms.backend.exception.NotFoundException e) {
            return new ResponseEntity<>(new ErrorResponse("Not Found", e.getMessage()), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(new ErrorResponse("Internal Server Error", "An error occurred: " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{submissionId}/grades")
    public ResponseEntity<?> getSubmissionGrades(
            @PathVariable Long submissionId,
            @RequestAttribute(value = "jwt_role", required = false) Object role) {
        
        try {
            String userRole = (role != null) ? role.toString() : "STUDENT";
            GradeListResponse response = gradeService.getSubmissionGrades(submissionId, userRole);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(new ErrorResponse("Internal Server Error", "An error occurred: " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/{id}/grades")
    public ResponseEntity<?> submitGrade(
            @PathVariable Long id,
            @Valid @RequestBody GradeSubmissionRequest request,
            @RequestAttribute("jwt_userId") Object userId) {
        try {
            Long reviewerId = Long.valueOf(userId.toString());
            GradeSubmissionResponse response = gradeService.submitGrade(id, reviewerId, request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (SecurityException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            return new ResponseEntity<>(new ErrorResponse("Internal Server Error", "An error occurred: " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
