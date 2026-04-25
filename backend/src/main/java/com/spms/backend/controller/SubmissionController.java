package com.spms.backend.controller;

import com.spms.backend.dto.SubmissionResponse;
import com.spms.backend.dto.response.ErrorResponse;
import com.spms.backend.dto.response.GradeListResponse;
import com.spms.backend.dto.response.ReviewDto;
import com.spms.backend.dto.response.RevisionCreateResponseDto;
import com.spms.backend.dto.response.RevisionHistoryResponseDto;
import com.spms.backend.dto.response.SubmissionListResponse;
import com.spms.backend.dto.request.GradeSubmissionRequest;
import com.spms.backend.dto.response.GradeSubmissionResponse;
import com.spms.backend.model.enums.DeliverableType;
import com.spms.backend.service.ReviewService;
import com.spms.backend.service.SubmissionService;
import com.spms.backend.service.SubmissionGradeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final SubmissionGradeService gradeService;
    private final ReviewService reviewService;

    public SubmissionController(SubmissionService submissionService,
                                SubmissionGradeService gradeService,
                                ReviewService reviewService) {
        this.submissionService = submissionService;
        this.gradeService = gradeService;
        this.reviewService = reviewService;
    }

    // ─────────────────────────────────────────────
    //  3.7  GET /submissions  — List submissions
    // ─────────────────────────────────────────────

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

    // ─────────────────────────────────────────────
    //  3.1  POST /submissions  — Submit deliverable
    // ─────────────────────────────────────────────

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

    // ─────────────────────────────────────────────
    //  3.2  POST /submissions/{submissionId}/revisions
    //       [P3-REV-1] Submit a revised version
    // ─────────────────────────────────────────────

    @PostMapping(value = "/{submissionId}/revisions", consumes = "multipart/form-data")
    public ResponseEntity<?> createRevision(
            @PathVariable Long submissionId,
            @RequestAttribute("jwt_userId") Object userId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description) {

        try {
            Long callerId = Long.valueOf(userId.toString());
            RevisionCreateResponseDto response = submissionService.createRevision(submissionId, callerId, file, description);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (com.spms.backend.exception.NotFoundException e) {
            return new ResponseEntity<>(new ErrorResponse("Not Found", e.getMessage()), HttpStatus.NOT_FOUND);
        } catch (com.spms.backend.exception.BadRequestException e) {
            return new ResponseEntity<>(new ErrorResponse("Bad Request", e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (com.spms.backend.exception.ForbiddenException e) {
            return new ResponseEntity<>(new ErrorResponse("Forbidden", e.getMessage()), HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            return new ResponseEntity<>(new ErrorResponse("Internal Server Error", "An error occurred: " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ─────────────────────────────────────────────
    //  3.2  GET /submissions/{submissionId}/revisions
    //       [P3-REV-2] Get full revision chain
    // ─────────────────────────────────────────────

    @GetMapping("/{submissionId}/revisions")
    public ResponseEntity<?> getRevisionHistory(
            @PathVariable Long submissionId,
            @RequestAttribute("jwt_userId") Object userId) {

        try {
            RevisionHistoryResponseDto response = submissionService.getRevisionHistory(submissionId);
            return ResponseEntity.ok(response);

        } catch (com.spms.backend.exception.NotFoundException e) {
            return new ResponseEntity<>(new ErrorResponse("Not Found", e.getMessage()), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(new ErrorResponse("Internal Server Error", "An error occurred: " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ─────────────────────────────────────────────
    //  3.5  GET /submissions/{submissionId}/grades
    // ─────────────────────────────────────────────

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

    // ─────────────────────────────────────────────
    //  3.5  POST /submissions/{id}/grades
    // ─────────────────────────────────────────────

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

    // ─────────────────────────────────────────────
    //  3.4  GET /submissions/{submissionId}/reviews
    // ─────────────────────────────────────────────

    @GetMapping("/{submissionId}/reviews")
    public ResponseEntity<List<ReviewDto>> getReviews(
            @PathVariable Long submissionId,
            @RequestAttribute("jwt_userId") Object userId) {

        Long currentUserId = Long.valueOf(userId.toString());
        List<ReviewDto> reviews = reviewService.getReviewsForSubmission(submissionId, currentUserId);
        return ResponseEntity.ok(reviews);
    }
}
