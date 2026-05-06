package com.spms.backend.service;

import com.spms.backend.model.*;
import com.spms.backend.repository.IssueValidationResultRepository;
import com.spms.backend.repository.ValidationJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Handles all transactional writes for the P7 validation pipeline.
 * Extracted from ValidationPipelineOrchestratorImpl to avoid Spring AOP
 * self-invocation bypassing @Transactional(REQUIRES_NEW).
 */
@Service
public class ValidationJobWriteService {

    private final ValidationJobRepository validationJobRepository;
    private final IssueValidationResultRepository issueValidationResultRepository;

    public ValidationJobWriteService(ValidationJobRepository validationJobRepository,
                                     IssueValidationResultRepository issueValidationResultRepository) {
        this.validationJobRepository = validationJobRepository;
        this.issueValidationResultRepository = issueValidationResultRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markJobInProgress(Long jobId) {
        validationJobRepository.findById(jobId).ifPresent(job -> {
            job.setJobStatus(ValidationJobStatus.IN_PROGRESS);
            job.setCurrentStep(ValidationJobStep.LOADING_CONTEXT);
            validationJobRepository.save(job);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStep(Long jobId, ValidationJobStep step) {
        validationJobRepository.findById(jobId).ifPresent(job -> {
            job.setCurrentStep(step);
            validationJobRepository.save(job);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateProgress(Long jobId, int total, int completed, int failed) {
        validationJobRepository.findById(jobId).ifPresent(job -> {
            int progress = total > 0 ? (completed + failed) * 100 / total : 100;
            job.setProgressPercentage(progress);
            job.setIssuesCompleted(completed);
            job.setIssuesFailed(failed);
            job.setCurrentStep(ValidationJobStep.STORING_RESULTS);
            validationJobRepository.save(job);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveResult(IssueValidationResult result) {
        issueValidationResultRepository.save(result);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailedIssue(Long jobId, SprintIssueTracking sit, String errorCode) {
        IssueValidationResult result = new IssueValidationResult();
        ValidationJob jobRef = new ValidationJob();
        jobRef.setJobId(jobId);
        result.setJob(jobRef);
        result.setIssueKey(sit.getIssueKey());
        result.setAssignee(sit.getAssigneeGithubUsername());
        result.setValidationStatus("FAILED");
        // PII-safe: store only the error code, never raw exception messages
        // that may contain diff content or GitHub/OpenAI response bodies.
        result.setReviewAiFeedback(errorCode);
        result.setEvaluatedAt(Instant.now());
        issueValidationResultRepository.save(result);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finalizeJob(Long jobId, int completed, int failed) {
        validationJobRepository.findById(jobId).ifPresent(job -> {
            if (failed == 0) {
                job.setJobStatus(ValidationJobStatus.COMPLETED);
            } else if (completed > 0) {
                job.setJobStatus(ValidationJobStatus.PARTIALLY_COMPLETED);
                job.setFailureReason(failed + " issue(s) could not be validated.");
            } else {
                job.setJobStatus(ValidationJobStatus.FAILED);
                job.setFailureReason("All issues failed validation.");
            }
            job.setProgressPercentage(100);
            job.setCompletedAt(Instant.now());
            validationJobRepository.save(job);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markJobFailed(Long jobId, String reason) {
        validationJobRepository.findById(jobId).ifPresent(job -> {
            job.setJobStatus(ValidationJobStatus.FAILED);
            String safeReason = reason != null && reason.length() > 500 ? reason.substring(0, 500) : reason;
            job.setFailureReason(safeReason);
            job.setCompletedAt(Instant.now());
            validationJobRepository.save(job);
        });
    }
}
