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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Process 4 - Test Coordinator Override Side Effects")
class CoordinatorOverrideApiTest extends BaseApiTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private TokenService tokenService;

    private User coordinator;
    private User oldAdvisor;
    private User newAdvisor;
    private User groupLeader;
    private Group group;

    private String coordinatorToken;

    @BeforeEach
    void setupTestData() {
        // 1. Create Coordinator (Inline creation as TestDataFactory.createCoordinator does not exist)
        coordinator = new User();
        coordinator.setFullName("Coordinator Admin");
        coordinator.setEmail("coord-admin-" + System.currentTimeMillis() + "@spms-test.com");
        coordinator.setRole("coordinator");
        coordinator.setCreatedAt(Instant.now());
        coordinator = userRepository.save(coordinator);

        coordinatorToken = TestDataFactory.mintToken(tokenService, coordinator);

        // 2. Create Advisors with initial currentAdviseeCount
        oldAdvisor = TestDataFactory.createProfessor(userRepository, "Old Advisor", TestDataFactory.uniqueEmail());
        oldAdvisor.setCurrentAdviseeCount(1);
        oldAdvisor = userRepository.save(oldAdvisor);

        newAdvisor = TestDataFactory.createProfessor(userRepository, "New Advisor", TestDataFactory.uniqueEmail());
        newAdvisor.setCurrentAdviseeCount(0);
        newAdvisor = userRepository.save(newAdvisor);

        groupLeader = TestDataFactory.createStudent(userRepository, "Group Leader", 
                TestDataFactory.uniqueStudentId(), TestDataFactory.uniqueGithubUsername());

        // 3. Create Group and assign to oldAdvisor
        group = new Group();
        group.setGroupName("State Cleanup Test Group " + System.currentTimeMillis());
        group.setLeader(groupLeader);
        group.setAdvisor(oldAdvisor);
        group.setStatus(GroupStatus.ADVISED);
        group = groupRepository.save(group);
    }

    @Test
    @DisplayName("Should successfully override advisor and verify all Issue #169 side effects")
    void shouldOverrideAdvisorAndVerifySideEffects() {
        // 1. Prepare Request Payload
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("teamId", group.getId());
        payload.put("advisorId", newAdvisor.getUserId());
        payload.put("reason", "Coordinator override for Issue #169");

        // 2. Perform Override endpoint call
        given()
                .header("Authorization", "Bearer " + coordinatorToken)
                .body(payload)
                .when()
                .post("/api/v1/advisor-assignments/override")
                .then()
                .statusCode(200)
                .body("status", equalTo("success"))
                .body("data.newAdvisorId", equalTo(newAdvisor.getUserId().intValue()));

        // 3. Verify Group advisor is updated to the new advisor
        Group updatedGroup = groupRepository.findById(group.getId()).orElseThrow();
        assertEquals(newAdvisor.getUserId(), updatedGroup.getAdvisor().getUserId(), 
                "Group advisor should be the new professor after override");

        // 4. Verify advisor workload counts (currentAdviseeCount)
        User oldAdvisorUpdated = userRepository.findById(oldAdvisor.getUserId()).orElseThrow();
        User newAdvisorUpdated = userRepository.findById(newAdvisor.getUserId()).orElseThrow();

        assertEquals(0, oldAdvisorUpdated.getCurrentAdviseeCount(), 
                "Old advisor currentAdviseeCount should decrement to 0");
        assertEquals(1, newAdvisorUpdated.getCurrentAdviseeCount(), 
                "New advisor currentAdviseeCount should increment to 1");

        // 5. Verify D9 Audit Log
        Page<AuditLog> logPage = auditLogRepository.findByGroupId(group.getId(), Pageable.unpaged());
        List<AuditLog> logs = logPage.getContent();
        
        boolean auditLogExists = logs.stream().anyMatch(log ->
                log.getActionType() == ActionType.ADVISOR_OVERRIDDEN &&
                log.getUserId().equals(coordinator.getUserId())
        );
        assertTrue(auditLogExists, "D9 audit log should be written with ADVISOR_OVERRIDDEN and coordinator userId");

        // 6. Verify D8 Notifications (Strengthened Structural Assertions)
        List<Notification> allNotifs = notificationRepository.findAll().stream()
                .filter(n -> group.getId().equals(n.getGroupId()))
                .toList();

        // New advisor notification
        boolean newAdvisorNotified = allNotifs.stream().anyMatch(n ->
                n.getToUser().getUserId().equals(newAdvisor.getUserId()) &&
                n.getType() == NotificationType.SYSTEM_ALERT &&
                n.getStatus() == NotificationStatus.PENDING &&
                group.getId().equals(n.getGroupId()) &&
                (n.getFromUser() != null && n.getFromUser().getUserId().equals(coordinator.getUserId()))
        );
        assertTrue(newAdvisorNotified, "New advisor should receive a PENDING SYSTEM_ALERT notification from the coordinator");

        // Old advisor notification
        boolean oldAdvisorNotified = allNotifs.stream().anyMatch(n ->
                n.getToUser().getUserId().equals(oldAdvisor.getUserId()) &&
                n.getType() == NotificationType.SYSTEM_ALERT &&
                n.getStatus() == NotificationStatus.PENDING &&
                group.getId().equals(n.getGroupId()) &&
                (n.getFromUser() != null && n.getFromUser().getUserId().equals(coordinator.getUserId()))
        );
        assertTrue(oldAdvisorNotified, "Old advisor should receive a PENDING SYSTEM_ALERT notification from the coordinator");
    }
}
