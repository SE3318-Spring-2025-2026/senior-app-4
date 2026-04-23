package com.spms.api;

import com.spms.backend.model.Group;
import com.spms.backend.model.GroupStatus;
import com.spms.backend.model.User;
import com.spms.backend.model.notification.Notification;
import com.spms.backend.model.notification.NotificationStatus;
import com.spms.backend.model.notification.NotificationType;
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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.either;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Process 4 - Test Advisor Approval Auto Reject and Limits")
class AdvisorApprovalAutoRejectApiTest extends BaseApiTest {

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private GroupRepository groupRepository;

        @Autowired
        private NotificationRepository notificationRepository;

        @Autowired
        private TokenService tokenService;

        private User groupLeader;
        private User professor1;
        private User professor2;
        private User professor3;
        private Group group;

        private String leaderToken;
        private String prof1Token;

        @BeforeEach
        void setupTestData() {
                groupLeader = TestDataFactory.createStudent(userRepository, "Leader Student",
                                TestDataFactory.uniqueStudentId(), TestDataFactory.uniqueGithubUsername());

                professor1 = TestDataFactory.createProfessor(userRepository, "Professor One",
                                TestDataFactory.uniqueEmail());
                professor2 = TestDataFactory.createProfessor(userRepository, "Professor Two",
                                TestDataFactory.uniqueEmail());
                professor3 = TestDataFactory.createProfessor(userRepository, "Professor Three",
                                TestDataFactory.uniqueEmail());

                leaderToken = TestDataFactory.mintToken(tokenService, groupLeader);
                prof1Token = TestDataFactory.mintToken(tokenService, professor1);

                group = new Group();
                group.setGroupName("Auto Reject Test Group " + System.currentTimeMillis());
                group.setLeader(groupLeader);
                group.setStatus(GroupStatus.FORMING);
                group = groupRepository.save(group);
        }

        @Test
        @DisplayName("Should return 400 or 429 when exceeding allowed number of active pending advisor requests")
        void shouldReturnErrorWhenSpammingRequests() {
                // 1. Send first valid request
                Map<String, Object> req1 = new LinkedHashMap<>();
                req1.put("professorId", professor1.getUserId());

                given()
                                .header("Authorization", "Bearer " + leaderToken)
                                .body(req1)
                                .when()
                                .post("/api/v1/groups/{groupId}/advisor-request", group.getId())
                                .then()
                                .statusCode(201)
                                .body("success", equalTo(true));

                // 2. Send second request which should fail (spam limit / single pending request
                // limit)
                Map<String, Object> req2 = new LinkedHashMap<>();
                req2.put("professorId", professor2.getUserId());

                given()
                                .header("Authorization", "Bearer " + leaderToken)
                                .body(req2)
                                .when()
                                .post("/api/v1/groups/{groupId}/advisor-request", group.getId())
                                .then()
                                .statusCode(either(equalTo(400)).or(equalTo(429))); // Accepts 400 or 429 based on
                                                                                    // branch implementation
        }

        @Test
        @DisplayName("Should auto-reject parallel requests and write D8 cancellation notifications when one is approved")
        void shouldAutoRejectParallelRequests() {
                // Since API might have a strict limit (e.g. 1) we seed the DB directly with
                // multiple pending requests
                // to test the auto-reject mechanism correctly which handles multiple requests
                // resolving
                Notification n1 = new Notification();
                n1.setType(NotificationType.ADVISOR_REQUEST);
                n1.setStatus(NotificationStatus.PENDING);
                n1.setGroupId(group.getId());
                n1.setFromUser(groupLeader);
                n1.setToUser(professor1);
                n1 = notificationRepository.save(n1);

                Notification n2 = new Notification();
                n2.setType(NotificationType.ADVISOR_REQUEST);
                n2.setStatus(NotificationStatus.PENDING);
                n2.setGroupId(group.getId());
                n2.setFromUser(groupLeader);
                n2.setToUser(professor2);
                n2 = notificationRepository.save(n2);

                Notification n3 = new Notification();
                n3.setType(NotificationType.ADVISOR_REQUEST);
                n3.setStatus(NotificationStatus.PENDING);
                n3.setGroupId(group.getId());
                n3.setFromUser(groupLeader);
                n3.setToUser(professor3);
                n3 = notificationRepository.save(n3);

                // Act: Professor 1 approves the request
                Map<String, Object> decisionReq = new LinkedHashMap<>();
                decisionReq.put("decision", "APPROVE");

                given()
                                .header("Authorization", "Bearer " + prof1Token)
                                .body(decisionReq)
                                .when()
                                .post("/api/v1/advisor-requests/{requestId}/decision", n1.getId())
                                .then()
                                .statusCode(200);

                // Assert: Group advisor is set
                Group updatedGroup = groupRepository.findById(group.getId()).orElseThrow();
                assertEquals(professor1.getUserId(), updatedGroup.getAdvisor().getUserId(),
                                "Group advisor should be Professor 1");

                // Assert: Approved request is ACCEPTED
                Notification updatedN1 = notificationRepository.findById(n1.getId()).orElseThrow();
                assertEquals(NotificationStatus.ACCEPTED, updatedN1.getStatus(), "Approved request should be ACCEPTED");

                // Assert: Other parallel requests are auto-REJECTED
                Notification updatedN2 = notificationRepository.findById(n2.getId()).orElseThrow();
                assertEquals(NotificationStatus.REJECTED, updatedN2.getStatus(),
                                "Parallel request 2 should be REJECTED");

                Notification updatedN3 = notificationRepository.findById(n3.getId()).orElseThrow();
                assertEquals(NotificationStatus.REJECTED, updatedN3.getStatus(),
                                "Parallel request 3 should be REJECTED");

                // Assert: D8 Cancellation notifications are created as SYSTEM_ALERT
                List<Notification> alerts = notificationRepository.findAll().stream()
                                .filter(n -> n.getGroupId() != null && n.getGroupId().equals(group.getId())
                                                && n.getType() == NotificationType.SYSTEM_ALERT)
                                .toList();

                boolean cancelledAlertForProf2 = alerts.stream()
                                .anyMatch(n -> n.getToUser().getUserId().equals(professor2.getUserId())
                                                && n.getMessage().contains("cancelled"));
                boolean cancelledAlertForProf3 = alerts.stream()
                                .anyMatch(n -> n.getToUser().getUserId().equals(professor3.getUserId())
                                                && n.getMessage().contains("cancelled"));

                assertTrue(cancelledAlertForProf2, "Professor 2 should receive a cancellation SYSTEM_ALERT");
                assertTrue(cancelledAlertForProf3, "Professor 3 should receive a cancellation SYSTEM_ALERT");
        }
}
