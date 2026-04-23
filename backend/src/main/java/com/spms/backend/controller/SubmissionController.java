package com.spms.backend.controller;

import com.spms.backend.dto.SubmissionRequest;
import com.spms.backend.dto.SubmissionResponse;
import com.spms.backend.service.SubmissionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.spms.backend.service.SubmissionGradeService;
import com.spms.backend.dto.response.GradeListResponse;

@RestController
@RequestMapping("/api/v1/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final SubmissionGradeService submissionGradeService;

    public SubmissionController(SubmissionService submissionService, SubmissionGradeService submissionGradeService) {
        this.submissionService = submissionService;
        this.submissionGradeService = submissionGradeService;
    }

    @PostMapping
    public ResponseEntity<?> createSubmission(@RequestBody SubmissionRequest request) {
        try {
            SubmissionResponse response = submissionService.submit(request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>("An internal error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{submissionId}/grades")
    public ResponseEntity<?> getSubmissionGrades(
            @PathVariable Long submissionId,
            @RequestAttribute(value = "jwt_role", required = false) Object role) {
        
        try {
            String userRole = (role != null) ? role.toString() : "STUDENT";
            GradeListResponse response = submissionGradeService.getSubmissionGrades(submissionId, userRole);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("An internal error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
