package com.spms.api;

import com.spms.backend.model.Committee;
import com.spms.backend.model.CommitteeAdvisor;
import com.spms.backend.model.CommitteeJury;
import com.spms.backend.model.Group;
import com.spms.backend.model.GroupCommitteeAssignment;
import com.spms.backend.model.Submission;
import com.spms.backend.model.SubmissionGrade;
import com.spms.backend.model.User;
import com.spms.backend.model.enums.CommitteeStatus;
import com.spms.backend.model.enums.DeliverableType;
import com.spms.backend.model.enums.SubmissionStatus;
import com.spms.backend.model.notification.Notification;
import com.spms.backend.model.notification.NotificationType;
import com.spms.backend.repository.NotificationRepository;
import com.spms.backend.repository.SubmissionRepository;
import com.spms.backend.repository.UserRepository;
import com.spms.backend.service.TokenService;
import io.restassured.response.Response;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Multi-state transition test suite for grading endpoints (Issue #167).
 *
 * <p>Pins the spec contract (P3-Submissions-api.yaml § 3.5) for:
 * <ul>
 *   <li>{@code isGradingComplete} stays false until ALL committee members have graded.</li>
 *   <li>The final {@code averageGrade} is computed only when grading is complete.</li>
 *   <li>Students cannot view {@code averageGrade} while grading is incomplete.</li>
 *   <li>Spec side-effects: notifications to coordinator and group leader on completion.</li>
 * </ul>
 *
 * <p>Committee size = 4 (2 advisors + 2 jury) — chosen deliberately to differ from
 * the hardcoded fallback of 3 in {@code SubmissionGradeService.getSubmissionGrades},
 * so this suite exercises the spec-correct path that derives totalCommitteeMembers
 * from the actual committee composition.
 *
 * <p>Known §9 limitation: the NotificationType enum currently has no GRADING_COMPLETE
 * value — all grading-complete notifications are persisted as type=SYSTEM_ALERT with
 * the discriminator embedded in the message. Notification assertions therefore combine
 * a structural check (type=SYSTEM_ALERT, recipient userId) with a message substring
 * check ("GRADING_COMPLETE"). A follow-up issue should add the enum value and update
 * NotificationServiceImpl.createSystemAlert to set the typed discriminator structurally.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SubmissionGradingMultiStateApiTest extends BaseApiTest {

    @Autowired private TokenService tokenService;
    @Autowired private UserRepository userRepository;
    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private PlatformTransactionManager txManager;

    @PersistenceContext
    private EntityManager em;

    private TransactionTemplate tx;

    private Long coordinatorId;
    private Long leaderId;
    private String leaderToken;

    private Long advisor1Id;
    private Long advisor2Id;
    private Long jury1Id;
    private Long jury2Id;

    private String advisor1Token;
    private String jury1Token;
    private String jury2Token;

    private Long groupId;
    private Long committeeId;

    private Long currentSubmissionId;

    @BeforeAll
    void seedSharedFixture() {
        tx = new TransactionTemplate(txManager);

        User coordinator = createCoordinator("Coord-167", TestDataFactory.uniqueEmail());
        coordinatorId = coordinator.getUserId();

        User advisor1 = TestDataFactory.createProfessor(userRepository, "Advisor1-167", TestDataFactory.uniqueEmail());
        User advisor2 = TestDataFactory.createProfessor(userRepository, "Advisor2-167", TestDataFactory.uniqueEmail());
        User jury1    = TestDataFactory.createProfessor(userRepository, "Jury1-167",    TestDataFactory.uniqueEmail());
        User jury2    = TestDataFactory.createProfessor(userRepository, "Jury2-167",    TestDataFactory.uniqueEmail());
        advisor1Id = advisor1.getUserId();
        advisor2Id = advisor2.getUserId();
        jury1Id    = jury1.getUserId();
        jury2Id    = jury2.getUserId();
        advisor1Token = TestDataFactory.mintToken(tokenService, advisor1);
        jury1Token    = TestDataFactory.mintToken(tokenService, jury1);
        jury2Token    = TestDataFactory.mintToken(tokenService, jury2);

        User leader = TestDataFactory.createStudent(
                userRepository,
                "Leader-167",
                TestDataFactory.uniqueStudentId(),
                TestDataFactory.uniqueGithubUsername());
        leaderId = leader.getUserId();
        leaderToken = TestDataFactory.mintToken(tokenService, leader);

        // Group, Committee, members, and the GroupCommitteeAssignment are persisted
        // in one transaction so foreign keys resolve. Committee has no JpaRepository
        // in the codebase, so EntityManager is used directly.
        tx.executeWithoutResult(status -> {
            Group g = new Group();
            g.setGroupName("Group-167-" + System.nanoTime());
            g.setLeader(em.find(User.class, leaderId));
            em.persist(g);

            Committee c = new Committee("Committee-167", "issue-167 fixture", CommitteeStatus.ACTIVE, coordinatorId);
            em.persist(c);

            em.persist(new CommitteeAdvisor(c, em.find(User.class, advisor1Id), "MEMBER", coordinatorId));
            em.persist(new CommitteeAdvisor(c, em.find(User.class, advisor2Id), "MEMBER", coordinatorId));
            em.persist(new CommitteeJury(c, em.find(User.class, jury1Id), "INTERNAL", coordinatorId));
            em.persist(new CommitteeJury(c, em.find(User.class, jury2Id), "INTERNAL", coordinatorId));

            em.persist(new GroupCommitteeAssignment(c, g.getId(), "ASSIGNED", coordinatorId));

            this.groupId = g.getId();
            this.committeeId = c.getCommitteeId();
        });
    }

    /**
     * Fresh Submission per test — the service flips submission.status to GRADED on the
     * final grade, so reusing the same submission would leak state across tests.
     */
    @BeforeEach
    void freshSubmission() {
        currentSubmissionId = tx.execute(status -> {
            Submission s = new Submission();
            s.setGroupId(groupId);
            s.setDeliverableType(DeliverableType.PROPOSAL);
            s.setContent("issue-167 grading fixture");
            s.setStatus(SubmissionStatus.APPROVED);
            s.setCommitteeId(committeeId);
            em.persist(s);
            return s.getId();
        });
    }

    // ─── AC #1 + AC #3 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /grades (student, 0 grades) -> isGradingComplete=false, averageGrade=null")
    void getGrades_zeroGrades_studentSeesIncompleteAndNullAverage() {
        given()
            .header("Authorization", "Bearer " + leaderToken)
        .when()
            .get("/api/v1/submissions/" + currentSubmissionId + "/grades")
        .then()
            .statusCode(200)
            .body("status", equalTo("success"))
            .body("data.isGradingComplete", is(false))
            .body("data.averageGrade", nullValue())
            .body("data.gradeCount", equalTo(0))
            .body("data.totalCommitteeMembers", equalTo(4));
    }

    @Test
    @DisplayName("GET /grades (student, 1 of 4 graded) -> still incomplete, averageGrade still null")
    void getGrades_oneGrade_studentStillSeesNullAverage() {
        seedGrade(advisor1Id, 70.0);

        given()
            .header("Authorization", "Bearer " + leaderToken)
        .when()
            .get("/api/v1/submissions/" + currentSubmissionId + "/grades")
        .then()
            .statusCode(200)
            .body("data.isGradingComplete", is(false))
            .body("data.averageGrade", nullValue())
            .body("data.gradeCount", equalTo(1))
            .body("data.totalCommitteeMembers", equalTo(4));
    }

    @Test
    @DisplayName("GET /grades (student, 3 of 4 graded) -> still incomplete, averageGrade still null")
    void getGrades_threeGrades_studentStillSeesNullAverage() {
        seedGrade(advisor1Id, 70.0);
        seedGrade(advisor2Id, 85.0);
        seedGrade(jury1Id,    90.0);

        given()
            .header("Authorization", "Bearer " + leaderToken)
        .when()
            .get("/api/v1/submissions/" + currentSubmissionId + "/grades")
        .then()
            .statusCode(200)
            .body("data.isGradingComplete", is(false))
            .body("data.averageGrade", nullValue())
            .body("data.gradeCount", equalTo(3))
            .body("data.totalCommitteeMembers", equalTo(4));
    }

    // ─── AC #1 (response side) + spec § 3.5 (no premature side-effects) ────

    @Test
    @DisplayName("POST /grades (3rd of 4) -> response.isGradingComplete=false; no notifications fired")
    void postGrade_thirdGrade_doesNotCompleteAndDoesNotNotify() {
        seedGrade(advisor1Id, 70.0);
        seedGrade(advisor2Id, 85.0);

        long coordNotifBefore  = countSystemAlerts(coordinatorId, "GRADING_COMPLETE");
        long leaderNotifBefore = countSystemAlerts(leaderId,      "GRADING_COMPLETE");

        given()
            .header("Authorization", "Bearer " + jury1Token)
            .body(Map.of("grade", 90.0))
        .when()
            .post("/api/v1/submissions/" + currentSubmissionId + "/grades")
        .then()
            .statusCode(201)
            .body("status", equalTo("success"))
            .body("data.isGradingComplete", is(false))
            .body("data.gradeId", notNullValue());

        // Spec § 3.5 "Internal Side-Effects": notifications fire only when ALL members
        // have graded. With 3 of 4 grades, neither coordinator nor leader is notified.
        Assertions.assertEquals(
                coordNotifBefore,
                countSystemAlerts(coordinatorId, "GRADING_COMPLETE"),
                "Coordinator must not receive GRADING_COMPLETE notification before all members grade");
        Assertions.assertEquals(
                leaderNotifBefore,
                countSystemAlerts(leaderId, "GRADING_COMPLETE"),
                "Group leader must not receive GRADING_COMPLETE notification before all members grade");
    }

    // ─── AC #2 + AC #3 + spec § 3.5 (final side-effects) ───────────────────

    @Test
    @DisplayName("POST /grades (4th of 4) -> completes, averages exactly, notifies coordinator+leader, exposes avg to student")
    void postGrade_finalGrade_completesAndAveragesAndNotifiesAndExposesToStudent() {
        seedGrade(advisor1Id, 70.0);
        seedGrade(advisor2Id, 85.0);
        seedGrade(jury1Id,    90.0);
        // Final POST score 95.0 -> avg = (70 + 85 + 90 + 95) / 4 = 85.0

        long coordNotifBefore  = countSystemAlerts(coordinatorId, "GRADING_COMPLETE");
        long leaderNotifBefore = countSystemAlerts(leaderId,      "GRADING_COMPLETE");

        given()
            .header("Authorization", "Bearer " + jury2Token)
            .body(Map.of("grade", 95.0))
        .when()
            .post("/api/v1/submissions/" + currentSubmissionId + "/grades")
        .then()
            .statusCode(201)
            .body("status", equalTo("success"))
            .body("data.isGradingComplete", is(true))
            .body("data.gradeId", notNullValue());

        // AC #2 + AC #3: student now sees the exact average.
        Response getResponse = given()
            .header("Authorization", "Bearer " + leaderToken)
        .when()
            .get("/api/v1/submissions/" + currentSubmissionId + "/grades")
        .then()
            .statusCode(200)
            .body("data.isGradingComplete", is(true))
            .body("data.gradeCount", equalTo(4))
            .body("data.totalCommitteeMembers", equalTo(4))
            .extract().response();

        Double averageGrade = getResponse.jsonPath().getDouble("data.averageGrade");
        Assertions.assertEquals(85.0, averageGrade, 0.001,
                "AC #2: average must be exactly (70+85+90+95)/4 = 85.0");

        // Spec § 3.5: notifications to coordinator (committee.createdBy) and leader,
        // exactly once each, on the final grade.
        Assertions.assertEquals(
                coordNotifBefore + 1,
                countSystemAlerts(coordinatorId, "GRADING_COMPLETE"),
                "Coordinator must receive exactly one GRADING_COMPLETE notification on completion");
        Assertions.assertEquals(
                leaderNotifBefore + 1,
                countSystemAlerts(leaderId, "GRADING_COMPLETE"),
                "Group leader must receive exactly one GRADING_COMPLETE notification on completion");

        // Submission status transitions to GRADED (spec listSubmissions enum).
        SubmissionStatus persisted = submissionRepository.findById(currentSubmissionId)
                .map(Submission::getStatus).orElse(null);
        Assertions.assertEquals(SubmissionStatus.GRADED, persisted,
                "Submission status must transition to GRADED on completion");
    }

    // ─── §5 contract: duplicate grade rejected ─────────────────────────────

    @Test
    @DisplayName("POST /grades (same professor twice) -> 400 (duplicate grade rejected)")
    void postGrade_sameProfessorTwice_returns400() {
        seedGrade(advisor1Id, 70.0);

        given()
            .header("Authorization", "Bearer " + advisor1Token)
            .body(Map.of("grade", 88.0))
        .when()
            .post("/api/v1/submissions/" + currentSubmissionId + "/grades")
        .then()
            .statusCode(400);
    }

    // ─── helpers ───────────────────────────────────────────────────────────

    private void seedGrade(Long professorId, Double score) {
        tx.executeWithoutResult(status -> {
            SubmissionGrade g = new SubmissionGrade();
            g.setSubmissionId(currentSubmissionId);
            g.setProfessorId(professorId);
            g.setScore(score);
            em.persist(g);
        });
    }

    /**
     * §9 limitation note (see class javadoc): NotificationType has no GRADING_COMPLETE
     * value, so we count SYSTEM_ALERT notifications addressed to the user whose
     * persisted message embeds the discriminator. This is the most structural check
     * available against the current notification subsystem.
     */
    private long countSystemAlerts(Long toUserId, String discriminator) {
        List<Notification> all = notificationRepository
                .findByToUser_UserIdAndTypeOrderByCreatedAtDesc(toUserId, NotificationType.SYSTEM_ALERT);
        return all.stream()
                .filter(n -> n.getMessage() != null && n.getMessage().contains(discriminator))
                .count();
    }

    /**
     * TestDataFactory does not yet expose a coordinator helper. Mirrors
     * {@link TestDataFactory#createProfessor} but with role=coordinator.
     */
    private User createCoordinator(String fullName, String email) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setFullName(fullName);
            u.setEmail(email);
            u.setRole("coordinator");
            u.setPasswordHash("pbkdf2$65536$dummySalt$dummyHash");
            u.setRequiresPasswordChange(false);
            u.setCreatedAt(Instant.now());
            return userRepository.save(u);
        });
    }
}
