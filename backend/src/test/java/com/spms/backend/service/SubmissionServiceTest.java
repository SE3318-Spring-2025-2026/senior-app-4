package com.spms.backend.service;

import com.spms.backend.dto.SubmissionResponse;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.exception.ForbiddenException;
import com.spms.backend.model.Committee;
import com.spms.backend.model.Group;
import com.spms.backend.model.GroupCommitteeAssignment;
import com.spms.backend.model.Submission;
import com.spms.backend.model.User;
import com.spms.backend.model.enums.DeliverableType;
import com.spms.backend.model.enums.SubmissionStatus;
import com.spms.backend.repository.GroupCommitteeAssignmentRepository;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.SubmissionRepository;
import com.spms.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SubmissionServiceTest {

    @Mock private SubmissionRepository submissionRepository;
    @Mock private GroupRepository groupRepository;
    @Mock private GroupCommitteeAssignmentRepository assignmentRepository;
    @Mock private NotificationService notificationService;
    @Mock private UserRepository userRepository;

    @InjectMocks private SubmissionService submissionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testHappyPath() {
        User leader = new User(); leader.setUserId(1L);
        Group group = new Group(); group.setId(10L); group.setLeader(leader);
        Committee committee = new Committee(); committee.setCommitteeId(100L);
        GroupCommitteeAssignment assignment = new GroupCommitteeAssignment(); assignment.setCommittee(committee);

        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(assignmentRepository.findTopByGroupIdAndStatusOrderByAssignedAtDesc(10L, "ASSIGNED")).thenReturn(Optional.of(assignment));
        when(submissionRepository.save(any())).thenAnswer(i -> {
            Submission s = i.getArgument(0);
            s.setId(500L);
            return s;
        });
        when(userRepository.findAllByRole("COORDINATOR")).thenReturn(Collections.emptyList());

        SubmissionResponse response = submissionService.submit(10L, DeliverableType.PROPOSAL, "content", "file.pdf", 1L);

        assertEquals("success", response.getStatus());
        assertNotNull(response.getData());
        assertEquals(500L, response.getData().getId());
        assertEquals(SubmissionStatus.PENDING_REVIEW, response.getData().getStatus());
        verify(submissionRepository).save(any());
    }

    @Test
    void testSowBeforeProposalGraded_ThrowsException() {
        User leader = new User(); leader.setUserId(1L);
        Group group = new Group(); group.setId(10L); group.setLeader(leader);
        Committee committee = new Committee(); committee.setCommitteeId(100L);
        GroupCommitteeAssignment assignment = new GroupCommitteeAssignment(); assignment.setCommittee(committee);

        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(assignmentRepository.findTopByGroupIdAndStatusOrderByAssignedAtDesc(10L, "ASSIGNED")).thenReturn(Optional.of(assignment));
        
        when(submissionRepository.findTopByGroupIdAndDeliverableTypeOrderByCreatedAtDesc(10L, DeliverableType.PROPOSAL)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> 
            submissionService.submit(10L, DeliverableType.STATEMENT_OF_WORK, "content", "file.pdf", 1L)
        );
    }

    @Test
    void testRevisedProposalWithoutRevisionRequested_ThrowsException() {
        User leader = new User(); leader.setUserId(1L);
        Group group = new Group(); group.setId(10L); group.setLeader(leader);
        Committee committee = new Committee(); committee.setCommitteeId(100L);
        GroupCommitteeAssignment assignment = new GroupCommitteeAssignment(); assignment.setCommittee(committee);

        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(assignmentRepository.findTopByGroupIdAndStatusOrderByAssignedAtDesc(10L, "ASSIGNED")).thenReturn(Optional.of(assignment));
        
        Submission proposal = new Submission(); proposal.setStatus(SubmissionStatus.PENDING_REVIEW);
        when(submissionRepository.findTopByGroupIdAndDeliverableTypeOrderByCreatedAtDesc(10L, DeliverableType.PROPOSAL)).thenReturn(Optional.of(proposal));

        assertThrows(BadRequestException.class, () -> 
            submissionService.submit(10L, DeliverableType.REVISED_PROPOSAL, "content", "file.pdf", 1L)
        );
    }

    @Test
    void testNoCommitteeAssignment_ThrowsForbidden() {
        User leader = new User(); leader.setUserId(1L);
        Group group = new Group(); group.setId(10L); group.setLeader(leader);

        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(assignmentRepository.findTopByGroupIdAndStatusOrderByAssignedAtDesc(10L, "ASSIGNED")).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> 
            submissionService.submit(10L, DeliverableType.PROPOSAL, "content", "file.pdf", 1L)
        );
    }
}
