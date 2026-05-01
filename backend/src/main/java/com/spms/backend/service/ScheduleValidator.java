package com.spms.backend.service;

import com.spms.backend.exception.BadRequestException;
import com.spms.backend.model.GroupCommitteeAssignment;
import com.spms.backend.model.Schedule;
import com.spms.backend.repository.GroupCommitteeAssignmentRepository;
import com.spms.backend.repository.ScheduleRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * P5.5 — Schedule conflict validator.
 *
 * Checks against D11 (Schedule) and existing GroupCommitteeAssignment exam slots
 * within a ±2-hour window. Used by P5.4 (Assign Groups to Committee).
 */
@Service
public class ScheduleValidator {

    /** Half-window for the conflict check (±2 hours). */
    private static final Duration EXAM_WINDOW = Duration.ofHours(2);

    private final GroupCommitteeAssignmentRepository assignmentRepository;
    private final ScheduleRepository scheduleRepository;

    public ScheduleValidator(GroupCommitteeAssignmentRepository assignmentRepository,
                             ScheduleRepository scheduleRepository) {
        this.assignmentRepository = assignmentRepository;
        this.scheduleRepository = scheduleRepository;
    }

    /**
     * Returns true when the committee already has an assignment whose exam date
     * sits within ±2 hours of the supplied {@code examDate}.
     */
    public boolean hasScheduleConflict(Long committeeId, Instant examDate) {
        return hasScheduleConflict(committeeId, examDate, null);
    }

    /**
     * Variant used during status updates where a single existing assignment must
     * be excluded from the conflict scan.
     */
    public boolean hasScheduleConflict(Long committeeId, Instant examDate, Long excludeAssignmentId) {
        if (examDate == null) {
            return false;
        }
        return !findConflictsForCommittee(committeeId, examDate, excludeAssignmentId).isEmpty();
    }

    /**
     * Validates the proposed exam date — fails fast if it is null, in the past,
     * or after the configured grading deadline (D11).
     */
    public void validateExamDate(Instant examDate) {
        if (examDate == null) {
            throw new BadRequestException("Exam date is required.");
        }
        Instant now = Instant.now();
        if (examDate.isBefore(now)) {
            throw new BadRequestException("Exam date must be in the future.");
        }

        Optional<Schedule> scheduleOpt = scheduleRepository.findTopByOrderByIdDesc();
        if (scheduleOpt.isPresent()) {
            Schedule schedule = scheduleOpt.get();
            Instant gradingDeadline = schedule.getGradingDeadline();
            if (gradingDeadline != null && examDate.isAfter(gradingDeadline)) {
                throw new BadRequestException(
                        "Exam date is after the grading deadline (" + gradingDeadline + ").");
            }
        }
    }

    /**
     * Returns every assignment (across all committees) whose exam date sits
     * within ±2 hours of {@code date}. Empty list when {@code date} is null.
     */
    public List<GroupCommitteeAssignment> getConflictingAssignments(Instant date) {
        if (date == null) {
            return List.of();
        }
        return assignmentRepository.findConflictingByWindow(
                date.minus(EXAM_WINDOW),
                date.plus(EXAM_WINDOW));
    }

    /**
     * Conflicts scoped to a single committee, optionally excluding a known
     * assignment id (useful when re-scheduling an existing record).
     */
    public List<GroupCommitteeAssignment> findConflictsForCommittee(Long committeeId,
                                                                     Instant examDate,
                                                                     Long excludeAssignmentId) {
        if (examDate == null) {
            return List.of();
        }
        return assignmentRepository.findConflictingByCommitteeAndWindow(
                committeeId,
                examDate.minus(EXAM_WINDOW),
                examDate.plus(EXAM_WINDOW),
                excludeAssignmentId);
    }
}
