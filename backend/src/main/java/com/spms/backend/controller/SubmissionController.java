package com.spms.backend.controller;

import com.spms.backend.dto.SubmissionRequest;
import com.spms.backend.dto.SubmissionResponse;
import com.spms.backend.service.SubmissionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.spms.backend.dto.request.GradeSubmissionRequest;
import com.spms.backend.dto.response.GradeSubmissionResponse;
import com.spms.backend.service.SubmissionGradeService;

@RestController
@RequestMapping("/api/v1/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final SubmissionGradeService gradeService;

    public SubmissionController(SubmissionService submissionService, SubmissionGradeService gradeService) {
        this.submissionService = submissionService;
        this.gradeService = gradeService;
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

    @PostMapping("/{id}/grades")
    public ResponseEntity<?> submitGrade(
            @PathVariable Long id,
            @RequestBody GradeSubmissionRequest request,
            @RequestAttribute("jwt_userId") Object userId) {
        try {
            Long reviewerId = Long.valueOf(userId.toString());
            GradeSubmissionResponse response = gradeService.submitGrade(id, reviewerId, request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (SecurityException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("An internal error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
