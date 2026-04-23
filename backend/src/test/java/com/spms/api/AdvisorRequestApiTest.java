package com.spms.api;

import com.spms.backend.model.User;
import com.spms.backend.repository.UserRepository;
import com.spms.backend.repository.ValidStudentIdRepository;
import com.spms.backend.service.TokenService;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AdvisorRequestApiTest extends BaseApiTest {

    @Autowired private TokenService tokenService;
    @Autowired private UserRepository userRepository;
    @Autowired private ValidStudentIdRepository validStudentIdRepository;

    private static final AtomicLong SEQ = new AtomicLong(System.currentTimeMillis());

    private String profToken;
    private Long profUserId;

    @BeforeAll
    void setup() {
        User prof = TestDataFactory.createProfessor(userRepository, "Prof Test", TestDataFactory.uniqueEmail());
        profUserId = prof.getUserId();
        profToken = TestDataFactory.mintToken(tokenService, prof);
    }

    @Test
    @Order(1)
    @DisplayName("POST /advisor-requests/{id}/decision (APPROVE) -> Returns AdvisorDecisionResponse")
    void approveAdvisorRequest() {
        String lToken = createFreshStudentToken();
        int gId = createTestGroup("Group-1", lToken);
        int rId = sendTestAdvisorRequest(gId, profUserId, lToken);

        given()
            .header("Authorization", "Bearer " + profToken)
            .body(Map.of("decision", "APPROVE"))
        .when()
            .post("/api/v1/advisor-requests/" + rId + "/decision")
        .then()
            .statusCode(200)
            .body("status", equalTo("success"))
            .body("data.decision", equalTo("APPROVE"))
            .body("data.teamId", equalTo(gId))
            .body("data.advisorId", equalTo(profUserId.intValue()));
    }

    @Test
    @Order(2)
    @DisplayName("POST /advisor-requests/{id}/decision (REJECT) -> Fails if no reason provided")
    void rejectAdvisorRequest_failsWithoutReason() {
        String lToken = createFreshStudentToken();
        int gId = createTestGroup("Group-2", lToken);
        int rId = sendTestAdvisorRequest(gId, profUserId, lToken);

        given()
            .header("Authorization", "Bearer " + profToken)
            .body(Map.of("decision", "REJECT"))
        .when()
            .post("/api/v1/advisor-requests/" + rId + "/decision")
        .then()
            .statusCode(400);
    }

    @Test
    @Order(3)
    @DisplayName("POST /advisor-requests/{id}/decision (REJECT) -> Success with reason")
    void rejectAdvisorRequest_successWithReason() {
        String lToken = createFreshStudentToken();
        int gId = createTestGroup("Group-3", lToken);
        int rId = sendTestAdvisorRequest(gId, profUserId, lToken);

        given()
            .header("Authorization", "Bearer " + profToken)
            .body(Map.of("decision", "REJECT", "reason", "Too busy"))
        .when()
            .post("/api/v1/advisor-requests/" + rId + "/decision")
        .then()
            .statusCode(200)
            .body("status", equalTo("success"))
            .body("data.decision", equalTo("REJECT"));
    }

    private String createFreshStudentToken() {
        String studentId = TestDataFactory.uniqueStudentId();
        TestDataFactory.seedStudentId(validStudentIdRepository, studentId);
        User leader = TestDataFactory.createStudent(userRepository, "Leader Student", studentId, "gh-" + SEQ.incrementAndGet());
        return TestDataFactory.mintToken(tokenService, leader);
    }

    private int createTestGroup(String suffix, String lToken) {
        return given()
                .header("Authorization", "Bearer " + lToken)
                .body(TestDataFactory.groupCreatePayload("API-Test-" + suffix + "-" + SEQ.get()))
            .post("/api/v1/groups")
            .then()
                .statusCode(201)
                .extract().path("id");
    }

    private int sendTestAdvisorRequest(int gId, Long pId, String lToken) {
        return given()
                .header("Authorization", "Bearer " + lToken)
                .body(Map.of("professorId", pId))
            .post("/api/v1/groups/" + gId + "/advisor-request")
            .then()
                .statusCode(201)
                .extract().path("data.requestId");
    }
}
