package com.spms.backend.controller;

import com.spms.backend.dto.request.TriggerValidationRequest;
import com.spms.backend.dto.response.ErrorResponse;
import com.spms.backend.dto.response.JobStatusResponse;
import com.spms.backend.dto.response.TriggerValidationResponse;
import com.spms.backend.exception.P7ApiException;
import com.spms.backend.model.ValidationJob;
import com.spms.backend.service.AiValidationJobService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai-validation")
public class AiValidationController {
    private final AiValidationJobService jobService;

    public AiValidationController(AiValidationJobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping("/sprints/{sprintId}/trigger")
    public ResponseEntity<?> trigger(
            @PathVariable Long sprintId,
            @RequestBody(required = false) TriggerValidationRequest request,
            @RequestAttribute("jwt_role") Object role
    ) {
        try {
            requireCoordinator(role);
            ValidationJob job = jobService.trigger(sprintId, request);
            return ResponseEntity.accepted().body(new TriggerValidationResponse(
                    "success",
                    "AI validation job queued. Poll GET /ai-validation/jobs/{jobId} for status.",
                    new TriggerValidationResponse.Data(
                            job.getJobId(),
                            job.getSprint().getId(),
                            job.getTeam() != null ? job.getTeam().getId() : null,
                            job.getIssuesTotal(),
                            job.getJobStatus().name(),
                            job.getStartedAt()
                    )
            ));
        } catch (P7ApiException ex) {
            return ResponseEntity.status(ex.getStatus())
                    .body(new ErrorResponse(ex.getErrorCode(), ex.getMessage()));
        }
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<?> getJobStatus(
            @PathVariable Long jobId,
            @RequestAttribute("jwt_role") Object role,
            @RequestAttribute("jwt_userId") Object userId
    ) {
        try {
            ValidationJob job = jobService.get(jobId);
            enforceReadAccess(job, role, userId);
            return ResponseEntity.ok(new JobStatusResponse(
                    "success",
                    new JobStatusResponse.Data(
                            job.getJobId(),
                            job.getSprint().getId(),
                            job.getJobStatus().name(),
                            job.getCurrentStep() != null ? job.getCurrentStep().name() : null,
                            job.getProgressPercentage(),
                            messageForStep(job),
                            job.getIssuesTotal(),
                            job.getIssuesCompleted(),
                            job.getIssuesFailed(),
                            job.getFailureReason(),
                            job.getStartedAt(),
                            job.getCompletedAt()
                    )
            ));
        } catch (P7ApiException ex) {
            return ResponseEntity.status(ex.getStatus())
                    .body(new ErrorResponse(ex.getErrorCode(), ex.getMessage()));
        }
    }

    @GetMapping("/sprints/{sprintId}/active-job")
    public ResponseEntity<?> getActiveJob(
            @PathVariable Long sprintId,
            @RequestAttribute("jwt_role") Object role,
            @RequestAttribute("jwt_userId") Object userId
    ) {
        try {
            enforceP7Access(role);
            return jobService.getActiveJobForSprint(sprintId)
                    .map(job -> {
                        enforceReadAccess(job, role, userId);
                        return ResponseEntity.ok(new JobStatusResponse(
                                "success",
                                new JobStatusResponse.Data(
                                        job.getJobId(),
                                        job.getSprint().getId(),
                                        job.getJobStatus().name(),
                                        job.getCurrentStep() != null ? job.getCurrentStep().name() : null,
                                        job.getProgressPercentage(),
                                        messageForStep(job),
                                        job.getIssuesTotal(),
                                        job.getIssuesCompleted(),
                                        job.getIssuesFailed(),
                                        job.getFailureReason(),
                                        job.getStartedAt(),
                                        job.getCompletedAt()
                                )
                        ));
                    })
                    .orElse(ResponseEntity.noContent().build());
        } catch (P7ApiException ex) {
            return ResponseEntity.status(ex.getStatus())
                    .body(new ErrorResponse(ex.getErrorCode(), ex.getMessage()));
        }
    }

    @PostMapping("/jobs/{jobId}/retry")
    public ResponseEntity<?> retry(
            @PathVariable Long jobId,
            @RequestAttribute("jwt_role") Object role
    ) {
        try {
            requireCoordinator(role);
            ValidationJob job = jobService.retry(jobId);
            return ResponseEntity.accepted().body(new TriggerValidationResponse(
                    "success",
                    "AI validation job queued. Poll GET /ai-validation/jobs/{jobId} for status.",
                    new TriggerValidationResponse.Data(
                            job.getJobId(),
                            job.getSprint().getId(),
                            job.getTeam() != null ? job.getTeam().getId() : null,
                            job.getIssuesTotal(),
                            job.getJobStatus().name(),
                            job.getStartedAt()
                    )
            ));
        } catch (P7ApiException ex) {
            return ResponseEntity.status(ex.getStatus())
                    .body(new ErrorResponse(ex.getErrorCode(), ex.getMessage()));
        }
    }

    private void enforceP7Access(Object role) {
        if (role != null && "student".equalsIgnoreCase(role.toString())) {
            throw new P7ApiException(org.springframework.http.HttpStatus.FORBIDDEN, "FORBIDDEN", "Caller role is not allowed.");
        }
    }

    private void requireCoordinator(Object role) {
        if (role == null || !"coordinator".equalsIgnoreCase(role.toString())) {
            throw new P7ApiException(org.springframework.http.HttpStatus.FORBIDDEN, "FORBIDDEN", "Coordinator role required.");
        }
    }

    private void enforceReadAccess(ValidationJob job, Object role, Object userIdObj) {
        String userRole = role == null ? "" : role.toString();
        if ("coordinator".equalsIgnoreCase(userRole)) {
            return;
        }
        if ("student".equalsIgnoreCase(userRole)) {
            throw new P7ApiException(org.springframework.http.HttpStatus.FORBIDDEN, "FORBIDDEN", "Caller role is not allowed.");
        }
        if ("advisor".equalsIgnoreCase(userRole)) {
            if (job.getTeam() == null || job.getTeam().getAdvisor() == null || userIdObj == null) {
                throw new P7ApiException(org.springframework.http.HttpStatus.FORBIDDEN, "FORBIDDEN_TEAM_ACCESS", "Advisor is not authorized for this team.");
            }
            Long userId = Long.valueOf(userIdObj.toString());
            if (!userId.equals(job.getTeam().getAdvisor().getUserId())) {
                throw new P7ApiException(org.springframework.http.HttpStatus.FORBIDDEN, "FORBIDDEN_TEAM_ACCESS", "Advisor is not authorized for this team.");
            }
            return;
        }
        throw new P7ApiException(org.springframework.http.HttpStatus.FORBIDDEN, "FORBIDDEN", "Caller role is not allowed.");
    }

    private String messageForStep(ValidationJob job) {
        if (job.getJobStatus() == null) {
            return null;
        }
        return switch (job.getJobStatus()) {
            case QUEUED -> "Validation job queued.";
            case IN_PROGRESS -> "Validation in progress.";
            case COMPLETED -> "Validation completed.";
            case PARTIALLY_COMPLETED -> "Validation partially completed.";
            case FAILED -> "Validation failed.";
        };
    }
}
