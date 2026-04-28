package com.spms.backend.service;

import com.spms.backend.dto.SubmissionResponse;
import com.spms.backend.dto.response.RevisionCreateResponseDto;
import com.spms.backend.dto.response.RevisionHistoryResponseDto;
import com.spms.backend.dto.response.SubmissionListResponse;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.exception.ForbiddenException;
import com.spms.backend.exception.NotFoundException;
import com.spms.backend.model.Committee;
import com.spms.backend.model.Group;
import com.spms.backend.model.GroupCommitteeAssignment;
import com.spms.backend.model.GroupMember;
import com.spms.backend.model.Submission;
import com.spms.backend.model.User;
import com.spms.backend.model.enums.DeliverableType;
import com.spms.backend.model.enums.SubmissionStatus;
import com.spms.backend.repository.CommitteeAdvisorRepository;
import com.spms.backend.repository.GroupCommitteeAssignmentRepository;
import com.spms.backend.repository.GroupMemberRepository;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.ScheduleRepository;
import com.spms.backend.repository.SubmissionRepository;
import com.spms.backend.repository.UserRepository;
import com.spms.backend.service.FileStorageService;
import com.spms.backend.service.impl.SubmissionServiceImpl;
import org.springframework.mock.web.MockMultipartFile;
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

import static org.junit.jupiter.api.Assertions.*;
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
    @Mock
    private ScheduleRepository scheduleRepository;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private CommitteeAdvisorRepository committeeAdvisorRepository;

    private SubmissionService submissionService;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        submissionService = new SubmissionServiceImpl(
                submissionRepository,
                groupRepository,
                assignmentRepository,
                notificationService,
                userRepository,
                groupMemberRepository,
                scheduleRepository,
                fileStorageService,
                committeeAdvisorRepository);
        pageable = PageRequest.of(0, 10);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Existing tests (submit + listSubmissions)
    // ──────────────────────────────────────────────────────────────────────────

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
        when(fileStorageService.store(any())).thenReturn("/api/v1/files/test.pdf");

        MockMultipartFile file = new MockMultipartFile("file", "file.pdf", "application/pdf", new byte[0]);
        SubmissionResponse response = submissionService.submit(
                10L, DeliverableType.PROPOSAL, "content", file, 1L);

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

        MockMultipartFile file = new MockMultipartFile("file", "file.pdf", "application/pdf", new byte[0]);
        assertThrows(BadRequestException.class, () ->
                submissionService.submit(10L, DeliverableType.STATEMENT_OF_WORK, "content", file, 1L));
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

        MockMultipartFile file = new MockMultipartFile("file", "file.pdf", "application/pdf", new byte[0]);
        assertThrows(BadRequestException.class, () ->
                submissionService.submit(10L, DeliverableType.REVISED_PROPOSAL, "content", file, 1L));
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

        MockMultipartFile file = new MockMultipartFile("file", "file.pdf", "application/pdf", new byte[0]);
        assertThrows(ForbiddenException.class, () ->
                submissionService.submit(10L, DeliverableType.PROPOSAL, "content", file, 1L));
    }

    @Test
    void listSubmissionsReturnsAllForCoordinator() {
        Submission submission = buildSubmission(101L, 7L);
        when(submissionRepository.findWithFilters(null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(submission)));

        SubmissionListResponse response = submissionService.listSubmissions(1L, "COORDINATOR", null, null, null, null, pageable);

        assertEquals("success", response.status());
        assertEquals(1, response.data().size());
        assertEquals(101L, response.data().get(0).id());
        verify(submissionRepository).findWithFilters(null, null, null, null, pageable);
    }

    @Test
    void listSubmissionsReturnsOnlyOwnGroupForStudent() {
        Group group = new Group();
        group.setId(25L);
        GroupMember member = new GroupMember();
        member.setGroup(group);

        Submission submission = buildSubmission(102L, 25L);
        when(groupMemberRepository.findTopByUser_UserId(55L)).thenReturn(Optional.of(member));
        when(submissionRepository.findWithFilters(25L, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(submission)));

        SubmissionListResponse response = submissionService.listSubmissions(55L, "STUDENT", null, null, null, null, pageable);

        assertEquals(1, response.data().size());
        assertEquals(25L, response.data().get(0).teamId());
        verify(submissionRepository).findWithFilters(25L, null, null, null, pageable);
    }

    @Test
    void listSubmissionsThrowsForbiddenWhenStudentHasNoGroup() {
        when(groupMemberRepository.findTopByUser_UserId(55L)).thenReturn(Optional.empty());

        assertThrows(
                ForbiddenException.class,
                () -> submissionService.listSubmissions(55L, "STUDENT", null, null, null, null, pageable));
    }

    @Test
    void listSubmissionsThrowsForbiddenForUnauthorizedRole() {
        assertThrows(
                ForbiddenException.class,
                () -> submissionService.listSubmissions(77L, "ADMIN", null, null, null, null, pageable));
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Revision tests  [P3-REV-1 / P3-REV-2]
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void createRevision_HappyPath_Returns201WithCorrectData() {
        User leader = new User();
        leader.setUserId(1L);
        Group group = new Group();
        group.setId(10L);
        group.setLeader(leader);

        Submission parent = buildSubmission(200L, 10L);
        parent.setStatus(SubmissionStatus.REVISION_REQUESTED);
        parent.setVersion(1);
        parent.setCommitteeId(100L);

        when(submissionRepository.findById(200L)).thenReturn(Optional.of(parent));
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(scheduleRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty()); // No deadline
        GroupCommitteeAssignment assignment = new GroupCommitteeAssignment();
        Committee committee = new Committee();
        committee.setCommitteeId(100L);
        assignment.setCommittee(committee);

        when(assignmentRepository.findTopByGroupIdAndStatusOrderByAssignedAtDesc(10L, "ASSIGNED"))
                .thenReturn(Optional.of(assignment));
        
        when(submissionRepository.save(any())).thenAnswer(i -> {
            Submission s = i.getArgument(0);
            if (s.getId() == null) s.setId(201L);
            return s;
        });
        when(userRepository.findAllByRole("COORDINATOR")).thenReturn(Collections.emptyList());
        when(fileStorageService.store(any())).thenReturn("/api/v1/files/revision.pdf");

        MockMultipartFile revFile = new MockMultipartFile("file", "revision.pdf", "application/pdf", new byte[0]);
        RevisionCreateResponseDto response = submissionService.createRevision(200L, revFile, "Fixed issues", 1L);

        assertEquals("success", response.getStatus());
        assertNotNull(response.getData());
        assertEquals(200L, response.getData().getParentSubmissionId());
        assertEquals(2, response.getData().getRevisionNumber());
        assertEquals("PENDING_REVIEW", response.getData().getStatus());
        // Parent should have been saved with SUPERSEDED status
        assertEquals(SubmissionStatus.SUPERSEDED, parent.getStatus());
    }

    @Test
    void createRevision_DeadlinePassed_ThrowsForbidden() {
        User leader = new User();
        leader.setUserId(1L);
        Group group = new Group();
        group.setId(10L);
        group.setLeader(leader);

        Submission parent = buildSubmission(200L, 10L);
        parent.setStatus(SubmissionStatus.REVISION_REQUESTED);

        com.spms.backend.model.Schedule schedule = new com.spms.backend.model.Schedule();
        schedule.setProposalRevisionDeadline(java.time.Instant.now().minusSeconds(3600)); // 1 hour ago

        when(submissionRepository.findById(200L)).thenReturn(Optional.of(parent));
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(scheduleRepository.findTopByOrderByIdDesc()).thenReturn(Optional.of(schedule));

        MockMultipartFile deadlineFile = new MockMultipartFile("file", "file.pdf", "application/pdf", new byte[0]);
        assertThrows(ForbiddenException.class, () ->
                submissionService.createRevision(200L, deadlineFile, "desc", 1L));
    }

    @Test
    void testCreateRevision_Returns400_WhenParentNotInRevisionRequestedStatus() {
        Submission parent = buildSubmission(200L, 10L);
        parent.setStatus(SubmissionStatus.PENDING_REVIEW); // Not REVISION_REQUESTED

        when(submissionRepository.findById(200L)).thenReturn(Optional.of(parent));

        MockMultipartFile badFile = new MockMultipartFile("file", "file.pdf", "application/pdf", new byte[0]);
        assertThrows(BadRequestException.class, () ->
                submissionService.createRevision(200L, badFile, null, 1L));
    }

    @Test
    void testCreateRevision_Returns404_WhenParentNotFound() {
        when(submissionRepository.findById(999L)).thenReturn(Optional.empty());

        MockMultipartFile nfFile = new MockMultipartFile("file", "file.pdf", "application/pdf", new byte[0]);
        assertThrows(NotFoundException.class, () ->
                submissionService.createRevision(999L, nfFile, null, 1L));
    }

    @Test
    void testGetRevisions_Returns404_WhenSubmissionNotFound() {
        when(submissionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                submissionService.getRevisionHistory(999L, 1L, "COORDINATOR"));
    }

    @Test
    void testGetRevisions_Returns403_WhenStudentAccessesOtherGroup() {
        Submission otherGroupSubmission = buildSubmission(200L, 99L); // Group 99
        when(submissionRepository.findById(200L)).thenReturn(Optional.of(otherGroupSubmission));
        
        Group group = new Group();
        group.setId(10L); // Student's group is 10
        GroupMember member = new GroupMember();
        member.setGroup(group);
        when(groupMemberRepository.findTopByUser_UserId(5L)).thenReturn(Optional.of(member));

        assertThrows(ForbiddenException.class, () ->
                submissionService.getRevisionHistory(200L, 5L, "STUDENT"));
    }

    @Test
    void getRevisionHistory_HappyPath_ReturnsFullChain() {
        Submission root = buildSubmission(200L, 10L);
        root.setVersion(1);
        root.setParentSubmissionId(null);

        Submission rev1 = buildSubmission(201L, 10L);
        rev1.setVersion(2);
        rev1.setParentSubmissionId(200L);

        when(submissionRepository.findById(200L)).thenReturn(Optional.of(root));
        when(submissionRepository.findAllByGroupIdAndDeliverableType(10L, DeliverableType.PROPOSAL))
                .thenReturn(List.of(root, rev1));

        RevisionHistoryResponseDto response = submissionService.getRevisionHistory(200L, 1L, "COORDINATOR");

        assertEquals("success", response.getStatus());
        assertEquals(2, response.getData().size());
        assertEquals(1, response.getData().get(0).getRevisionNumber());
        assertEquals(2, response.getData().get(1).getRevisionNumber());
    }

    @Test
    void getRevisionHistory_DeepChain_ReturnsAllLevels() {
        // v1 -> v2 -> v3
        Submission v1 = buildSubmission(100L, 10L);
        v1.setVersion(1);
        v1.setParentSubmissionId(null);

        Submission v2 = buildSubmission(101L, 10L);
        v2.setVersion(2);
        v2.setParentSubmissionId(100L);

        Submission v3 = buildSubmission(102L, 10L);
        v3.setVersion(3);
        v3.setParentSubmissionId(101L);

        // Mocking: calling for v3
        when(submissionRepository.findById(102L)).thenReturn(Optional.of(v3));
        when(submissionRepository.findById(101L)).thenReturn(Optional.of(v2));
        when(submissionRepository.findById(100L)).thenReturn(Optional.of(v1));
        
        when(submissionRepository.findAllByGroupIdAndDeliverableType(10L, DeliverableType.PROPOSAL))
                .thenReturn(List.of(v1, v2, v3));

        RevisionHistoryResponseDto response = submissionService.getRevisionHistory(102L, 1L, "COORDINATOR");

        assertEquals(3, response.getData().size());
        assertEquals(1, response.getData().get(0).getRevisionNumber());
        assertEquals(2, response.getData().get(1).getRevisionNumber());
        assertEquals(3, response.getData().get(2).getRevisionNumber());
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private Submission buildSubmission(Long submissionId, Long groupId) {
        Submission submission = new Submission();
        submission.setId(submissionId);
        submission.setGroupId(groupId);
        submission.setDeliverableType(DeliverableType.PROPOSAL);
        submission.setStatus(SubmissionStatus.PENDING_REVIEW);
        submission.setCommitteeId(9L);
        submission.setContent("content");
        submission.setCreatedAt(LocalDateTime.now());
        return submission;
    }
}
