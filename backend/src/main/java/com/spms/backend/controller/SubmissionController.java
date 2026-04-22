package com.spms.backend.controller;

import com.spms.backend.dto.request.GradeUpdateRequest;
import com.spms.backend.service.GradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Submission Management")
@RestController
@RequestMapping("/api/v1/submissions")
public class SubmissionController {

    private final GradeService gradeService;

    public SubmissionController(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    @Operation(summary = "Update an existing grade for a submission")
    @PutMapping("/{submissionId}/grades/{gradeId}")
    public ResponseEntity<?> updateGrade(
            @PathVariable Long submissionId,
            @PathVariable Long gradeId,
            @Valid @RequestBody GradeUpdateRequest request,
            @RequestAttribute("jwt_userId") Object userId) {

        Long professorId = Long.valueOf(userId.toString());
        gradeService.updateGrade(submissionId, gradeId, professorId, request);

        return ResponseEntity.ok().body(Map.of(
                "success", true,
                "message", "Grade successfully updated"
        ));
    }
}
