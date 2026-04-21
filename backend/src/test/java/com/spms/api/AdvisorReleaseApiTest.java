package com.spms.api;

import com.spms.backend.model.Group;
import com.spms.backend.model.GroupStatus;
import com.spms.backend.model.User;
import com.spms.backend.repository.AuditLogRepository;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.UserRepository;
import com.spms.backend.repository.ValidStudentIdRepository;
import com.spms.backend.service.TokenService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.atomic.AtomicLong;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end API tests for Process 4.10 — Professor Release Advisee Group.
 *
 * <h3>Scope (Issue #84)</h3>
 * <ul>
 *     <li>Happy path: advisor releases their group → 200, advisor cleared, audit log written.</li>
 *     <li>Different professor tries to release → 403.</li>
 *     <li>Group has no advisor → 400.</li>
 *     <li>Student tries to release → 403.</li>
 *     <li>Group does not exist → 404.</li>
 * </ul>
 *
 * <p>Users and a single group are seeded directly via repositories in {@code @BeforeAll};
 * each test resets the advisor field on that group before calling the endpoint. This
 * sidesteps the lack of an assign-advisor API on main (the approval/override paths are
 * the subject of separate Process 4 issues).</p>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AdvisorReleaseApiTest extends BaseApiTest {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ValidStudentIdRepository validStudentIdRepository;

    private static final AtomicLong SEQ = new AtomicLong(System.currentTimeMillis());

    private String leaderToken;
    private String profAToken;
    private String profBToken;

    private Long leaderUserId;
    private Long profAUserId;
    private Long profBUserId;
    private String leaderStudentId;

    private Long groupId;

    @BeforeAll
    void seedUsersAndGroup() {
        leaderStudentId = TestDataFactory.uniqueStudentId();
        String profAEmail = TestDataFactory.uniqueEmail();
        String profBEmail = TestDataFactory.uniqueEmail();

        TestDataFactory.seedStudentId(validStudentIdRepository, leaderStudentId);

        User leader = TestDataFactory.createStudent(userRepository, "Release Leader",
                leaderStudentId, "gh-rel-" + SEQ.get());
        User profA = TestDataFactory.createProfessor(userRepository, "Prof Release A", profAEmail);
        User profB = TestDataFactory.createProfessor(userRepository, "Prof Release B", profBEmail);

        leaderUserId = leader.getUserId();
        profAUserId = profA.getUserId();
        profBUserId = profB.getUserId();

        leaderToken = TestDataFactory.mintToken(tokenService, leader);
        profAToken = TestDataFactory.mintToken(tokenService, profA);
        profBToken = TestDataFactory.mintToken(tokenService, profB);

        // Seed the group directly via the repository. RestAssured.port is wired in
        // @BeforeEach, so API calls are not yet possible here; also the assign-advisor
        // path (Process 4) is not on main, so direct repo writes are the only way
        // to reach the state this suite needs to exercise.
        Group group = new Group();
        group.setGroupName("Release-Test-" + SEQ.incrementAndGet());
        group.setLeader(leader);
        group.setStatus(GroupStatus.FORMING);
        Group saved = groupRepository.save(group);
        groupId = saved.getId();
        assertNotNull(groupId, "seeded groupId must be non-null");
    }

    @AfterAll
    void cleanup() {
        if (groupId != null) {
            try {
                groupRepository.deleteById(groupId);
            } catch (Exception ignored) {
            }
        }
        deleteUserSafely(leaderUserId);
        deleteUserSafely(profAUserId);
        deleteUserSafely(profBUserId);
        if (leaderStudentId != null) {
            try {
                validStudentIdRepository.deleteByStudentId(leaderStudentId);
            } catch (Exception ignored) {
            }
        }
    }

    @Test
    @Order(1)
    @DisplayName("POST release → 200 when the assigned advisor releases their own group")
    void release_happyPath_clearsAdvisorAndLogs() {
        setAdvisor(profAUserId);

        given()
            .header("Authorization", "Bearer " + profAToken)
        .when()
            .post("/api/v1/advisor-assignments/" + groupId + "/release")
        .then()
            .statusCode(200)
            .body("status", equalTo("success"));

        Group reloaded = groupRepository.findById(groupId).orElseThrow();
        assertNull(reloaded.getAdvisor(), "advisor should be cleared after release");

        boolean hasAuditLog = auditLogRepository.findAll().stream()
                .anyMatch(log -> "ADVISOR_RELEASED".equals(log.getAction())
                        && log.getEntityId() != null && log.getEntityId().equals(groupId)
                        && log.getUserId() != null && log.getUserId().equals(profAUserId));
        assertTrue(hasAuditLog, "AuditLog entry for ADVISOR_RELEASED should exist");

        given()
            .header("Authorization", "Bearer " + leaderToken)
        .when()
            .get("/api/v1/groups/" + groupId)
        .then()
            .statusCode(200)
            .body("advisorId", nullValue());
    }

    @Test
    @Order(2)
    @DisplayName("POST release → 403 when a different professor tries to release")
    void release_differentProfessor_returns403() {
        setAdvisor(profAUserId);

        given()
            .header("Authorization", "Bearer " + profBToken)
        .when()
            .post("/api/v1/advisor-assignments/" + groupId + "/release")
        .then()
            .statusCode(403);

        Group reloaded = groupRepository.findById(groupId).orElseThrow();
        assertNotNull(reloaded.getAdvisor(), "advisor must remain assigned on 403");
        assertEquals(profAUserId, reloaded.getAdvisor().getUserId());
    }

    @Test
    @Order(3)
    @DisplayName("POST release → 400 when group has no advisor")
    void release_noAdvisor_returns400() {
        setAdvisor(null);

        given()
            .header("Authorization", "Bearer " + profAToken)
        .when()
            .post("/api/v1/advisor-assignments/" + groupId + "/release")
        .then()
            .statusCode(400);
    }

    @Test
    @Order(4)
    @DisplayName("POST release → 403 when a student (non-professor) calls the endpoint")
    void release_studentRole_returns403() {
        setAdvisor(profAUserId);

        given()
            .header("Authorization", "Bearer " + leaderToken)
        .when()
            .post("/api/v1/advisor-assignments/" + groupId + "/release")
        .then()
            .statusCode(403);

        Group reloaded = groupRepository.findById(groupId).orElseThrow();
        assertNotNull(reloaded.getAdvisor(), "advisor must remain assigned on 403");
    }

    @Test
    @Order(5)
    @DisplayName("POST release → 404 when group does not exist")
    void release_groupMissing_returns404() {
        long bogusGroupId = 9_000_000_000L;

        given()
            .header("Authorization", "Bearer " + profAToken)
        .when()
            .post("/api/v1/advisor-assignments/" + bogusGroupId + "/release")
        .then()
            .statusCode(404);
    }

    // ─── Helpers ──────────────────────────────────────────────────────

    /**
     * Directly sets or clears the group's advisor via repository.
     * The assign-advisor path (Process 4 approval or Coordinator override) is not
     * yet on main, so test setup bypasses it to exercise only the release flow.
     */
    private void setAdvisor(Long advisorUserId) {
        Group group = groupRepository.findById(groupId).orElseThrow();
        if (advisorUserId == null) {
            group.setAdvisor(null);
        } else {
            User advisor = userRepository.findById(advisorUserId).orElseThrow();
            group.setAdvisor(advisor);
        }
        groupRepository.save(group);
    }

    private void deleteUserSafely(Long userId) {
        if (userId == null) return;
        try {
            userRepository.findByUserId(userId).ifPresent(userRepository::delete);
        } catch (Exception ignored) {
        }
    }
}
