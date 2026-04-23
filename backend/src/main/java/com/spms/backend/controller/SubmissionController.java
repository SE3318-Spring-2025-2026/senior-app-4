package com.spms.backend.controller;

import com.spms.backend.model.Submission;
import com.spms.backend.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Submission Management")
@RestController
@RequestMapping("/api/v1/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @Operation(summary = "Submit a revision for a submission")
    @PostMapping("/{submissionId}/revisions")
    public ResponseEntity<?> createRevision(@PathVariable Long submissionId) {
        Submission revision = submissionService.createRevision(submissionId);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Revision successfully created",
            "data", revision
        ));
    }

    @Operation(summary = "Get revisions of a submission")
    @GetMapping("/{submissionId}/revisions")
    public ResponseEntity<?> getRevisions(@PathVariable Long submissionId) {
        List<Submission> revisions = submissionService.getRevisions(submissionId);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", revisions
        ));
    }
}
