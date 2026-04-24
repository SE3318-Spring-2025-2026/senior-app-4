package com.spms.backend.service;

import com.spms.backend.model.Group;
import com.spms.backend.model.GroupStatus;
import com.spms.backend.model.Schedule;
import com.spms.backend.model.User;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.ScheduleRepository;
import com.spms.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdvisorDeadlineDisbandServiceTest {

    private static final Long COORDINATOR_ID = 1L;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupService groupService;

    @Mock
    private UserRepository userRepository;

    private AdvisorDeadlineDisbandService service;

    @BeforeEach
    void setUp() {
        service = new AdvisorDeadlineDisbandService(
                scheduleRepository, groupRepository, groupService, userRepository);
    }

    @Test
    void skipsWhenNoScheduleConfigured() {
        when(scheduleRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());

        service.disbandUnassignedGroups();

        verifyNoInteractions(groupRepository, groupService, userRepository);
    }

    @Test
    void skipsWhenDeadlineNotYetReached() {
        when(scheduleRepository.findTopByOrderByIdDesc())
                .thenReturn(Optional.of(scheduleWithAdvisorDeadline(Instant.now().plus(1, ChronoUnit.DAYS))));

        service.disbandUnassignedGroups();

        verifyNoInteractions(groupRepository, groupService, userRepository);
    }

    @Test
    void skipsWhenNoCoordinatorFound() {
        when(scheduleRepository.findTopByOrderByIdDesc())
                .thenReturn(Optional.of(scheduleWithAdvisorDeadline(Instant.now().minus(1, ChronoUnit.HOURS))));
        when(userRepository.findAllByRole("coordinator")).thenReturn(Collections.emptyList());

        service.disbandUnassignedGroups();

        verify(groupService, never()).disbandGroup(anyLong(), anyLong(), anyString());
        verify(groupRepository, never()).findByAdvisorIsNullAndStatusNot(any());
    }

    @Test
    void skipsWhenNoUnassignedGroupsFound() {
        setUpHappyPreconditions();
        when(groupRepository.findByAdvisorIsNullAndStatusNot(GroupStatus.DISBANDED))
                .thenReturn(Collections.emptyList());

        service.disbandUnassignedGroups();

        verify(groupService, never()).disbandGroup(anyLong(), anyLong(), anyString());
    }

    @Test
    void disbandsOnlyGroupsWithNullAdvisor() {
        setUpHappyPreconditions();
        Group g1 = groupWithId(10L, null);
        Group g2 = groupWithId(11L, null);
        when(groupRepository.findByAdvisorIsNullAndStatusNot(GroupStatus.DISBANDED))
                .thenReturn(List.of(g1, g2));

        service.disbandUnassignedGroups();

        verify(groupService).disbandGroup(eq(10L), eq(COORDINATOR_ID), eq("coordinator"));
        verify(groupService).disbandGroup(eq(11L), eq(COORDINATOR_ID), eq("coordinator"));
        verify(groupService, times(2)).disbandGroup(anyLong(), anyLong(), anyString());
    }

    @Test
    void doesNotTouchGroupsThatHaveAnAdvisor() {
        setUpHappyPreconditions();
        // Repository filter already excludes advised groups; verify we ONLY call disbandGroup
        // for the set returned by findByAdvisorIsNullAndStatusNot — no extra lookups that
        // might sweep advised groups.
        when(groupRepository.findByAdvisorIsNullAndStatusNot(GroupStatus.DISBANDED))
                .thenReturn(Collections.emptyList());

        service.disbandUnassignedGroups();

        verify(groupRepository).findByAdvisorIsNullAndStatusNot(GroupStatus.DISBANDED);
        verify(groupService, never()).disbandGroup(anyLong(), anyLong(), anyString());
    }

    @Test
    void continuesWhenIndividualGroupDisbandFails() {
        setUpHappyPreconditions();
        Group g1 = groupWithId(10L, null);
        Group g2 = groupWithId(11L, null);
        Group g3 = groupWithId(12L, null);
        when(groupRepository.findByAdvisorIsNullAndStatusNot(GroupStatus.DISBANDED))
                .thenReturn(List.of(g1, g2, g3));

        lenient().doThrow(new RuntimeException("db glitch"))
                .when(groupService).disbandGroup(11L, COORDINATOR_ID, "coordinator");

        service.disbandUnassignedGroups();

        verify(groupService).disbandGroup(eq(10L), eq(COORDINATOR_ID), eq("coordinator"));
        verify(groupService).disbandGroup(eq(11L), eq(COORDINATOR_ID), eq("coordinator"));
        verify(groupService).disbandGroup(eq(12L), eq(COORDINATOR_ID), eq("coordinator"));
    }

    // ─── Helpers ──────────────────────────────────────────────────────

    private void setUpHappyPreconditions() {
        when(scheduleRepository.findTopByOrderByIdDesc())
                .thenReturn(Optional.of(scheduleWithAdvisorDeadline(Instant.now().minus(1, ChronoUnit.HOURS))));
        User coordinator = new User();
        coordinator.setUserId(COORDINATOR_ID);
        when(userRepository.findAllByRole("coordinator")).thenReturn(List.of(coordinator));
    }

    private Schedule scheduleWithAdvisorDeadline(Instant advisorDeadline) {
        Schedule s = new Schedule();
        s.setGroupFormationDeadline(advisorDeadline.minus(7, ChronoUnit.DAYS));
        s.setAdvisorAssignmentDeadline(advisorDeadline);
        return s;
    }

    private Group groupWithId(Long id, User advisor) {
        Group g = new Group();
        g.setId(id);
        g.setAdvisor(advisor);
        g.setStatus(advisor != null ? GroupStatus.ADVISED : GroupStatus.FORMING);
        return g;
    }
}
