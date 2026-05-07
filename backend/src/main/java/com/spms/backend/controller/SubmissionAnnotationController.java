package com.spms.backend.controller;

import com.spms.backend.dto.AnnotationRequest;
import com.spms.backend.dto.AnnotationResponse;
import com.spms.backend.exception.ForbiddenException;
import com.spms.backend.exception.NotFoundException;
import com.spms.backend.model.SubmissionAnnotation;
import com.spms.backend.repository.SubmissionAnnotationRepository;
import com.spms.backend.repository.SubmissionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/submissions/{submissionId}/annotations")
public class SubmissionAnnotationController {

    private final SubmissionAnnotationRepository annotationRepository;
    private final SubmissionRepository submissionRepository;

    public SubmissionAnnotationController(SubmissionAnnotationRepository annotationRepository,
                                          SubmissionRepository submissionRepository) {
        this.annotationRepository = annotationRepository;
        this.submissionRepository = submissionRepository;
    }

    @GetMapping
    public ResponseEntity<List<AnnotationResponse>> getAnnotations(
            @PathVariable Long submissionId,
            @RequestAttribute("jwt_userId") Object userId,
            @RequestAttribute("jwt_role") Object role) {

        ensureSubmissionExists(submissionId);

        List<SubmissionAnnotation> annotations = annotationRepository
                .findBySubmissionIdOrderByStartOffsetAsc(submissionId);

        return ResponseEntity.ok(annotations.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @PostMapping
    public ResponseEntity<AnnotationResponse> createAnnotation(
            @PathVariable Long submissionId,
            @RequestBody AnnotationRequest request,
            @RequestAttribute("jwt_userId") Object userId,
            @RequestAttribute("jwt_role") Object role) {

        ensureSubmissionExists(submissionId);
        ensureAdvisorOrCoordinator(role.toString());

        if (request.getSelectedText() == null || request.getSelectedText().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (request.getStartOffset() == null || request.getEndOffset() == null
                || request.getStartOffset() < 0 || request.getEndOffset() <= request.getStartOffset()) {
            return ResponseEntity.badRequest().build();
        }

        SubmissionAnnotation annotation = new SubmissionAnnotation();
        annotation.setSubmissionId(submissionId);
        annotation.setAdvisorId(Long.valueOf(userId.toString()));
        annotation.setCriterionId(request.getCriterionId());
        annotation.setSelectedText(request.getSelectedText());
        annotation.setStartOffset(request.getStartOffset());
        annotation.setEndOffset(request.getEndOffset());
        annotation.setComment(request.getComment());
        annotation.setGrade(request.getGrade());

        SubmissionAnnotation saved = annotationRepository.save(annotation);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @PutMapping("/{annotationId}")
    public ResponseEntity<AnnotationResponse> updateAnnotation(
            @PathVariable Long submissionId,
            @PathVariable Long annotationId,
            @RequestBody AnnotationRequest request,
            @RequestAttribute("jwt_userId") Object userId,
            @RequestAttribute("jwt_role") Object role) {

        ensureSubmissionExists(submissionId);
        ensureAdvisorOrCoordinator(role.toString());

        SubmissionAnnotation annotation = annotationRepository.findById(annotationId)
                .orElseThrow(() -> new NotFoundException("Annotation not found"));

        if (!annotation.getSubmissionId().equals(submissionId)) {
            throw new ForbiddenException("Annotation does not belong to this submission");
        }
        if (!annotation.getAdvisorId().equals(Long.valueOf(userId.toString()))) {
            throw new ForbiddenException("You can only edit your own annotations");
        }

        annotation.setCriterionId(request.getCriterionId());
        annotation.setComment(request.getComment());
        annotation.setGrade(request.getGrade());

        SubmissionAnnotation updated = annotationRepository.save(annotation);
        return ResponseEntity.ok(toResponse(updated));
    }

    @DeleteMapping("/{annotationId}")
    public ResponseEntity<Void> deleteAnnotation(
            @PathVariable Long submissionId,
            @PathVariable Long annotationId,
            @RequestAttribute("jwt_userId") Object userId,
            @RequestAttribute("jwt_role") Object role) {

        ensureSubmissionExists(submissionId);
        ensureAdvisorOrCoordinator(role.toString());

        SubmissionAnnotation annotation = annotationRepository.findById(annotationId)
                .orElseThrow(() -> new NotFoundException("Annotation not found"));

        if (!annotation.getSubmissionId().equals(submissionId)) {
            throw new ForbiddenException("Annotation does not belong to this submission");
        }
        if (!annotation.getAdvisorId().equals(Long.valueOf(userId.toString()))) {
            throw new ForbiddenException("You can only delete your own annotations");
        }

        annotationRepository.delete(annotation);
        return ResponseEntity.noContent().build();
    }

    private void ensureSubmissionExists(Long submissionId) {
        if (!submissionRepository.existsById(submissionId)) {
            throw new NotFoundException("Submission not found");
        }
    }

    private void ensureAdvisorOrCoordinator(String role) {
        String r = role.toLowerCase();
        if (!r.equals("professor") && !r.equals("advisor") && !r.equals("coordinator") && !r.equals("admin")) {
            throw new ForbiddenException("Only advisors and coordinators can manage annotations");
        }
    }

    private AnnotationResponse toResponse(SubmissionAnnotation a) {
        AnnotationResponse r = new AnnotationResponse();
        r.setId(a.getId());
        r.setSubmissionId(a.getSubmissionId());
        r.setAdvisorId(a.getAdvisorId());
        r.setCriterionId(a.getCriterionId());
        r.setSelectedText(a.getSelectedText());
        r.setStartOffset(a.getStartOffset());
        r.setEndOffset(a.getEndOffset());
        r.setComment(a.getComment());
        r.setGrade(a.getGrade());
        r.setCreatedAt(a.getCreatedAt());
        r.setUpdatedAt(a.getUpdatedAt());
        return r;
    }
}
