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
    private GradeRepository gradeRepository;
    @Mock
    private GroupCommitteeAssignmentRepository assignmentRepository;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private NotificationService notificationService;

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

        Grade grade = new Grade(submissionId, professorId, 80.0, null);
        grade.setId(1L);

        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
        when(gradeRepository.existsBySubmissionIdAndProfessorId(submissionId, professorId)).thenReturn(false);
        when(assignmentRepository.findTopByGroupIdAndStatusOrderByAssignedAtDesc(groupId, "ASSIGNED"))
                .thenReturn(Optional.of(assignment));
        when(gradeRepository.save(any(Grade.class))).thenReturn(grade);
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

        Grade grade = new Grade(submissionId, professorId, 80.0, null);
        grade.setId(1L);

        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
        when(gradeRepository.existsBySubmissionIdAndProfessorId(submissionId, professorId)).thenReturn(false);
        when(assignmentRepository.findTopByGroupIdAndStatusOrderByAssignedAtDesc(groupId, "ASSIGNED"))
                .thenReturn(Optional.of(assignment));
        when(gradeRepository.save(any(Grade.class))).thenReturn(grade);
        when(gradeRepository.findBySubmissionId(submissionId)).thenReturn(Collections.singletonList(grade));

        // Act
        GradeSubmissionResponse response = submissionGradeService.submitGrade(submissionId, professorId, request);

        // Assert
        assertFalse(response.getData().getIsGradingComplete());
        assertNull(submission.getFinalGrade());
        verify(notificationService, never()).createSystemAlert(anyLong(), anyString(), anyString(), anyString());
    }
}

