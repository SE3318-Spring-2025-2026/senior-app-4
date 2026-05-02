package com.spms.backend.service;

import com.spms.backend.model.*;
import com.spms.backend.model.notification.Notification;
import com.spms.backend.model.notification.NotificationType;
import com.spms.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class DisbandUnassignedGroupsTest {

    @Autowired
    private AdvisorDeadlineDisbandService disbandService;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    private User coordinator;
    private User professor;
    private User studentLeaderA;
    private User studentLeaderB;
    private User studentMemberB;

    @BeforeEach
    void setUp() {
        // 1. Create Coordinator
        coordinator = createTestUser("Coordinator " + UUID.randomUUID(), "coord_" + UUID.randomUUID() + "@test.com", "coordinator");

        // 2. Create Professor
        professor = createTestUser("Professor " + UUID.randomUUID(), "prof_" + UUID.randomUUID() + "@test.com", "professor");

        // 3. Create Students
        studentLeaderA = createTestStudent("Student A " + UUID.randomUUID(), "S_" + UUID.randomUUID());
        studentLeaderB = createTestStudent("Student B " + UUID.randomUUID(), "S_" + UUID.randomUUID());
        studentMemberB = createTestStudent("Student B-Member " + UUID.randomUUID(), "S_" + UUID.randomUUID());

        // 4. Setup Schedule with past deadline
        Schedule schedule = new Schedule();
        schedule.setGroupFormationDeadline(Instant.now().minus(2, ChronoUnit.DAYS));
        schedule.setAdvisorAssignmentDeadline(Instant.now().minus(1, ChronoUnit.HOURS));
        scheduleRepository.save(schedule);
    }

    @Test
    void testDisbandOnlyUnassignedGroups() {
        // GIVEN: Group A is assigned an advisor
        Group groupA = new Group();
        groupA.setGroupName("Assigned Group " + UUID.randomUUID());
        groupA.setLeader(studentLeaderA);
        groupA.setAdvisor(professor);
        groupA.setStatus(GroupStatus.ADVISED);
        groupA = groupRepository.save(groupA);

        // GIVEN: Group B is unassigned
        Group groupB = new Group();
        groupB.setGroupName("Unassigned Group " + UUID.randomUUID());
        groupB.setLeader(studentLeaderB);
        groupB.setAdvisor(null);
        groupB.setStatus(GroupStatus.FORMING);
        groupB = groupRepository.save(groupB);

        // Add members to Group B
        addGroupMember(groupB, studentLeaderB, GroupRole.LEADER);
        addGroupMember(groupB, studentMemberB, GroupRole.MEMBER);

        // WHEN: Disband logic runs
        disbandService.disbandUnassignedGroups();

        // THEN: Group A should remain ADVISED
        Group updatedGroupA = groupRepository.findById(groupA.getId()).orElseThrow();
        assertEquals(GroupStatus.ADVISED, updatedGroupA.getStatus(), "Assigned group should not be disbanded");
        assertNotNull(updatedGroupA.getAdvisor(), "Assigned group should still have an advisor");

        // THEN: Group B should be DISBANDED
        Group updatedGroupB = groupRepository.findById(groupB.getId()).orElseThrow();
        assertEquals(GroupStatus.DISBANDED, updatedGroupB.getStatus(), "Unassigned group should be disbanded");

        // THEN: Members of Group B should receive GROUP_DISBANDED notification
        Page<Notification> notificationsPage = notificationRepository.findByToUser_UserId(studentMemberB.getUserId(), PageRequest.of(0, 20));
        List<Notification> notifications = notificationsPage.getContent();
        boolean hasDisbandNotification = notifications.stream()
                .anyMatch(n -> n.getType() == NotificationType.GROUP_DISBANDED);
        assertTrue(hasDisbandNotification, "Members of disbanded group should receive notification");
        
        Page<Notification> leaderNotificationsPage = notificationRepository.findByToUser_UserId(studentLeaderB.getUserId(), PageRequest.of(0, 20));
        List<Notification> leaderNotifications = leaderNotificationsPage.getContent();
        boolean leaderHasDisbandNotification = leaderNotifications.stream()
                .anyMatch(n -> n.getType() == NotificationType.GROUP_DISBANDED);
        assertTrue(leaderHasDisbandNotification, "Leader of disbanded group should receive notification");
    }

    private User createTestUser(String fullName, String email, String role) {
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setRole(role);
        user.setCreatedAt(Instant.now());
        return userRepository.save(user);
    }

    private User createTestStudent(String fullName, String studentId) {
        User user = new User();
        user.setFullName(fullName);
        user.setStudentId(studentId);
        user.setEmail(studentId + "@test.com");
        user.setRole("student");
        user.setCreatedAt(Instant.now());
        return userRepository.save(user);
    }

    private void addGroupMember(Group group, User user, GroupRole role) {
        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(user);
        member.setRole(role);
        member.setJoinedAt(Instant.now());
        groupMemberRepository.save(member);
        group.getMembers().add(member);
    }
}
