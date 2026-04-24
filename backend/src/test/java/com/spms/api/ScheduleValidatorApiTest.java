package com.spms.api;

import com.spms.backend.model.Group;
import com.spms.backend.model.GroupStatus;
import com.spms.backend.model.Schedule;
import com.spms.backend.model.User;
import com.spms.backend.repository.GroupMemberRepository;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.ScheduleRepository;
import com.spms.backend.repository.UserRepository;
import com.spms.backend.service.NotificationService;
import com.spms.backend.service.ScheduleValidatorService;
import com.spms.backend.service.TokenService;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * P2.7 — Schedule Validator & Coordinator API Tests
 * Covers Issue #21, #17, #35
 */
public class ScheduleValidatorApiTest extends BaseApiTest {

    @MockBean
    private ScheduleRepository scheduleRepository;

    @MockBean
    private GroupRepository groupRepository;

    @MockBean
    private GroupMemberRepository groupMemberRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private NotificationService notificationService;

    @Autowired
    private ScheduleValidatorService validatorService;

    @Autowired
    private TokenService tokenService;

    private String coordinatorToken;
    private final Long coordinatorId = 999L;

    @BeforeEach
    void setup() {
        // Create a coordinator user
        User coord = new User();
        coord.setUserId(coordinatorId);
        coord.setRole("coordinator");

        coordinatorToken = tokenService.generateToken(coord);

        // Common mock for findCoordinatorId helper
        when(userRepository.findAllByRole("coordinator")).thenReturn(List.of(coord));
    }

    // ── API TESTS: GET /schedule

    @Test
    @DisplayName("GET /schedule → 200 OK with data")
    void getSchedule_exists_returns200() {
        Schedule s = new Schedule();
        s.setId(1L);
        s.setGroupFormationDeadline(Instant.now().plus(7, ChronoUnit.DAYS));
        s.setAdvisorAssignmentDeadline(Instant.now().plus(14, ChronoUnit.DAYS));

        when(scheduleRepository.findTopByOrderByIdDesc()).thenReturn(Optional.of(s));

        RestAssured.given()
                .header("Authorization", "Bearer " + coordinatorToken)
                .when()
                .get("/api/v1/coordinator/schedule")
                .then()
                .statusCode(200)
                .body("groupFormationDeadline", notNullValue())
                .body("advisorAssignmentDeadline", notNullValue());
    }

    @Test
    @DisplayName("GET /schedule → 404 if not initialized")
    void getSchedule_notExists_returns404() {
        when(scheduleRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());

        RestAssured.given()
                .header("Authorization", "Bearer " + coordinatorToken)
                .when()
                .get("/api/v1/coordinator/schedule")
                .then()
                .statusCode(404);
    }

    // ── API TESTS: PUT /schedule

