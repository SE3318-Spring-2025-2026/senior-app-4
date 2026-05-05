package com.spms.backend.service;

import com.spms.backend.dto.request.TriggerValidationRequest;
import com.spms.backend.exception.P7ApiException;
import com.spms.backend.model.*;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.SprintIssueTrackingRepository;
import com.spms.backend.repository.SprintRepository;
import com.spms.backend.repository.ValidationJobRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiValidationJobService {
    private static final Set<ValidationJobStatus> ACTIVE = Set.of(ValidationJobStatus.QUEUED, ValidationJobStatus.IN_PROGRESS);

    private final ValidationJobRepository validationJobRepository;
    private final SprintRepository sprintRepository;
    private final GroupRepository groupRepository;
    private final SprintIssueTrackingRepository sprintIssueTrackingRepository;
    private final AiValidationQueueProducer queueProducer;

    public AiValidationJobService(
            ValidationJobRepository validationJobRepository,
            SprintRepository sprintRepository,
            GroupRepository groupRepository,
            SprintIssueTrackingRepository sprintIssueTrackingRepository,
            AiValidationQueueProducer queueProducer
    ) {
        this.validationJobRepository = validationJobRepository;
        this.sprintRepository = sprintRepository;
        this.groupRepository = groupRepository;
        this.sprintIssueTrackingRepository = sprintIssueTrackingRepository;
        this.queueProducer = queueProducer;
    }

    @Transactional
    public ValidationJob trigger(Long sprintId, TriggerValidationRequest request) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new P7ApiException(HttpStatus.NOT_FOUND, "SPRINT_NOT_FOUND", "Sprint not found."));

        Group team = null;
        Long teamId = request != null ? request.teamId() : null;
        if (teamId != null) {
            team = groupRepository.findById(teamId)
                    .orElseThrow(() -> new P7ApiException(HttpStatus.NOT_FOUND, "TEAM_NOT_FOUND", "Team not found."));
        }

        List<SprintIssueTracking> allIssues = sprintIssueTrackingRepository.findBySprint_Id(sprintId);
        List<SprintIssueTracking> filtered = allIssues.stream()
                .filter(it -> teamId == null || Objects.equals(it.getGroup().getId(), teamId))
                .filter(it -> request == null || request.issueKeys() == null || request.issueKeys().isEmpty()
                        || request.issueKeys().contains(it.getIssueKey()))
                .collect(Collectors.toList());

        if (allIssues.isEmpty()) {
            throw new P7ApiException(HttpStatus.BAD_REQUEST, "NO_ISSUES_IN_SPRINT", "No issues found in sprint context.");
        }
        if (filtered.isEmpty()) {
            throw new P7ApiException(HttpStatus.BAD_REQUEST, "NO_MATCHING_ISSUES", "No issues match the given teamId or issueKeys filter.");
        }

        boolean activeExists = teamId == null
                ? validationJobRepository.existsBySprint_IdAndTeamIsNullAndJobStatusIn(sprintId, ACTIVE)
                : validationJobRepository.existsBySprint_IdAndTeam_IdAndJobStatusIn(sprintId, teamId, ACTIVE);
        if (activeExists) {
            throw new P7ApiException(HttpStatus.CONFLICT, "VALIDATION_ALREADY_RUNNING", "Validation already running for this sprint/team.");
        }

        ValidationJob job = new ValidationJob();
        job.setSprint(sprint);
        job.setTeam(team);
        job.setJobStatus(ValidationJobStatus.QUEUED);
        job.setCurrentStep(ValidationJobStep.LOADING_CONTEXT);
        job.setProgressPercentage(0);
        job.setIssuesTotal(filtered.size());
        job.setIssuesCompleted(0);
        job.setIssuesFailed(0);
        job.setStartedAt(Instant.now());

        try {
            ValidationJob saved = validationJobRepository.save(job);
            queueProducer.enqueue(saved, false);
            return saved;
        } catch (DataIntegrityViolationException ex) {
            throw new P7ApiException(HttpStatus.CONFLICT, "VALIDATION_ALREADY_RUNNING", "Validation already running for this sprint/team.");
        }
    }

    @Transactional(readOnly = true)
    public ValidationJob get(Long jobId) {
        return validationJobRepository.findById(jobId)
                .orElseThrow(() -> new P7ApiException(HttpStatus.NOT_FOUND, "JOB_NOT_FOUND", "Validation job not found."));
    }

    @Transactional(readOnly = true)
    public Optional<ValidationJob> getActiveJobForSprint(Long sprintId) {
        sprintRepository.findById(sprintId)
                .orElseThrow(() -> new P7ApiException(HttpStatus.NOT_FOUND, "SPRINT_NOT_FOUND", "Sprint not found."));
        return validationJobRepository.findFirstBySprint_IdAndJobStatusIn(sprintId, ACTIVE);
    }

    @Transactional
    public ValidationJob retry(Long jobId) {
        ValidationJob parent = get(jobId);
        if (parent.getJobStatus() != ValidationJobStatus.FAILED
                && parent.getJobStatus() != ValidationJobStatus.PARTIALLY_COMPLETED) {
            throw new P7ApiException(HttpStatus.BAD_REQUEST, "JOB_NOT_RETRYABLE", "Job is not retryable.");
        }

        if (validationJobRepository.findFirstByParentJob_JobIdAndJobStatusIn(jobId, ACTIVE).isPresent()) {
            throw new P7ApiException(HttpStatus.CONFLICT, "JOB_RETRY_ALREADY_RUNNING", "Retry already running for this job.");
        }

        ValidationJob retry = new ValidationJob();
        retry.setParentJob(parent);
        retry.setSprint(parent.getSprint());
        retry.setTeam(parent.getTeam());
        retry.setJobStatus(ValidationJobStatus.QUEUED);
        retry.setCurrentStep(ValidationJobStep.LOADING_CONTEXT);
        retry.setProgressPercentage(0);
        retry.setIssuesTotal(parent.getIssuesFailed() != null ? parent.getIssuesFailed() : 0);
        retry.setIssuesCompleted(0);
        retry.setIssuesFailed(0);
        retry.setStartedAt(Instant.now());

        try {
            ValidationJob saved = validationJobRepository.save(retry);
            queueProducer.enqueue(saved, true);
            return saved;
        } catch (DataIntegrityViolationException ex) {
            throw new P7ApiException(HttpStatus.CONFLICT, "JOB_RETRY_ALREADY_RUNNING", "Retry already running for this job.");
        }
    }
}
