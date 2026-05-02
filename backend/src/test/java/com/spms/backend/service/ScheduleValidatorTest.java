package com.spms.backend.service;

import com.spms.backend.exception.BadRequestException;
import com.spms.backend.model.GroupCommitteeAssignment;
import com.spms.backend.model.Schedule;
import com.spms.backend.repository.GroupCommitteeAssignmentRepository;
import com.spms.backend.repository.ScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleValidatorTest {

    @Mock
    private GroupCommitteeAssignmentRepository assignmentRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @InjectMocks
    private ScheduleValidator validator;

    private Instant base;

    @BeforeEach
    void setUp() {
        base = Instant.now().plus(7, ChronoUnit.DAYS);
    }

    @Test
    @DisplayName("hasScheduleConflict returns false when nothing overlaps")
    void noConflict() {
        when(assignmentRepository.findConflictingByCommitteeAndWindow(
                anyLong(), any(), any(), any())).thenReturn(List.of());

        assertFalse(validator.hasScheduleConflict(1L, base));
    }

    @Test
    @DisplayName("hasScheduleConflict returns true when an assignment is within ±2h")
    void detectsConflict() {
        GroupCommitteeAssignment existing = new GroupCommitteeAssignment();
        existing.setAssignmentId(99L);
        existing.setExamDate(base.plus(1, ChronoUnit.HOURS));

        when(assignmentRepository.findConflictingByCommitteeAndWindow(
                anyLong(), any(), any(), any())).thenReturn(List.of(existing));

        assertTrue(validator.hasScheduleConflict(1L, base));
    }

    @Test
    @DisplayName("validateExamDate rejects null")
    void validateExamDate_null() {
        assertThrows(BadRequestException.class, () -> validator.validateExamDate(null));
    }

    @Test
    @DisplayName("validateExamDate rejects past dates")
    void validateExamDate_past() {
        Instant past = Instant.now().minus(1, ChronoUnit.DAYS);
        assertThrows(BadRequestException.class, () -> validator.validateExamDate(past));
    }

    @Test
    @DisplayName("validateExamDate rejects dates after grading deadline")
    void validateExamDate_afterGradingDeadline() {
        Schedule s = new Schedule();
        s.setGradingDeadline(Instant.now().plus(1, ChronoUnit.DAYS));
        when(scheduleRepository.findTopByOrderByIdDesc()).thenReturn(Optional.of(s));

        Instant tooLate = Instant.now().plus(10, ChronoUnit.DAYS);
        assertThrows(BadRequestException.class, () -> validator.validateExamDate(tooLate));
    }

    @Test
    @DisplayName("validateExamDate accepts a sane future date")
    void validateExamDate_accepts() {
        when(scheduleRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> validator.validateExamDate(base));
    }

    @Test
    @DisplayName("getConflictingAssignments returns empty list for null date")
    void getConflicts_null() {
        assertTrue(validator.getConflictingAssignments(null).isEmpty());
    }
}
