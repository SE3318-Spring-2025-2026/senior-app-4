package com.spms.backend.service;

import com.spms.backend.dto.request.GradeSubmissionRequest;
import com.spms.backend.dto.response.GradeSubmissionResponse;
import com.spms.backend.model.*;
import com.spms.backend.model.enums.SubmissionStatus;
import com.spms.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SubmissionGradeServiceTest {

    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private SubmissionGradeRepository gradeRepository;
    @Mock
    private GroupCommitteeAssignmentRepository assignmentRepository;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ScheduleRepository scheduleRepository;

    @InjectMocks
    private SubmissionGradeService submissionGradeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void submitGrade_DuplicateGrade_ThrowsException() {
        // Arrange
        Long submissionId = 1L;
        Long professorId = 5L;
        GradeSubmissionRequest request = new GradeSubmissionRequest();
        request.setGrade(85.0);

        Submission submission = new Submission();
        submission.setId(submissionId);

        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
        when(gradeRepository.existsBySubmissionIdAndProfessorId(submissionId, professorId)).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            submissionGradeService.submitGrade(submissionId, professorId, request)
        );
    }

    @Test
    void submitGrade_AlreadyGraded_ThrowsConflictException() {
        // Arrange
        Long submissionId = 1L;
        Long professorId = 5L;
        GradeSubmissionRequest request = new GradeSubmissionRequest();
        request.setGrade(85.0);

        Submission submission = new Submission();
        submission.setId(submissionId);
        submission.setStatus(SubmissionStatus.GRADED);

        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));

        // Act & Assert
        com.spms.backend.exception.ConflictException ex = assertThrows(com.spms.backend.exception.ConflictException.class, () -> 
            submissionGradeService.submitGrade(submissionId, professorId, request)
        );
        assertTrue(ex.getMessage().contains("fully graded"));
    }

    @Test
    void submitGrade_PastDeadline_ThrowsForbiddenException() {
        // Arrange
        Long submissionId = 1L;
        Long professorId = 5L;
        GradeSubmissionRequest request = new GradeSubmissionRequest();
        request.setGrade(85.0);

        Submission submission = new Submission();
        submission.setId(submissionId);
        submission.setStatus(SubmissionStatus.APPROVED);

        Schedule schedule = new Schedule();
        schedule.setGradingDeadline(java.time.Instant.now().minusSeconds(3600)); // 1 hour ago

        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
        when(scheduleRepository.findTopByOrderByIdDesc()).thenReturn(Optional.of(schedule));

        // Act & Assert
        com.spms.backend.exception.ForbiddenException ex = assertThrows(com.spms.backend.exception.ForbiddenException.class, () -> 
            submissionGradeService.submitGrade(submissionId, professorId, request)
        );
        assertTrue(ex.getMessage().contains("Grading deadline has passed"));
    }


    @Test
    void submitGrade_CompletesGradingAndNotifies_WhenAllMembersGraded() {
        // Arrange
        Long submissionId = 1L;
        Long professorId = 5L;
        Long groupId = 10L;
        GradeSubmissionRequest request = new GradeSubmissionRequest();
        request.setGrade(80.0);

        Submission submission = new Submission();
        submission.setId(submissionId);
        submission.setGroupId(groupId);

        Committee committee = new Committee();
        committee.setCreatedBy(100L); // Coordinator ID
        
        CommitteeAdvisor advisor = new CommitteeAdvisor();
        User advisorUser = new User();
        advisorUser.setUserId(professorId);
        advisor.setAdvisor(advisorUser);
        committee.setAdvisors(Collections.singletonList(advisor));
        committee.setJuryMembers(Collections.emptyList());

        GroupCommitteeAssignment assignment = new GroupCommitteeAssignment();
        assignment.setCommittee(committee);

        Group group = new Group();
        User leader = new User();
        leader.setUserId(200L); // Group Leader ID
        group.setLeader(leader);

        SubmissionGrade grade = new SubmissionGrade();
        grade.setSubmissionId(submissionId);
        grade.setProfessorId(professorId);
        grade.setScore(80.0);
        grade.setId(1L);
        grade.setScore(80.0);

        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
        when(gradeRepository.existsBySubmissionIdAndProfessorId(submissionId, professorId)).thenReturn(false);
        when(assignmentRepository.findTopByGroupIdAndStatusOrderByAssignedAtDesc(groupId, "ASSIGNED"))
                .thenReturn(Optional.of(assignment));
        when(gradeRepository.save(any(SubmissionGrade.class))).thenReturn(grade);
        when(gradeRepository.findBySubmissionId(submissionId)).thenReturn(Collections.singletonList(grade));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));

        // Act
        GradeSubmissionResponse response = submissionGradeService.submitGrade(submissionId, professorId, request);

        // Assert
        assertTrue(response.getData().getIsGradingComplete());
        assertEquals(80.0, submission.getFinalGrade());
        assertEquals(SubmissionStatus.GRADED, submission.getStatus());
        
        // Verify notifications
        verify(notificationService).createSystemAlert(eq(100L), anyString(), eq("GRADING_COMPLETE"), anyString());
        verify(notificationService).createSystemAlert(eq(200L), anyString(), eq("GRADING_COMPLETE"), anyString());
    }

    @Test
    void submitGrade_DoesNotCompleteGrading_WhenMoreMembersPending() {
        // Arrange
        Long submissionId = 1L;
        Long professorId = 5L;
        Long groupId = 10L;
        GradeSubmissionRequest request = new GradeSubmissionRequest();
        request.setGrade(80.0);

        Submission submission = new Submission();
        submission.setId(submissionId);
        submission.setGroupId(groupId);

        Committee committee = new Committee();
        
        CommitteeAdvisor advisor1 = new CommitteeAdvisor();
        User user1 = new User(); user1.setUserId(5L);
        advisor1.setAdvisor(user1);

        CommitteeAdvisor advisor2 = new CommitteeAdvisor();
        User user2 = new User(); user2.setUserId(6L);
        advisor2.setAdvisor(user2);

        committee.setAdvisors(List.of(advisor1, advisor2));
        committee.setJuryMembers(Collections.emptyList());

        GroupCommitteeAssignment assignment = new GroupCommitteeAssignment();
        assignment.setCommittee(committee);

        SubmissionGrade grade = new SubmissionGrade();
        grade.setSubmissionId(submissionId);
        grade.setProfessorId(professorId);
        grade.setScore(80.0);
        grade.setId(1L);
        grade.setScore(80.0);

        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
        when(gradeRepository.existsBySubmissionIdAndProfessorId(submissionId, professorId)).thenReturn(false);
        when(assignmentRepository.findTopByGroupIdAndStatusOrderByAssignedAtDesc(groupId, "ASSIGNED"))
                .thenReturn(Optional.of(assignment));
        when(gradeRepository.save(any(SubmissionGrade.class))).thenReturn(grade);
        when(gradeRepository.findBySubmissionId(submissionId)).thenReturn(Collections.singletonList(grade));

        // Act
        GradeSubmissionResponse response = submissionGradeService.submitGrade(submissionId, professorId, request);

        // Assert
        assertFalse(response.getData().getIsGradingComplete());
        assertNull(submission.getFinalGrade());
        verify(notificationService, never()).createSystemAlert(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    void updateGrade_Success_ReturnsSuccessResponse() {
        // Arrange
        Long submissionId = 1L;
        Long gradeId = 100L;
        Long professorId = 5L;
        GradeSubmissionRequest request = new GradeSubmissionRequest();
        request.setGrade(95.0);
        request.setFeedback("Updated feedback");

        SubmissionGrade existingGrade = new SubmissionGrade();
        existingGrade.setId(gradeId);
        existingGrade.setSubmissionId(submissionId);
        existingGrade.setProfessorId(professorId);
        existingGrade.setScore(80.0);

        Submission submission = new Submission();
        submission.setId(submissionId);
        submission.setGroupId(10L);

        Committee committee = new Committee();
        CommitteeAdvisor advisor = new CommitteeAdvisor();
        User advisorUser = new User();
        advisorUser.setUserId(professorId);
        advisor.setAdvisor(advisorUser);
        committee.setAdvisors(Collections.singletonList(advisor));
        committee.setJuryMembers(Collections.emptyList());

        GroupCommitteeAssignment assignment = new GroupCommitteeAssignment();
        assignment.setCommittee(committee);

        when(gradeRepository.findById(gradeId)).thenReturn(Optional.of(existingGrade));
        when(scheduleRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        when(gradeRepository.save(any(SubmissionGrade.class))).thenReturn(existingGrade);
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
        when(assignmentRepository.findTopByGroupIdAndStatusOrderByAssignedAtDesc(10L, "ASSIGNED"))
                .thenReturn(Optional.of(assignment));
        when(gradeRepository.findBySubmissionId(submissionId)).thenReturn(Collections.singletonList(existingGrade));

        // Act
        com.spms.backend.dto.response.SuccessResponse response =
                submissionGradeService.updateGrade(submissionId, gradeId, professorId, request);

        // Assert — spec says SuccessResponse {status, message}
        assertEquals("success", response.status());
        assertEquals("Grade updated successfully.", response.message());
        // Verify entity fields were updated
        assertEquals(95.0, existingGrade.getScore());
        assertEquals("Updated feedback", existingGrade.getFeedback());
        verify(gradeRepository).save(existingGrade);
    }

    @Test
    void updateGrade_NotOwner_ThrowsForbidden() {
        // Arrange
        Long submissionId = 1L;
        Long gradeId = 100L;
        Long ownerProfessorId = 5L;
        Long wrongProfessorId = 6L;
        GradeSubmissionRequest request = new GradeSubmissionRequest();
        request.setGrade(95.0);

        SubmissionGrade existingGrade = new SubmissionGrade();
        existingGrade.setId(gradeId);
        existingGrade.setSubmissionId(submissionId);
        existingGrade.setProfessorId(ownerProfessorId);

        when(gradeRepository.findById(gradeId)).thenReturn(Optional.of(existingGrade));

        // Act & Assert — must be 403
        assertThrows(com.spms.backend.exception.ForbiddenException.class, () -> 
            submissionGradeService.updateGrade(submissionId, gradeId, wrongProfessorId, request)
        );
        verify(gradeRepository, never()).save(any());
    }

    @Test
    void updateGrade_GradeNotFound_ThrowsNotFound() {
        // Arrange
        Long submissionId = 1L;
        Long gradeId = 999L;
        Long professorId = 5L;
        GradeSubmissionRequest request = new GradeSubmissionRequest();
        request.setGrade(85.0);

        when(gradeRepository.findById(gradeId)).thenReturn(Optional.empty());

        // Act & Assert — must be 404
        assertThrows(com.spms.backend.exception.NotFoundException.class, () ->
            submissionGradeService.updateGrade(submissionId, gradeId, professorId, request)
        );
    }

    @Test
    void updateGrade_GradeBelongsToDifferentSubmission_ThrowsNotFound() {
        // Arrange
        Long submissionId = 1L;
        Long differentSubmissionId = 99L;
        Long gradeId = 100L;
        Long professorId = 5L;
        GradeSubmissionRequest request = new GradeSubmissionRequest();
        request.setGrade(85.0);

        SubmissionGrade existingGrade = new SubmissionGrade();
        existingGrade.setId(gradeId);
        existingGrade.setSubmissionId(differentSubmissionId); // belongs to another submission
        existingGrade.setProfessorId(professorId);

        when(gradeRepository.findById(gradeId)).thenReturn(Optional.of(existingGrade));

        // Act & Assert — must be 404 (grade not found for this submission)
        assertThrows(com.spms.backend.exception.NotFoundException.class, () ->
            submissionGradeService.updateGrade(submissionId, gradeId, professorId, request)
        );
        verify(gradeRepository, never()).save(any());
    }

    @Test
    void updateGrade_UpdatesGradedAtTimestamp() {
        // Arrange
        Long submissionId = 1L;
        Long gradeId = 100L;
        Long professorId = 5L;
        GradeSubmissionRequest request = new GradeSubmissionRequest();
        request.setGrade(90.0);

        java.time.LocalDateTime originalTime = java.time.LocalDateTime.of(2025, 1, 1, 12, 0);
        SubmissionGrade existingGrade = new SubmissionGrade();
        existingGrade.setId(gradeId);
        existingGrade.setSubmissionId(submissionId);
        existingGrade.setProfessorId(professorId);
        existingGrade.setScore(80.0);
        existingGrade.setGradedAt(originalTime);

        Submission submission = new Submission();
        submission.setId(submissionId);
        submission.setGroupId(10L);

        when(gradeRepository.findById(gradeId)).thenReturn(Optional.of(existingGrade));
        when(scheduleRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        when(gradeRepository.save(any(SubmissionGrade.class))).thenReturn(existingGrade);
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
        when(assignmentRepository.findTopByGroupIdAndStatusOrderByAssignedAtDesc(10L, "ASSIGNED"))
                .thenReturn(Optional.empty());

        // Act
        submissionGradeService.updateGrade(submissionId, gradeId, professorId, request);

        // Assert — gradedAt should have been refreshed
        assertNotEquals(originalTime, existingGrade.getGradedAt());
        assertNotNull(existingGrade.getGradedAt());
    }
}