    @Test
    @DisplayName("PUT /schedule → 200 OK for future dates")
    void updateSchedule_futureDates_returns200() {
        Instant future1 = Instant.now().plus(10, ChronoUnit.DAYS);
        Instant future2 = Instant.now().plus(20, ChronoUnit.DAYS);

        when(scheduleRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        when(scheduleRepository.save(Mockito.any(Schedule.class))).thenAnswer(i -> i.getArguments()[0]);

        RestAssured.given()
                .header("Authorization", "Bearer " + coordinatorToken)
                .body(Map.of(
                        "groupFormationDeadline", future1.toString(),
                        "advisorAssignmentDeadline", future2.toString()))
                .when()
                .put("/api/v1/coordinator/schedule")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("PUT /schedule → 400 Bad Request for past dates")
    void updateSchedule_pastDates_returns400() {
        Instant past = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant future = Instant.now().plus(10, ChronoUnit.DAYS);

        RestAssured.given()
                .header("Authorization", "Bearer " + coordinatorToken)
                .body(Map.of(
                        "groupFormationDeadline", past.toString(),
                        "advisorAssignmentDeadline", future.toString()))
                .when()
                .put("/api/v1/coordinator/schedule")
                .then()
                .statusCode(400)
                .body("message", containsStringIgnoringCase("future"));
    }

    @Test
    @DisplayName("PUT /schedule → 403 Forbidden for students")
    void updateSchedule_studentRole_returns403() {
        User student = new User();
        student.setUserId(2L);
        student.setRole("student");
        String studentToken = tokenService.generateToken(student);

        RestAssured.given()
                .header("Authorization", "Bearer " + studentToken)
                .body(Map.of(
                        "groupFormationDeadline", Instant.now().plus(10, ChronoUnit.DAYS).toString(),
                        "advisorAssignmentDeadline", Instant.now().plus(20, ChronoUnit.DAYS).toString()))
                .when()
                .put("/api/v1/coordinator/schedule")
                .then()
                .statusCode(403);
    }

    // ── BACKGROUND LOGIC TESTS: ScheduleValidatorService

    @Test
    @DisplayName("Validator: Formation deadline passed → Create Alert & Flag Groups")
    void validator_formationDeadlinePassed_triggersActions() {
        // 1. Setup expired formation deadline, future advisor deadline
        Schedule s = new Schedule();
        s.setGroupFormationDeadline(Instant.now().minus(1, ChronoUnit.HOURS));
        s.setAdvisorAssignmentDeadline(Instant.now().plus(1, ChronoUnit.DAYS));
        when(scheduleRepository.findTopByOrderByIdDesc()).thenReturn(Optional.of(s));

        // 2. Setup incomplete group
        Group g = new Group();
        g.setId(1L);
        g.setGroupName("Incomplete Group");
        g.setStatus(GroupStatus.FORMED);
        g.setMembers(Collections.emptyList());

        when(groupRepository.findByStatus(GroupStatus.FORMED)).thenReturn(List.of(g));
        when(notificationService.systemAlertExists(eq(coordinatorId), anyString())).thenReturn(false);

        // 3. Trigger validator
        validatorService.checkDeadlines();

        // 4. Verify system alert created
        verify(notificationService, atLeastOnce()).createSystemAlert(
                eq(coordinatorId),
                argThat(msg -> msg.toLowerCase().contains("formation deadline")),
                eq("formation_deadline_missed"),
                isNull());

        // 5. Verify group status updated
        verify(groupRepository, atLeastOnce()).save(Mockito.argThat(group -> group.getStatus() == GroupStatus.FORMING));
    }

    @Test
    @DisplayName("Validator: Advisor deadline passed → Create Alert & Flag Unadvised Groups")
    void validator_advisorDeadlinePassed_triggersActions() {
        // 1. Setup expired advisor deadline (formation deadline must be before advisor
        // deadline)
        Schedule s = new Schedule();
        s.setGroupFormationDeadline(Instant.now().minus(2, ChronoUnit.DAYS));
        s.setAdvisorAssignmentDeadline(Instant.now().minus(1, ChronoUnit.HOURS));
        when(scheduleRepository.findTopByOrderByIdDesc()).thenReturn(Optional.of(s));

        // 2. Setup unadvised group
        Group g = new Group();
        g.setId(2L);
        g.setGroupName("Unadvised Group");
        g.setAdvisor(null);
        g.setStatus(GroupStatus.FORMING);

        when(groupRepository.findByAdvisorIsNullAndStatusNot(GroupStatus.DISBANDED)).thenReturn(List.of(g));
        when(notificationService.systemAlertExists(eq(coordinatorId), anyString())).thenReturn(false);

        // 3. Trigger validator
        validatorService.checkDeadlines();

        // 4. Verify alert
        verify(notificationService, atLeastOnce()).createSystemAlert(
                eq(coordinatorId),
                argThat(msg -> msg.toLowerCase().contains("advisor assignment")),
                eq("advisor_deadline_missed"),
                isNull());
    }
}
