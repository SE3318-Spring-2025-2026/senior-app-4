package com.spms.backend.service;

import com.spms.backend.dto.response.ScheduleValidationResponse;
import com.spms.backend.dto.response.ValidationRulesResponse;
import com.spms.backend.model.Committee;
import com.spms.backend.repository.CommitteeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.spms.backend.model.GroupCommitteeAssignment;
import com.spms.backend.repository.GroupCommitteeAssignmentRepository;

@Service
public class ValidationService {

    private final CommitteeRepository committeeRepository;
    private final ScheduleValidator scheduleValidator;
    private final GroupCommitteeAssignmentRepository assignmentRepository;

    public ValidationService(CommitteeRepository committeeRepository,
                             ScheduleValidator scheduleValidator,
                             GroupCommitteeAssignmentRepository assignmentRepository) {
        this.committeeRepository = committeeRepository;
        this.scheduleValidator = scheduleValidator;
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional(readOnly = true)
    public ValidationResult validateAllAssignments(Long committeeId) {
        return validateAssignmentRules(committeeId);
    }

    @Transactional(readOnly = true)
    public ValidationResult validateAssignmentRules(Long committeeId) {
        Optional<Committee> committeeOpt = committeeRepository.findById(committeeId);
        if (committeeOpt.isEmpty()) {
            return ValidationResult.failure("Committee not found.");
        }

        Committee committee = committeeOpt.get();
        int advisorCount = committee.getAdvisors() != null ? committee.getAdvisors().size() : 0;
        int juryCount = committee.getJuryMembers() != null ? committee.getJuryMembers().size() : 0;

        if (advisorCount < 3) {
            return ValidationResult.failure("Committee must have at least 3 advisors.");
        }
        if (advisorCount > 5) {
            return ValidationResult.failure("Committee cannot have more than 5 advisors.");
        }
        if (juryCount < 1) {
            return ValidationResult.failure("Committee must have at least 1 jury member.");
        }
        if (juryCount > 2) {
            return ValidationResult.failure("Committee cannot have more than 2 jury members.");
        }

        return ValidationResult.success("All assignment rules passed.");
    }

    public ScheduleValidationResponse validateSchedule(Long committeeId, Instant examDate, Long groupId) {
        if (examDate == null) {
            return new ScheduleValidationResponse(false, "Exam date is required.");
        }
        try {
            scheduleValidator.validateExamDate(examDate);
        } catch (Exception e) {
            return new ScheduleValidationResponse(false, e.getMessage());
        }

        Long excludeAssignmentId = null;
        if (groupId != null) {
            Optional<GroupCommitteeAssignment> existing = assignmentRepository.findByCommittee_CommitteeIdAndGroupId(committeeId, groupId);
            if (existing.isPresent()) {
                excludeAssignmentId = existing.get().getAssignmentId();
            }
        }

        boolean hasConflict = scheduleValidator.hasScheduleConflict(committeeId, examDate, excludeAssignmentId);
        if (hasConflict) {
            return new ScheduleValidationResponse(false, "Schedule conflict detected within ±2 hours window.");
        }
        return new ScheduleValidationResponse(true, "Schedule is clear.");
    }

    public ValidationRulesResponse getValidationRules(Long committeeId) {
        return new ValidationRulesResponse(
                3, 5, 1, 2, "±2 hours",
                List.of(
                        "Advisors must have faculty status",
                        "No duplicate advisor assignments",
                        "Min 3 advisors",
                        "Max 5 advisors",
                        "1-2 jury members",
                        "No schedule conflict within ±2 hours window"
                )
        );
    }
}
