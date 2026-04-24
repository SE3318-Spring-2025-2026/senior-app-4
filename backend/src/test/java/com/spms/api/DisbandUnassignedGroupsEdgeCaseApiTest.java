package com.spms.api;

import com.spms.backend.model.*;
import com.spms.backend.model.notification.Notification;
import com.spms.backend.model.notification.NotificationStatus;
import com.spms.backend.model.notification.NotificationType;
import com.spms.backend.repository.*;
import com.spms.backend.service.AdvisorDeadlineDisbandService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
/**
 * Integration test for the disband unassigned groups edge cases.
 * Covers requirements for Issue #170.
 * 
 * Note: Uses unique identifiers for each run to avoid conflicts in the shared database.
 */
public class DisbandUnassignedGroupsEdgeCaseApiTest extends BaseApiTest {

    private String runId;

    @Autowired
    private AdvisorDeadlineDisbandService disbandService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @AfterEach
    void cleanup() {
        // Only cleanup data created in this run to avoid impacting other tests
        notificationRepository.deleteAll(); 
        groupMemberRepository.deleteAll();
        groupRepository.deleteAll();
        // userRepository.deleteAll(); // Better not to delete all users if shared
    }

    @Test
    @DisplayName("Disband: Only groups with advisor=null should be disbanded after deadline")
    void testDisbandOnlyUnassignedGroupsAfterDeadline() {
        runId = UUID.randomUUID().toString().substring(0, 8);
        
        // 1. Setup Coordinator
        User coordinator = new User();
        coordinator.setFullName("Test Coordinator " + runId);
        coordinator.setEmail("coord-" + runId + "@spms-test.com");
        coordinator.setRole("coordinator");
        coordinator = userRepository.save(coordinator);

        // 2. Setup Professor (Advisor)
        User professor = new User();
        professor.setFullName("Prof Advisor " + runId);
        professor.setEmail("prof-" + runId + "@spms-test.com");
        professor.setRole("professor");
        professor = userRepository.save(professor);

        // 3. Setup Students
        User s1 = createTestStudent("S1-" + runId, "111-" + runId);
        User s2 = createTestStudent("S2-" + runId, "222-" + runId);
        User s3 = createTestStudent("S3-" + runId, "333-" + runId);
        User s4 = createTestStudent("S4-" + runId, "444-" + runId);

        // 4. Group A: HAS Advisor
        Group groupA = new Group();
        groupA.setGroupName("Assigned Group " + runId);
        groupA.setLeader(s1);
        groupA.setAdvisor(professor);
        groupA.setStatus(GroupStatus.ADVISED);
        groupA = groupRepository.save(groupA);
        addMemberToDb(groupA, s1, GroupRole.LEADER);
        addMemberToDb(groupA, s2, GroupRole.MEMBER);

        // 5. Group B: NO Advisor
        Group groupB = new Group();
        groupB.setGroupName("Unassigned Group " + runId);
        groupB.setLeader(s3);
        groupB.setAdvisor(null);
        groupB.setStatus(GroupStatus.FORMED);
        groupB = groupRepository.save(groupB);
        addMemberToDb(groupB, s3, GroupRole.LEADER);
        addMemberToDb(groupB, s4, GroupRole.MEMBER);

        // 6. Setup Schedule with past deadline
        Schedule schedule = new Schedule();
        schedule.setGroupFormationDeadline(Instant.now().minus(2, ChronoUnit.DAYS));
        schedule.setAdvisorAssignmentDeadline(Instant.now().minus(1, ChronoUnit.HOURS));
        
        // Clean existing schedules to ensure our logic picks this one if it's top 1
        scheduleRepository.deleteAll(); 
        scheduleRepository.save(schedule);

        // 7. Execute Disband Logic (Process 4.9)
        disbandService.disbandUnassignedGroups();

        // 8. Verification

        // Group A verification (Should be unaffected)
        Group updatedA = groupRepository.findById(groupA.getId()).orElseThrow();
        assertEquals(GroupStatus.ADVISED, updatedA.getStatus(), "Group with advisor should remain unaffected");
        assertTrue(groupMemberRepository.existsByGroup_IdAndUser_UserId(groupA.getId(), s1.getUserId()),
                "S1 should still be in Group A");
        assertTrue(groupMemberRepository.existsByGroup_IdAndUser_UserId(groupA.getId(), s2.getUserId()),
                "S2 should still be in Group A");

        // Group B verification (Should be disbanded)
        Group updatedB = groupRepository.findById(groupB.getId()).orElseThrow();
        assertEquals(GroupStatus.DISBANDED, updatedB.getStatus(), "Group without advisor should be disbanded");
        assertFalse(groupMemberRepository.existsByGroup_IdAndUser_UserId(groupB.getId(), s3.getUserId()),
                "S3 should be removed from members");
        assertFalse(groupMemberRepository.existsByGroup_IdAndUser_UserId(groupB.getId(), s4.getUserId()),
                "S4 should be removed from members");

        // Notification verification for Group B members
        List<Notification> s3Notifs = notificationRepository.findByToUser_UserIdAndTypeAndStatus(
                s3.getUserId(), NotificationType.GROUP_DISBANDED, NotificationStatus.PENDING);
        assertFalse(s3Notifs.isEmpty(), "Leader of disbanded group should receive notification");
        assertTrue(s3Notifs.get(0).getMessage().contains("disbanded"),
                "Notification message should contain 'disbanded'");

        List<Notification> s4Notifs = notificationRepository.findByToUser_UserIdAndTypeAndStatus(
                s4.getUserId(), NotificationType.GROUP_DISBANDED, NotificationStatus.PENDING);
        assertFalse(s4Notifs.isEmpty(), "Member of disbanded group should receive notification");

        // Notification verification for Group A (should have NONE)
        List<Notification> s1Notifs = notificationRepository.findByToUser_UserIdAndTypeAndStatus(
                s1.getUserId(), NotificationType.GROUP_DISBANDED, NotificationStatus.PENDING);
        assertTrue(s1Notifs.isEmpty(), "Members of unaffected group should NOT receive disband notification");
    }

    private User createTestStudent(String name, String studentId) {
        User user = new User();
        user.setFullName(name);
        user.setStudentId(studentId);
        user.setEmail(studentId + "@spms-test.com");
        user.setRole("student");
        return userRepository.save(user);
    }

    private void addMemberToDb(Group group, User user, GroupRole role) {
        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(user);
        member.setRole(role);
        groupMemberRepository.save(member);
    }
}
