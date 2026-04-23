package com.spms.backend.controller;

import com.spms.backend.dto.SubmissionResponse;
import com.spms.backend.dto.response.ErrorResponse;
import com.spms.backend.model.enums.DeliverableType;
import com.spms.backend.service.SubmissionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

            // For simplicity, we use description as 'content' in the entity
            String content = description != null ? description : "";
            String fileName = file.getOriginalFilename();

            SubmissionResponse response = submissionService.submit(groupId, type, content, fileName, callerId);
            return new ResponseEntity<>(response, HttpStatus.CREATED);

        } catch (IllegalArgumentException e) {
            // FIX-9: Use ErrorResponse DTO
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
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("An internal error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
