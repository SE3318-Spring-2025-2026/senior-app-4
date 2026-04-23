package com.spms.api;

import com.spms.backend.model.User;
import com.spms.backend.repository.GradingCriteriaRepository;
import com.spms.backend.repository.UserRepository;
import com.spms.backend.service.TokenService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * API tests for POST /api/v1/grading-criteria and GET /api/v1/grading-criteria.
 * Covers P3-CRITERIA-1: Coordinator-only create + filtered list.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GradingCriteriaApiTest extends BaseApiTest {

    private static final AtomicLong SEQ = new AtomicLong(System.currentTimeMillis());

    @Autowired private TokenService tokenService;
    @Autowired private UserRepository userRepository;
    @Autowired private GradingCriteriaRepository gradingCriteriaRepository;

    private String coordinatorToken;
    private String professorToken;
    private String studentToken;

    private Long coordinatorUserId;
    private Long professorUserId;
    private Long studentUserId;

    @BeforeAll
    void seedUsers() {
        User coordinator = createUser("coordinator");
        User professor   = createUser("professor");
        User student     = createUser("student");

        coordinatorUserId = coordinator.getUserId();
        professorUserId   = professor.getUserId();
        studentUserId     = student.getUserId();

        coordinatorToken = tokenService.generateToken(coordinator);
        professorToken   = tokenService.generateToken(professor);
        studentToken     = tokenService.generateToken(student);
    }

    @AfterAll
    void cleanupTestData() {
        gradingCriteriaRepository.deleteAll();

        deleteUserSafely(coordinatorUserId, "Coordinator");
        deleteUserSafely(professorUserId,   "Professor");
        deleteUserSafely(studentUserId,     "Student");
    }

    private void deleteUserSafely(Long userId, String label) {
        if (userId == null) return;
        userRepository.findByUserId(userId).ifPresentOrElse(
            u -> {
                userRepository.delete(u);
                System.out.println("  " + label + " user deleted: userId=" + userId);
            },
            () -> System.err.println("  [CLEANUP] " + label + " user not found: userId=" + userId)
        );
    }

    private User createUser(String role) {
        long seq = SEQ.incrementAndGet();
        User user = new User();
        user.setFullName("Test " + role + " " + seq);
        user.setEmail("criteria-" + role + "-" + seq + "@spms-test.com");
        user.setRole(role);
        user.setPasswordHash("pbkdf2$65536$dummySalt$dummyHash");
        user.setRequiresPasswordChange(false);
        user.setCreatedAt(Instant.now());
        if ("student".equals(role)) {
            user.setStudentId(String.format("99%09d", seq % 1_000_000_000));
        }
        return userRepository.save(user);
    }

    // ── POST /api/v1/grading-criteria ────────────────────────────────

    @Test
    @Order(1)
    void postAsCoordinator_returns201() {
        given()
            .header("Authorization", "Bearer " + coordinatorToken)
            .body(Map.of(
                "deliverableType", "PROPOSAL",
                "name", "Technical Feasibility",
                "description", "Evaluate technical feasibility",
                "weight", 30.0
            ))
        .when()
            .post("/api/v1/grading-criteria")
        .then()
            .statusCode(201)
            .body("status", equalTo("success"))
            .body("data.id", notNullValue())
            .body("data.deliverableType", equalTo("PROPOSAL"))
            .body("data.name", equalTo("Technical Feasibility"))
            .body("data.weight", equalTo(30.0f));
    }

    @Test
    @Order(2)
    void postAsProfessor_returns403() {
        given()
            .header("Authorization", "Bearer " + professorToken)
            .body(Map.of(
                "deliverableType", "PROPOSAL",
                "name", "Clarity",
                "weight", 20.0
            ))
        .when()
            .post("/api/v1/grading-criteria")
        .then()
            .statusCode(403);
    }

    @Test
    @Order(3)
    void postAsStudent_returns403() {
        given()
            .header("Authorization", "Bearer " + studentToken)
            .body(Map.of(
                "deliverableType", "PROPOSAL",
                "name", "Clarity",
                "weight", 20.0
            ))
        .when()
            .post("/api/v1/grading-criteria")
        .then()
            .statusCode(403);
    }

    @Test
    @Order(4)
    void postWithInvalidDeliverableType_returns400() {
        given()
            .header("Authorization", "Bearer " + coordinatorToken)
            .body(Map.of(
                "deliverableType", "BOGUS_TYPE",
                "name", "Some Criteria",
                "weight", 10.0
            ))
        .when()
            .post("/api/v1/grading-criteria")
        .then()
            .statusCode(400);
    }

    // ── GET /api/v1/grading-criteria ─────────────────────────────────

    @Test
    @Order(5)
    void getWithoutFilter_returns200WithList() {
        // Seed a SOW criterion so we have data across types
        given()
            .header("Authorization", "Bearer " + coordinatorToken)
            .body(Map.of(
                "deliverableType", "STATEMENT_OF_WORK",
                "name", "Implementation Plan",
                "weight", 40.0
            ))
        .when()
            .post("/api/v1/grading-criteria");

        given()
            .header("Authorization", "Bearer " + coordinatorToken)
        .when()
            .get("/api/v1/grading-criteria")
        .then()
            .statusCode(200)
            .body("status", equalTo("success"))
            .body("data", hasSize(greaterThanOrEqualTo(1)));
    }

    @Test
    @Order(6)
    void getWithDeliverableTypeFilter_returnsOnlyMatchingType() {
        given()
            .header("Authorization", "Bearer " + coordinatorToken)
            .queryParam("deliverableType", "PROPOSAL")
        .when()
            .get("/api/v1/grading-criteria")
        .then()
            .statusCode(200)
            .body("status", equalTo("success"))
            .body("data", everyItem(hasEntry("deliverableType", "PROPOSAL")));
    }
}
