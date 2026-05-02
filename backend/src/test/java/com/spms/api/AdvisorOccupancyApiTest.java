package com.spms.api;

import com.spms.backend.model.ActionType;
import com.spms.backend.model.AuditLog;
import com.spms.backend.model.Group;
import com.spms.backend.model.GroupStatus;
import com.spms.backend.model.User;
import com.spms.backend.model.notification.Notification;
import com.spms.backend.model.notification.NotificationStatus;
import com.spms.backend.model.notification.NotificationType;
import com.spms.backend.repository.AuditLogRepository;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.NotificationRepository;
import com.spms.backend.repository.UserRepository;
import com.spms.backend.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the spec rule:
 * "An advisor accepted a group and they are matched, the advisor must release
 *  the team first in order them to make another advisee request."
 *
 * Once a professor approves a group, they are occupied — APPROVE on a different
 * group must be rejected with 409, and any other groups' PENDING requests to that
 * same professor must be auto-rejected.
 */
@DisplayName("Process 4 - Advisor Occupancy Guard and Same-Professor Auto-Reject")
class AdvisorOccupancyApiTest extends BaseApiTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private TokenService tokenService;

    private User leaderA;
    private User leaderB;
    private User professor;

    private Group groupA;
    private Group groupB;

    private String professorToken;

    @BeforeEach
    void setupTwoGroupsOneProfessor() {
        leaderA = TestDataFactory.createStudent(userRepository, "Leader A",
                TestDataFactory.uniqueStudentId(), TestDataFactory.uniqueGithubUsername());
        leaderB = TestDataFactory.createStudent(userRepository, "Leader B",
                TestDataFactory.uniqueStudentId(), TestDataFactory.uniqueGithubUsername());

        professor = TestDataFactory.createProfessor(userRepository, "Occupied Professor",
                TestDataFactory.uniqueEmail());
        professorToken = TestDataFactory.mintToken(tokenService, professor);

        groupA = new Group();
        groupA.setGroupName("Group A " + System.nanoTime());
        groupA.setLeader(leaderA);
        groupA.setStatus(GroupStatus.FORMING);
        groupA = groupRepository.save(groupA);

        groupB = new Group();
        groupB.setGroupName("Group B " + System.nanoTime());
        groupB.setLeader(leaderB);
        groupB.setStatus(GroupStatus.FORMING);
        groupB = groupRepository.save(groupB);
    }

    @Test
    @DisplayName("Approving a request auto-rejects PENDING requests from other groups to the same professor")
    void approve_autoRejectsParallelRequestsToSameProfessor() {
        Notification reqA = seedAdvisorRequest(groupA, leaderA, professor);
        Notification reqB = seedAdvisorRequest(groupB, leaderB, professor);

        approve(reqA.getId()).statusCode(200);

        Notification updatedA = notificationRepository.findById(reqA.getId()).orElseThrow();
        assertEquals(NotificationStatus.ACCEPTED, updatedA.getStatus(),
                "Approved request must be ACCEPTED");

        Notification updatedB = notificationRepository.findById(reqB.getId()).orElseThrow();
        assertEquals(NotificationStatus.REJECTED, updatedB.getStatus(),
                "Other group's PENDING request to the same professor must be auto-rejected");

        Group persistedB = groupRepository.findById(groupB.getId()).orElseThrow();
        assertNull(persistedB.getAdvisor(), "Group B must remain unassigned");

        boolean leaderAlertExists = notificationRepository.findAll().stream()
                .anyMatch(n -> n.getType() == NotificationType.SYSTEM_ALERT
                        && n.getStatus() == NotificationStatus.PENDING
                        && groupB.getId().equals(n.getGroupId())
                        && n.getToUser() != null
                        && leaderB.getUserId().equals(n.getToUser().getUserId()));
        assertTrue(leaderAlertExists,
                "Group B's leader must receive a SYSTEM_ALERT explaining the auto-rejection");

        List<AuditLog> groupBLogs = auditLogRepository.findAll().stream()
                .filter(log -> groupB.getId().equals(log.getGroupId())
                        && log.getActionType() == ActionType.ADVISOR_REJECTED)
                .toList();
        assertEquals(1, groupBLogs.size(),
                "Exactly one ADVISOR_REJECTED audit log must be written for group B");
    }

    @Test
    @DisplayName("Approving a second group's request returns 409 when the professor already advises another group")
    void approve_blocksWhenProfessorAlreadyAdvisesAnotherGroup() {
        Notification reqA = seedAdvisorRequest(groupA, leaderA, professor);
        approve(reqA.getId()).statusCode(200);

        Notification reqB = seedAdvisorRequest(groupB, leaderB, professor);

        approve(reqB.getId()).statusCode(409);

        Notification persistedB = notificationRepository.findById(reqB.getId()).orElseThrow();
        assertEquals(NotificationStatus.PENDING, persistedB.getStatus(),
                "Blocked request must remain PENDING");

        Group groupBPersisted = groupRepository.findById(groupB.getId()).orElseThrow();
        assertNull(groupBPersisted.getAdvisor(),
                "Group B must not be assigned to an already-occupied professor");
    }

    private Notification seedAdvisorRequest(Group group, User leader, User toProfessor) {
        Notification n = new Notification();
        n.setType(NotificationType.ADVISOR_REQUEST);
        n.setStatus(NotificationStatus.PENDING);
        n.setGroupId(group.getId());
        n.setFromUser(leader);
        n.setToUser(toProfessor);
        return notificationRepository.save(n);
    }

    private io.restassured.response.ValidatableResponse approve(Long requestId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("decision", "APPROVE");
        return given()
                .header("Authorization", "Bearer " + professorToken)
                .body(body)
                .when()
                .post("/api/v1/advisor-requests/{requestId}/decision", requestId)
                .then();
    }
}
