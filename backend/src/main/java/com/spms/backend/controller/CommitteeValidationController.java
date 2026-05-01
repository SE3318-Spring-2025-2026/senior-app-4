package com.spms.backend.controller;

import com.spms.backend.dto.request.ScheduleValidationRequest;
import com.spms.backend.dto.response.ScheduleValidationResponse;
import com.spms.backend.dto.response.ValidationRulesResponse;
import com.spms.backend.service.ValidationResult;
import com.spms.backend.service.ValidationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/committees/{committeeId}")
public class CommitteeValidationController {

    private final ValidationService validationService;

    public CommitteeValidationController(ValidationService validationService) {
        this.validationService = validationService;
    }

    @PostMapping("/validate/all")
    public ResponseEntity<ValidationResult> validateAll(@PathVariable Long committeeId) {
        ValidationResult result = validationService.validateAllAssignments(committeeId);
        if (!result.valid()) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/validate/schedule")
    public ResponseEntity<ScheduleValidationResponse> validateSchedule(
            @PathVariable Long committeeId,
            @RequestBody ScheduleValidationRequest request) {
        java.time.Instant examDateInstant = request.examDate() != null
                ? request.examDate().atZone(java.time.ZoneId.systemDefault()).toInstant()
                : null;
        ScheduleValidationResponse result = validationService.validateSchedule(
                committeeId, examDateInstant, request.groupId());
        if (!result.valid()) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/validation-rules")
    public ResponseEntity<ValidationRulesResponse> getValidationRules(@PathVariable Long committeeId) {
        return ResponseEntity.ok(validationService.getValidationRules(committeeId));
    }
}
