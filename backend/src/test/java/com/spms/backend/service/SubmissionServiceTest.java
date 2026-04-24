package com.spms.backend.service;

import com.spms.backend.dto.SubmissionResponse;
import com.spms.backend.dto.response.SubmissionListResponse;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.exception.ForbiddenException;
import com.spms.backend.model.Committee;
import com.spms.backend.model.Group;
import com.spms.backend.model.GroupCommitteeAssignment;
import com.spms.backend.model.GroupMember;
import com.spms.backend.model.Submission;
import com.spms.backend.model.User;
import com.spms.backend.model.enums.DeliverableType;
import com.spms.backend.model.enums.SubmissionStatus;
import com.spms.backend.repository.GroupCommitteeAssignmentRepository;
import com.spms.backend.repository.GroupMemberRepository;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.SubmissionRepository;
import com.spms.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private GroupCommitteeAssignmentRepository assignmentRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private GroupMemberRepository groupMemberRepository;

    private SubmissionService submissionService;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        submissionService = new SubmissionService(
                submissionRepository,
                groupRepository,
                assignmentRepository,
                notificationService,
                userRepository,
                groupMemberRepository);
        pageable = PageRequest.of(0, 10);
    }

    @Test
    void submitHappyPath() {
        User leader = new User();
        leader.setUserId(1L);
        Group group = new Group();
        group.setId(10L);
        group.setLeader(leader);
        Committee committee = new Committee();
        committee.setCommitteeId(100L);
        GroupCommitteeAssignment assignment = new GroupCommitteeAssignment();
        assignment.setCommittee(committee);

        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(assignmentRepository.findTopByGroupIdAndStatusOrderByAssignedAtDesc(10L, "ASSIGNED"))
                .thenReturn(Optional.of(assignment));
        when(submissionRepository.save(any())).thenAnswer(i -> {
            Submission s = i.getArgument(0);
            s.setId(500L);
            return s;
        });
        when(userRepository.findAllByRole("COORDINATOR")).thenReturn(Collections.emptyList());

        SubmissionResponse response = submissionService.submit(
                10L, DeliverableType.PROPOSAL, "content", "file.pdf", 1L);

        assertEquals("success", response.getStatus());
        assertNotNull(response.getData());
        assertEquals(500L, response.getData().getId());
        assertEquals(SubmissionStatus.PENDING_REVIEW, response.getData().getStatus());
        verify(submissionRepository).save(any());
    }

    @Test
    void submitSowBeforeProposalGradedThrowsBadRequest() {
        User leader = new User();
        leader.setUserId(1L);
        Group group = new Group();
        group.setId(10L);
        group.setLeader(leader);
        Committee committee = new Committee();
        committee.setCommitteeId(100L);
        GroupCommitteeAssignment assignment = new GroupCommitteeAssignment();
        assignment.setCommittee(committee);

        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(assignmentRepository.findTopByGroupIdAndStatusOrderByAssignedAtDesc(10L, "ASSIGNED"))
                .thenReturn(Optional.of(assignment));
        when(submissionRepository.findTopByGroupIdAndDeliverableTypeOrderByCreatedAtDesc(
                10L, DeliverableType.PROPOSAL)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () ->
                submissionService.submit(10L, DeliverableType.STATEMENT_OF_WORK, "content", "file.pdf", 1L));
    }

    @Test
    void submitRevisedProposalWithoutRevisionRequestedThrowsBadRequest() {
        User leader = new User();
        leader.setUserId(1L);
        Group group = new Group();
        group.setId(10L);
        group.setLeader(leader);
        Committee committee = new Committee();
        committee.setCommitteeId(100L);
        GroupCommitteeAssignment assignment = new GroupCommitteeAssignment();
        assignment.setCommittee(committee);

        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(assignmentRepository.findTopByGroupIdAndStatusOrderByAssignedAtDesc(10L, "ASSIGNED"))
                .thenReturn(Optional.of(assignment));

        Submission proposal = new Submission();
        proposal.setStatus(SubmissionStatus.PENDING_REVIEW);
        when(submissionRepository.findTopByGroupIdAndDeliverableTypeOrderByCreatedAtDesc(
                10L, DeliverableType.PROPOSAL)).thenReturn(Optional.of(proposal));

        assertThrows(BadRequestException.class, () ->
                submissionService.submit(10L, DeliverableType.REVISED_PROPOSAL, "content", "file.pdf", 1L));
    }

    @Test
    void submitNoCommitteeAssignmentThrowsForbidden() {
        User leader = new User();
        leader.setUserId(1L);
        Group group = new Group();
        group.setId(10L);
        group.setLeader(leader);

        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(assignmentRepository.findTopByGroupIdAndStatusOrderByAssignedAtDesc(10L, "ASSIGNED"))
                .thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () ->
                submissionService.submit(10L, DeliverableType.PROPOSAL, "content", "file.pdf", 1L));
    }

    @Test
    void listSubmissionsReturnsAllForCoordinator() {
        Submission submission = buildSubmission(101L, 7L);
        when(submissionRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(submission)));

        SubmissionListResponse response = submissionService.listSubmissions(1L, "COORDINATOR", pageable);

        assertEquals("success", response.status());
        assertEquals(1, response.data().size());
        assertEquals(101L, response.data().get(0).id());
        verify(submissionRepository).findAll(pageable);
    }

    @Test
    void listSubmissionsReturnsOnlyOwnGroupForStudent() {
        Group group = new Group();
        group.setId(25L);
        GroupMember member = new GroupMember();
        member.setGroup(group);

        Submission submission = buildSubmission(102L, 25L);
        when(groupMemberRepository.findTopByUser_UserId(55L)).thenReturn(Optional.of(member));
        when(submissionRepository.findByGroupId(25L, pageable)).thenReturn(new PageImpl<>(List.of(submission)));

        SubmissionListResponse response = submissionService.listSubmissions(55L, "STUDENT", pageable);

        assertEquals(1, response.data().size());
        assertEquals(25L, response.data().get(0).teamId());
        verify(submissionRepository).findByGroupId(25L, pageable);
    }

    @Test
    void listSubmissionsThrowsForbiddenWhenStudentHasNoGroup() {
        when(groupMemberRepository.findTopByUser_UserId(55L)).thenReturn(Optional.empty());

        assertThrows(
                ForbiddenException.class,
                () -> submissionService.listSubmissions(55L, "STUDENT", pageable));
    }

    @Test
    void listSubmissionsThrowsForbiddenForUnauthorizedRole() {
        assertThrows(
                ForbiddenException.class,
                () -> submissionService.listSubmissions(77L, "PROFESSOR", pageable));
    }

    private Submission buildSubmission(Long submissionId, Long groupId) {
        Submission submission = new Submission();
        submission.setId(submissionId);
        submission.setGroupId(groupId);
        submission.setDeliverableType(DeliverableType.PROPOSAL);
        submission.setStatus(SubmissionStatus.PENDING_REVIEW);
        submission.setCommitteeId(9L);
        submission.setCreatedAt(LocalDateTime.now());
        return submission;
    }
}
