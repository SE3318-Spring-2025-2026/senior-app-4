package com.spms.api;

import com.spms.backend.model.*;
import com.spms.backend.repository.AdvisorRequestRepository;
import com.spms.backend.repository.GroupMemberRepository;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.UserRepository;
import com.spms.backend.repository.ValidStudentIdRepository;
import com.spms.backend.service.TokenService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * API tests — GET /api/v1/advisor-requests/{requestId} [P4-DETAIL-1]
 *
 * Covers 200 paths (professor + coordinator). Error paths (403, 404) are
 * covered by AdvisorRequestDetailServiceTest at the service layer, since
 * RestAssured 5.x throws HttpResponseException for non-2xx responses when
 * no prior 2xx warm-up has occurred in the test sequence.
 *
 * Cleanup: @AfterAll deletes advisor_requests → group_members → groups → users in FK order.
 */
@ActiveProfiles("dev-mock")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AdvisorRequestDetailApiTest extends BaseApiTest {

    @Autowired private TokenService tokenService;
    @Autowired private UserRepository userRepository;
    @Autowired private ValidStudentIdRepository validStudentIdRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired private GroupMemberRepository groupMemberRepository;
    @Autowired private AdvisorRequestRepository advisorRequestRepository;

    // ── Test actors ────────────────────────────────────────────────
    private User professor;
    private User coordinatorUser;
    private User studentLeader;

    private String profToken;
    private String coordToken;

    private String leaderStudentId;

    // ── Seeded data ────────────────────────────────────────────────
    private Long groupId;
    private Long requestId;

    @BeforeAll
    void seedTestData() {
        leaderStudentId = TestDataFactory.uniqueStudentId();
        TestDataFactory.seedStudentId(validStudentIdRepository, leaderStudentId);

        studentLeader   = TestDataFactory.createStudent(userRepository, "Detail-Leader",
                leaderStudentId, TestDataFactory.uniqueGithubUsername());
        professor       = TestDataFactory.createProfessor(userRepository, "Dr. Target Prof",
                TestDataFactory.uniqueEmail());
        coordinatorUser = TestDataFactory.createProfessor(userRepository, "Detail-Coordinator",
                TestDataFactory.uniqueEmail());
        coordinatorUser.setRole("coordinator");
        coordinatorUser = userRepository.save(coordinatorUser);

        profToken  = TestDataFactory.mintToken(tokenService, professor);
        coordToken = TestDataFactory.mintToken(tokenService, coordinatorUser);

        // Group seeded directly — no REST call needed for setup
        Group group = new Group();
        group.setGroupName("Detail-Test-Group");
        group.setLeader(studentLeader);
        group.setStatus(GroupStatus.FORMING);

        GroupMember leaderMember = new GroupMember();
        leaderMember.setGroup(group);
        leaderMember.setUser(studentLeader);
        leaderMember.setRole(GroupRole.LEADER);
        group.getMembers().add(leaderMember);

        group = groupRepository.save(group);
        groupId = group.getId();

        AdvisorRequest req = new AdvisorRequest();
        req.setGroup(group);
        req.setProfessor(professor);
        req.setRequester(studentLeader);
        req.setMessage("Please advise us");
        req.setStatus(AdvisorRequestStatus.PENDING);
        req.setRequestedAt(Instant.now());
        requestId = advisorRequestRepository.save(req).getId();
    }

    @AfterAll
    void cleanupTestData() {
        // FK order: advisor_requests → group_members → groups → users → valid_student_ids
        if (requestId != null) {
            try { advisorRequestRepository.deleteById(requestId); } catch (Exception ignored) {}
        }
        if (groupId != null) {
            try {
                groupMemberRepository.findAll().stream()
                        .filter(m -> m.getGroup().getId().equals(groupId))
                        .forEach(groupMemberRepository::delete);
            } catch (Exception ignored) {}
            try { groupRepository.deleteById(groupId); } catch (Exception ignored) {}
        }
        deleteUserSafely(studentLeader);
        deleteUserSafely(professor);
        deleteUserSafely(coordinatorUser);
        deleteStudentIdSafely(leaderStudentId);
    }

    // ── Tests ──────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("200 — targeted professor sees full request detail")
    void targetedProfessorGetsDetail() {
        given()
            .header("Authorization", "Bearer " + profToken)
        .when()
            .get("/api/v1/advisor-requests/" + requestId)
        .then()
            .statusCode(200)
            .body("status", equalTo("success"))
            .body("data.id", equalTo(requestId.intValue()))
            .body("data.status", equalTo("PENDING"))
            .body("data.message", equalTo("Please advise us"))
            .body("data.decidedAt", nullValue())
            .body("data.team.name", equalTo("Detail-Test-Group"))
            .body("data.team.memberCount", greaterThanOrEqualTo(1))
            .body("data.team.leaderName", notNullValue())
            .body("data.team.hasCurrentAdvisor", equalTo(false))
            .body("data.professor.id", equalTo(professor.getUserId().intValue()))
            .body("data.professor.fullName", equalTo("Dr. Target Prof"))
            .body("data.professor.currentAdviseeCount", greaterThanOrEqualTo(0));
    }

    @Test
    @Order(2)
    @DisplayName("200 — coordinator sees full request detail")
    void coordinatorGetsDetail() {
        given()
            .header("Authorization", "Bearer " + coordToken)
        .when()
            .get("/api/v1/advisor-requests/" + requestId)
        .then()
            .statusCode(200)
            .body("data.id", equalTo(requestId.intValue()))
            .body("data.professor.fullName", equalTo("Dr. Target Prof"));
    }

    // ── Cleanup helpers ────────────────────────────────────────────

    private void deleteUserSafely(User user) {
        if (user == null || user.getUserId() == null) return;
        try { userRepository.findByUserId(user.getUserId()).ifPresent(userRepository::delete); }
        catch (Exception ignored) {}
    }

    private void deleteStudentIdSafely(String studentId) {
        if (studentId == null) return;
        try { validStudentIdRepository.deleteByStudentId(studentId); }
        catch (Exception ignored) {}
    }
}
