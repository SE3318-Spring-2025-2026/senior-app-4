package com.spms.backend.service.impl;

import com.spms.backend.dto.response.AdvisorAssignmentListResponse;
import com.spms.backend.exception.ForbiddenException;
import com.spms.backend.model.Group;
import com.spms.backend.model.GroupStatus;
import com.spms.backend.model.User;
import com.spms.backend.repository.GroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class AdvisorAssignmentServiceImplTest {

    @Mock
    private GroupRepository groupRepository;

    private AdvisorAssignmentServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AdvisorAssignmentServiceImpl(groupRepository);
    }

    @Test
    void coordinator_seesAssignedTeamsOnly_byDefault() {
        Group withAdvisor = group(1L, "Team A", 10L, "Dr. Smith");
        Group withoutAdvisor = group(2L, "Team B", null, null);
        when(groupRepository.findAllNonDisbandedWithAdvisorAndLeaderFetched(GroupStatus.DISBANDED))
                .thenReturn(List.of(withAdvisor, withoutAdvisor));

        AdvisorAssignmentListResponse res = service.listAdvisorAssignments("coordinator", 99L, null, null);

        assertEquals("success", res.status());
        assertEquals(1, res.data().size());
        assertEquals(1L, res.data().get(0).groupId());
        assertEquals(10L, res.data().get(0).advisorId());
    }

    @Test
    void coordinator_hasAdvisorFalse_listsUnassigned() {
        Group withAdvisor = group(1L, "Team A", 10L, "Dr. Smith");
        Group withoutAdvisor = group(2L, "Team B", null, null);
        when(groupRepository.findAllNonDisbandedWithAdvisorAndLeaderFetched(GroupStatus.DISBANDED))
                .thenReturn(List.of(withAdvisor, withoutAdvisor));

        AdvisorAssignmentListResponse res =
                service.listAdvisorAssignments("COORDINATOR", 99L, null, false);

        assertEquals(1, res.data().size());
        assertEquals(2L, res.data().get(0).groupId());
        assertNull(res.data().get(0).advisorId());
    }

    @Test
    void professor_seesOnlyTheirGroups() {
        Group mine = group(1L, "Mine", 5L, "Me");
        Group other = group(2L, "Other", 99L, "Other Prof");
        when(groupRepository.findAllNonDisbandedWithAdvisorAndLeaderFetched(GroupStatus.DISBANDED))
                .thenReturn(List.of(mine, other));

        AdvisorAssignmentListResponse res = service.listAdvisorAssignments("professor", 5L, null, null);

        assertEquals(1, res.data().size());
        assertEquals(5L, res.data().get(0).advisorId());
    }

    @Test
    void student_forbidden() {
        assertThrows(
                ForbiddenException.class,
                () -> service.listAdvisorAssignments("student", 1L, null, null));
    }

    private static Group group(Long id, String name, Long advisorUserId, String advisorName) {
        Group g = new Group();
        g.setId(id);
        g.setGroupName(name);
        g.setStatus(GroupStatus.ADVISED);
        User leader = new User();
        leader.setFullName("Leader");
        g.setLeader(leader);
        if (advisorUserId != null) {
            User advisor = new User();
            advisor.setUserId(advisorUserId);
            advisor.setFullName(advisorName);
            g.setAdvisor(advisor);
        }
        return g;
    }
}
