package com.spms.backend.service;

import com.spms.backend.model.*;
import com.spms.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DisbandUnassignedGroupsTest {

    @InjectMocks
    private AdvisorDeadlineDisbandService disbandService;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private GroupService groupService;

    private User coordinator;
    private User professor;
    private User studentLeaderA;
    private User studentLeaderB;

    @BeforeEach
    void setUp() {
        coordinator = new User();
        coordinator.setUserId(1L);
        coordinator.setRole("coordinator");

        professor = new User();
        professor.setUserId(2L);
        professor.setRole("professor");

        studentLeaderA = new User();
        studentLeaderA.setUserId(3L);
        studentLeaderA.setRole("student");

        studentLeaderB = new User();
        studentLeaderB.setUserId(4L);
        studentLeaderB.setRole("student");
    }

    @Test
    void testDisbandOnlyUnassignedGroups() {
        // GIVEN: Schedule with past deadline
        Schedule schedule = new Schedule();
        schedule.setAdvisorAssignmentDeadline(Instant.now().minus(1, ChronoUnit.HOURS));
        when(scheduleRepository.findTopByOrderByIdDesc()).thenReturn(Optional.of(schedule));

        // GIVEN: Coordinator exists
        when(userRepository.findAllByRoleIgnoreCase("coordinator")).thenReturn(Collections.singletonList(coordinator));

        // GIVEN: Group A is assigned an advisor (but the query findByAdvisorIsNullAndStatusNot should filter it out)
        // Actually, the service calls groupRepository.findByAdvisorIsNullAndStatusNot(GroupStatus.DISBANDED)
        
        // GIVEN: Group B is unassigned
        Group groupB = new Group();
        groupB.setId(102L);
        groupB.setGroupName("Unassigned Group");
        groupB.setLeader(studentLeaderB);
        groupB.setAdvisor(null);
        groupB.setStatus(GroupStatus.FORMING);

        when(groupRepository.findByAdvisorIsNullAndStatusNot(GroupStatus.DISBANDED))
                .thenReturn(Collections.singletonList(groupB));

        // WHEN: Disband logic runs
        disbandService.disbandUnassignedGroups();

        // THEN: groupService.disbandGroup should be called for groupB
        verify(groupService, times(1)).disbandGroup(eq(102L), eq(1L), eq("coordinator"));
        
        // THEN: groupService.disbandGroup should NOT be called for other groups (not returned by repo)
        verify(groupService, times(1)).disbandGroup(any(), any(), any());
    }

    @Test
    void testNoDisbandIfDeadlineNotReached() {
        // GIVEN: Schedule with future deadline
        Schedule schedule = new Schedule();
        schedule.setAdvisorAssignmentDeadline(Instant.now().plus(1, ChronoUnit.HOURS));
        when(scheduleRepository.findTopByOrderByIdDesc()).thenReturn(Optional.of(schedule));

        // WHEN: Disband logic runs
        disbandService.disbandUnassignedGroups();

        // THEN: No groups should be disbanded
        verify(groupRepository, never()).findByAdvisorIsNullAndStatusNot(any());
        verify(groupService, never()).disbandGroup(any(), any(), any());
    }
}
