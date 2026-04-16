package com.spms.api;

import com.spms.backend.model.User;
import com.spms.backend.model.ValidStudentId;
import com.spms.backend.repository.UserRepository;
import com.spms.backend.repository.ValidStudentIdRepository;
import com.spms.backend.service.TokenService;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * End-to-end API tests for the Group Creation and Role Assignment workflow.
 *
 * <h3>Scope</h3>
 * <ul>
 *   <li>P2-API-01 : POST   /api/v1/groups                   — Create group, creator becomes leader</li>
 *   <li>P2-API-12 : GET    /api/v1/groups                   — Verify created group appears in list</li>
 *   <li>P2-API-02 : PUT    /api/v1/groups/{groupId}         — Update group (authorization check)</li>
 *   <li>P2-API-05 : POST   /api/v1/groups/{groupId}/members — Add member with role "member"</li>
 *   <li>P2-API-06 : GET    /api/v1/groups/{groupId}/members — List members (leader + members)</li>
 * </ul>
 *
 * <h3>DFD Sub-processes</h3>
 * 2.1 (Manage Project Groups), 2.2 (Manage Group Members), 2.4 (View Project Groups)
 *
 * <h3>Token Generation Strategy</h3>
 * <p>
 *     Student tokens are normally obtained through the GitHub OAuth flow
 *     (GithubOAuthService → GithubApiClient), which requires real GitHub API
 *     calls and cannot work in an isolated test environment.
 * </p>
 * <p>
 *     Solution: We inject {@code @Autowired TokenService} from the Spring context
 *     and call {@link TokenService#generateToken(User)} directly. Since the tokens
 *     are signed with the same {@code token.secret}, {@code JwtAuthFilter} accepts
 *     them. User records are persisted directly via {@code @Autowired UserRepository}.
 * </p>
 *
 * <h3>Acceptance Criteria</h3>
 * <ol>
 *   <li>All happy-path tests pass (create → add → verify)</li>
 *   <li>Duplicate student returns 400 with clear error message</li>
 *   <li>Non-existent studentId returns 400</li>
 *   <li>Full group returns 400 with member limit info</li>
 *   <li>Non-leader update returns 403</li>
 *   <li>Test database is seeded and cleaned per test run</li>
 * </ol>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GroupWorkflowApiTest extends BaseApiTest {

    // ─── Spring-managed beans ─────────────────────────────────────
    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ValidStudentIdRepository validStudentIdRepository;

    // ─── Atomic counter for unique values per test run ─────────────
    private static final AtomicLong SEQ = new AtomicLong(System.currentTimeMillis());

    // ─── Shared state across ordered tests ─────────────────────────
    private String leaderToken;
    private String memberToken;
    private String nonLeaderToken;
    private int createdGroupId;
    private String leaderStudentId;
    private String memberStudentId;
    private String nonLeaderStudentId;

    // ─── User IDs retained for cleanup ─────────────────────────────
    private Long leaderUserId;
    private Long memberUserId;
    private Long nonLeaderUserId;

    // ═══════════════════════════════════════════════════════════════
    //  SETUP & TEARDOWN
    // ═══════════════════════════════════════════════════════════════

    /**
     * Runs once before all tests:
     * <ol>
     *   <li>Generates unique student IDs.</li>
     *   <li>Seeds the valid_student_ids table via ValidStudentIdRepository.</li>
     *   <li>Creates student user records via UserRepository.</li>
     *   <li>Mints JWT tokens via TokenService.generateToken(user).</li>
     * </ol>
     * Bypasses the GitHub OAuth flow — tokens are signed with the same
     * HMAC-SHA256 secret so JwtAuthFilter accepts them.
     */
    @BeforeAll
    void seedTestData() {
        leaderStudentId    = uniqueStudentId();
        memberStudentId    = uniqueStudentId();
        nonLeaderStudentId = uniqueStudentId();

        // 1) Seed valid_student_ids table
        saveValidStudentId(leaderStudentId);
        saveValidStudentId(memberStudentId);
        saveValidStudentId(nonLeaderStudentId);

        // 2) Create student user records in the users table
        User leaderUser    = createAndSaveStudent("Leader QA User",    leaderStudentId,    "gh-ldr-" + SEQ.get());
        User memberUser    = createAndSaveStudent("Member QA User",    memberStudentId,    "gh-mbr-" + SEQ.get());
        User nonLeaderUser = createAndSaveStudent("NonLeader QA User", nonLeaderStudentId, "gh-nlr-" + SEQ.get());

        leaderUserId    = leaderUser.getUserId();
        memberUserId    = memberUser.getUserId();
        nonLeaderUserId = nonLeaderUser.getUserId();

        // 3) Mint real JWT tokens via TokenService
        //    (Same secret + same HMAC-SHA256 → JwtAuthFilter accepts them)
        leaderToken    = tokenService.generateToken(leaderUser);
        memberToken    = tokenService.generateToken(memberUser);
        nonLeaderToken = tokenService.generateToken(nonLeaderUser);

        System.out.println("=== TEST SETUP ===");
        System.out.println("  Leader    : userId=" + leaderUserId    + ", studentId=" + leaderStudentId);
        System.out.println("  Member    : userId=" + memberUserId    + ", studentId=" + memberStudentId);
        System.out.println("  NonLeader : userId=" + nonLeaderUserId + ", studentId=" + nonLeaderStudentId);
        System.out.println("  Tokens minted via TokenService.generateToken()");
        System.out.println("==================");
    }

    /**
     * Runs once after all tests.
     * Cleanup order matters due to FK constraints:
     * <ol>
     *   <li>Delete the group (cascades to memberships)</li>
     *   <li>Delete test users</li>
     *   <li>Delete valid_student_ids records</li>
     * </ol>
     */
    @AfterAll
    void cleanupTestData() {
        System.out.println("=== TEST CLEANUP ===");

        // 1) Delete the group via API (cascades to members)
        if (createdGroupId > 0 && leaderToken != null) {
            given()
                .header("Authorization", "Bearer " + leaderToken)
            .when()
                .delete("/api/v1/groups/" + createdGroupId)
            .then()
                .log().ifError();
            System.out.println("  Group " + createdGroupId + " deleted");
        }

        // 2) Delete filler users created during TEST-7
        //    They are removed from group_members on group deletion,
        //    but remain in the users table. Clean up by pattern match.
        userRepository.findAll().stream()
                .filter(u -> u.getStudentId() != null && u.getStudentId().startsWith("99"))
                .filter(u -> u.getEmail() != null && u.getEmail().endsWith("@spms-test.com"))
                .forEach(u -> {
                    userRepository.delete(u);
                    System.out.println("  Filler user deleted: " + u.getStudentId());
                });

        // 3) Delete primary test users
        deleteUserSafely(leaderUserId,    "Leader");
        deleteUserSafely(memberUserId,    "Member");
        deleteUserSafely(nonLeaderUserId, "NonLeader");

        // 4) Delete valid_student_ids records
        deleteStudentIdSafely(leaderStudentId);
        deleteStudentIdSafely(memberStudentId);
        deleteStudentIdSafely(nonLeaderStudentId);

        // Also clean up filler student IDs
        validStudentIdRepository.findAll().stream()
                .filter(sid -> sid.getStudentId() != null && sid.getStudentId().startsWith("99"))
                .forEach(sid -> {
                    validStudentIdRepository.delete(sid);
                    System.out.println("  Filler studentId deleted: " + sid.getStudentId());
                });

        System.out.println("====================");
    }

    // ═══════════════════════════════════════════════════════════════
    //  HAPPY PATH TESTS
    // ═══════════════════════════════════════════════════════════════

    /**
     * TEST 1 — POST /api/v1/groups
     * Creates a group and verifies that the creator is automatically
     * assigned the "leader" role.
     *
     * P2-API-01 · DFD: f1, f2, f2b, f3, f4 · Sub-process: 2.1
     */
    @Test
    @Order(1)
    @DisplayName("POST /groups → 201: Creator becomes leader")
    void createGroup_creatorBecomesLeader() {
        String groupName = "QA-Test-Group-" + SEQ.incrementAndGet();

        Response response = given()
            .header("Authorization", "Bearer " + leaderToken)
            .body(Map.of("groupName", groupName))
        .when()
            .post("/api/v1/groups")
        .then()
            .statusCode(201)
            .contentType(ContentType.JSON)
            .body("success", equalTo(true))
            .body("message", notNullValue())
            .body("data.groupId", notNullValue())
            .body("data.groupName", equalTo(groupName))
            .body("data.leaderId", notNullValue())
            .body("data.status", anyOf(equalTo("forming"), equalTo("formed")))
            .body("data.memberCount", greaterThanOrEqualTo(1))
        .extract()
            .response();

        createdGroupId = response.jsonPath().getInt("data.groupId");

        System.out.println("[TEST-1] Group created: groupId=" + createdGroupId
                + ", groupName=" + groupName);
    }

    /**
     * TEST 2 — GET /api/v1/groups
     * Verifies that the newly created group appears in the group list.
     *
     * P2-API-12 · DFD: f15, f16, f17, f18 · Sub-process: 2.4
     */
    @Test
    @Order(2)
    @DisplayName("GET /groups → 200: Created group appears in list")
    void listGroups_containsCreatedGroup() {
        given()
            .header("Authorization", "Bearer " + leaderToken)
        .when()
            .get("/api/v1/groups")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("success", equalTo(true))
            .body("count", greaterThanOrEqualTo(1))
            .body("data.groupId", hasItem(createdGroupId));

        System.out.println("[TEST-2] Group " + createdGroupId
                + " found in GET /groups list");
    }

    /**
     * TEST 3 — POST /api/v1/groups/{groupId}/members
     * Adds a member to the group and verifies the member receives the
     * "member" role.
     *
     * P2-API-05 · DFD: f5, f7, ns_f1 · Sub-process: 2.2
     */
    @Test
    @Order(3)
    @DisplayName("POST /members → 200: Member added with role 'member'")
    void addMember_roleIsMember() {
        given()
            .header("Authorization", "Bearer " + leaderToken)
            .body(Map.of("studentId", memberStudentId))
        .when()
            .post("/api/v1/groups/" + createdGroupId + "/members")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("success", equalTo(true))
            .body("message", notNullValue());

        System.out.println("[TEST-3] Member " + memberStudentId + " added to group "
                + createdGroupId);
    }

    /**
     * TEST 4 — GET /api/v1/groups/{groupId}/members
     * Verifies the member list contains both the leader and the added
     * member with their correct roles.
     *
     * P2-API-06 · DFD: f7b, f17 · Sub-process: 2.2
     */
    @Test
    @Order(4)
    @DisplayName("GET /members → 200: Leader and members returned correctly")
    void listMembers_returnsLeaderAndMembers() {
        Response response = given()
            .header("Authorization", "Bearer " + leaderToken)
        .when()
            .get("/api/v1/groups/" + createdGroupId + "/members")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("success", equalTo(true))
            .body("count", greaterThanOrEqualTo(2))
            .body("data.studentId", hasItem(leaderStudentId))
            .body("data.studentId", hasItem(memberStudentId))
        .extract()
            .response();

        // Verify roles: at least one "leader" and one "member" must exist
        response.then()
            .body("data.find { it.studentId == '" + leaderStudentId + "' }.role",
                    equalTo("leader"))
            .body("data.find { it.studentId == '" + memberStudentId + "' }.role",
                    equalTo("member"));

        System.out.println("[TEST-4] Members list contains leader ("
                + leaderStudentId + ") and member (" + memberStudentId + ")");
    }

    // ═══════════════════════════════════════════════════════════════
    //  EDGE CASE / NEGATIVE TESTS
    // ═══════════════════════════════════════════════════════════════

    /**
     * TEST 5 — POST /api/v1/groups (duplicate student)
     * Attempts to create a second group with a student who is already
     * in a group. Expected: 400 Bad Request.
     *
     * P2-API-01 · Sub-process: 2.1
     * Acceptance: "Duplicate student returns 400 with clear error message"
     */
    @Test
    @Order(5)
    @DisplayName("POST /groups → 400: Duplicate student (already in a group)")
    void createGroup_duplicateStudent_returns400() {
        given()
            .header("Authorization", "Bearer " + leaderToken)
            .body(Map.of("groupName", "Duplicate-Attempt-" + SEQ.incrementAndGet()))
        .when()
            .post("/api/v1/groups")
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("success", equalTo(false))
            .body("message", containsStringIgnoringCase("already"));

        System.out.println("[TEST-5] Duplicate student group creation blocked with 400");
    }

    /**
     * TEST 6 — POST /api/v1/groups/{groupId}/members (non-existent studentId)
     * Attempts to add a member with a studentId that does not exist in the
     * database. Expected: 400 Bad Request.
     *
     * P2-API-05 · Sub-process: 2.2
     * Acceptance: "Non-existent studentId returns 400"
     */
    @Test
    @Order(6)
    @DisplayName("POST /members → 400: Non-existent studentId")
    void addMember_nonExistentStudentId_returns400() {
        String fakeStudentId = "00000000000";

        given()
            .header("Authorization", "Bearer " + leaderToken)
            .body(Map.of("studentId", fakeStudentId))
        .when()
            .post("/api/v1/groups/" + createdGroupId + "/members")
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("success", equalTo(false))
            .body("message", notNullValue());

        System.out.println("[TEST-6] Non-existent studentId returns 400");
    }

    /**
     * TEST 7 — POST /api/v1/groups/{groupId}/members (group full)
     * Fills the group to its maximum capacity and then attempts to add
     * one more member. Expected: 400 Bad Request.
     *
     * P2-API-05 · Sub-process: 2.2
     * Acceptance: "Full group returns 400 with member limit info"
     *
     * Strategy: Add members until the group is full, then attempt one more.
     */
    @Test
    @Order(7)
    @DisplayName("POST /members → 400: Group is full (member limit exceeded)")
    void addMember_groupFull_returns400() {
        // Current state: leader (1) + member added in TEST-3 (1) = 2 members
        // Assuming a max of 5 members, we need to add 3 more to fill
        int maxMembers = 5;
        int currentMembers = 2;

        for (int i = currentMembers; i < maxMembers; i++) {
            String fillerStudentId = uniqueStudentId();
            saveValidStudentId(fillerStudentId);
            createAndSaveStudent("Filler-" + i, fillerStudentId,
                    "gh-fill-" + SEQ.incrementAndGet());

            Response addResponse = given()
                .header("Authorization", "Bearer " + leaderToken)
                .body(Map.of("studentId", fillerStudentId))
            .when()
                .post("/api/v1/groups/" + createdGroupId + "/members");

            if (addResponse.statusCode() == 400) {
                addResponse.then()
                    .body("success", equalTo(false))
                    .body("message", notNullValue());
                System.out.println("[TEST-7] Group is full, 400 returned at member #" + i);
                return;
            }
        }

        // After filling to capacity, attempt to add one more member
        String overflowStudentId = uniqueStudentId();
        saveValidStudentId(overflowStudentId);
        createAndSaveStudent("Overflow User", overflowStudentId,
                "gh-ovfl-" + SEQ.incrementAndGet());

        given()
            .header("Authorization", "Bearer " + leaderToken)
            .body(Map.of("studentId", overflowStudentId))
        .when()
            .post("/api/v1/groups/" + createdGroupId + "/members")
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("success", equalTo(false))
            .body("message", anyOf(
                containsStringIgnoringCase("limit"),
                containsStringIgnoringCase("full"),
                containsStringIgnoringCase("maximum")
            ));

        System.out.println("[TEST-7] Group full, member limit 400 returned");
    }

    /**
     * TEST 8 — PUT /api/v1/groups/{groupId} (non-leader)
     * Attempts to update a group using a token from a non-leader student.
     * Expected: 403 Forbidden.
     *
     * P2-API-02 · DFD: f1, f3 · Sub-process: 2.1
     * Acceptance: "Non-leader update returns 403"
     */
    @Test
    @Order(8)
    @DisplayName("PUT /groups/{groupId} → 403: Non-leader cannot update group")
    void updateGroup_byNonLeader_returns403() {
        given()
            .header("Authorization", "Bearer " + nonLeaderToken)
            .body(Map.of("groupName", "Hacked-Name-" + SEQ.incrementAndGet()))
        .when()
            .put("/api/v1/groups/" + createdGroupId)
        .then()
            .statusCode(403)
            .contentType(ContentType.JSON)
            .body("success", equalTo(false))
            .body("message", anyOf(
                containsStringIgnoringCase("forbidden"),
                containsStringIgnoringCase("leader"),
                containsStringIgnoringCase("authorized"),
                containsStringIgnoringCase("permission")
            ));

        System.out.println("[TEST-8] Non-leader update blocked with 403");
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPER METHODS — Direct database access via Spring repositories
    // ═══════════════════════════════════════════════════════════════

    /**
     * Generates a unique 11-digit student ID.
     * The "99" prefix makes test data easily identifiable for cleanup.
     */
    private String uniqueStudentId() {
        long seq = SEQ.incrementAndGet();
        return String.format("99%09d", seq % 1_000_000_000);
    }

    /**
     * Inserts a record directly into the valid_student_ids table.
     * Uses ValidStudentIdRepository — no API call involved.
     */
    private void saveValidStudentId(String studentId) {
        if (!validStudentIdRepository.existsByStudentId(studentId)) {
            validStudentIdRepository.saveStudentId(studentId);
        }
    }

    /**
     * Creates a student user record directly in the users table and
     * returns the persisted User entity.
     * Uses UserRepository — no API call involved.
     *
     * The returned User object is used with TokenService.generateToken(user).
     */
    private User createAndSaveStudent(String fullName, String studentId, String githubUsername) {
        // Return existing user if one already exists with this studentId
        return userRepository.findByStudentId(studentId)
                .orElseGet(() -> {
                    User user = new User();
                    user.setFullName(fullName);
                    user.setEmail(studentId + "@spms-test.com");
                    user.setStudentId(studentId);
                    user.setGithubUsername(githubUsername);
                    user.setRole("student");
                    user.setCreatedAt(Instant.now());
                    return userRepository.save(user);
                });
    }

    /**
     * Safely deletes a user record by ID.
     * Logs errors instead of throwing exceptions to avoid masking test failures.
     */
    private void deleteUserSafely(Long userId, String label) {
        if (userId == null) return;
        try {
            userRepository.findByUserId(userId).ifPresent(user -> {
                userRepository.delete(user);
                System.out.println("  " + label + " user deleted: userId=" + userId);
            });
        } catch (Exception e) {
            System.err.println("  [CLEANUP] Failed to delete " + label
                    + " user (userId=" + userId + "): " + e.getMessage());
        }
    }

    /**
     * Safely deletes a valid_student_ids record.
     * Logs errors instead of throwing exceptions.
     */
    private void deleteStudentIdSafely(String studentId) {
        if (studentId == null) return;
        try {
            validStudentIdRepository.deleteByStudentId(studentId);
            System.out.println("  StudentId deleted: " + studentId);
        } catch (Exception e) {
            System.err.println("  [CLEANUP] Failed to delete studentId "
                    + studentId + ": " + e.getMessage());
        }
    }
}
