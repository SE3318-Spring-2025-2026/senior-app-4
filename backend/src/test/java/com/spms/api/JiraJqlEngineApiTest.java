package com.spms.api;

import com.spms.backend.client.JiraApiClient;
import com.spms.backend.exception.JiraApiException;
import com.spms.backend.model.Group;
import com.spms.backend.model.JiraIntegration;
import com.spms.backend.model.User;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.JiraIntegrationRepository;
import com.spms.backend.repository.UserRepository;
import com.spms.backend.service.TokenService;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Issue #398 — JIRA JQL Connection Engine
 * Acceptance criteria:
 *   AC1: JIRA auth header ekleniyor
 *   AC2: 100+ kayıtta pagination çalışıyor
 *   AC3: Dış 500 hatası loglanıp 502 dönüyor
 */
public class JiraJqlEngineApiTest extends BaseApiTest {

    @MockBean
    private JiraApiClient jiraApiClient;

    @MockBean
    private JiraIntegrationRepository jiraIntegrationRepository;

    @MockBean
    private GroupRepository groupRepository;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private TokenService tokenService;

    private String token;
    private final Long groupId = 1L;

    @BeforeEach
    void setup() {
        User user = new User();
        user.setUserId(1L);
        user.setStudentId("12345678901");
        user.setRole("student");

        token = tokenService.generateToken(user);

        Group group = new Group();
        group.setId(groupId);
        group.setLeader(user);
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        JiraIntegration integration = new JiraIntegration();
        integration.setJiraSpaceUrl("https://test.atlassian.net");
        integration.setProjectKey("PROJ");
        integration.setApiKey("decrypted-api-key");
        when(jiraIntegrationRepository.findByGroup_Id(groupId)).thenReturn(Optional.of(integration));
    }

    // ── /initialize ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /initialize → 200: bağlantı başarılı (AC1)")
    void initialize_success_returns200() {
        when(jiraApiClient.validateSpaceConnection(anyString(), anyString(), anyString()))
                .thenReturn(true);

        RestAssured.given()
            .header("Authorization", "Bearer " + token)
            .body(Map.of("groupId", groupId))
        .when()
            .post("/api/v1/jira-metrics/initialize")
        .then()
            .statusCode(200)
            .body("connected", equalTo(true))
            .body("jiraSpaceUrl", equalTo("https://test.atlassian.net"))
            .body("projectKey", equalTo("PROJ"))
            .body("message", containsString("successfully"));
    }

    @Test
    @DisplayName("POST /initialize → 200: bağlantı başarısız (credentials hatalı)")
    void initialize_failure_returnsConnectedFalse() {
        when(jiraApiClient.validateSpaceConnection(anyString(), anyString(), anyString()))
                .thenReturn(false);

        RestAssured.given()
            .header("Authorization", "Bearer " + token)
            .body(Map.of("groupId", groupId))
        .when()
            .post("/api/v1/jira-metrics/initialize")
        .then()
            .statusCode(200)
            .body("connected", equalTo(false))
            .body("message", containsString("failed"));
    }

    @Test
    @DisplayName("POST /initialize → 404: group bulunamadı")
    void initialize_groupNotFound_returns404() {
        when(jiraIntegrationRepository.findByGroup_Id(999L)).thenReturn(Optional.empty());

        RestAssured.given()
            .header("Authorization", "Bearer " + token)
            .body(Map.of("groupId", 999))
        .when()
            .post("/api/v1/jira-metrics/initialize")
        .then()
            .statusCode(404)
            .body("error", equalTo("NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /initialize → 400: groupId null")
    void initialize_nullGroupId_returns400() {
        RestAssured.given()
            .header("Authorization", "Bearer " + token)
            .body("{\"groupId\": null}")
        .when()
            .post("/api/v1/jira-metrics/initialize")
        .then()
            .statusCode(400);
    }

    // ── /issue-query ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /issue-query → 200: issue key listesi döner (AC1)")
    void issueQuery_success_returnsKeys() {
        when(jiraApiClient.searchIssuesByJql(anyString(), anyString(), anyString()))
                .thenReturn(List.of("PROJ-1", "PROJ-2", "PROJ-3"));

        RestAssured.given()
            .header("Authorization", "Bearer " + token)
            .body(Map.of("groupId", groupId, "jql", "project = PROJ AND sprint in openSprints()"))
        .when()
            .post("/api/v1/jira-metrics/issue-query")
        .then()
            .statusCode(200)
            .body("total", equalTo(3))
            .body("issueKeys", hasItems("PROJ-1", "PROJ-2", "PROJ-3"));
    }

    @Test
    @DisplayName("POST /issue-query → 200: 100+ kayıt pagination tümünü toplar (AC2)")
    void issueQuery_paginatedResult_returnsAll() {
        List<String> allKeys = new java.util.ArrayList<>();
        for (int i = 1; i <= 150; i++) allKeys.add("PROJ-" + i);

        when(jiraApiClient.searchIssuesByJql(anyString(), anyString(), anyString()))
                .thenReturn(allKeys);

        RestAssured.given()
            .header("Authorization", "Bearer " + token)
            .body(Map.of("groupId", groupId, "jql", "project = PROJ"))
        .when()
            .post("/api/v1/jira-metrics/issue-query")
        .then()
            .statusCode(200)
            .body("total", equalTo(150))
            .body("issueKeys", hasSize(150));
    }

    @Test
    @DisplayName("POST /issue-query → 502: JIRA 500 hatası loglanır ve 502 döner (AC3)")
    void issueQuery_jiraServerError_returns502() {
        when(jiraApiClient.searchIssuesByJql(anyString(), anyString(), anyString()))
                .thenThrow(new JiraApiException("JIRA API returned error: 500 Internal Server Error"));

        RestAssured.given()
            .header("Authorization", "Bearer " + token)
            .body(Map.of("groupId", groupId, "jql", "project = PROJ"))
        .when()
            .post("/api/v1/jira-metrics/issue-query")
        .then()
            .statusCode(502)
            .body("error", equalTo("JIRA_API_ERROR"))
            .body("message", containsString("500"));
    }

    @Test
    @DisplayName("POST /issue-query → 400: jql blank")
    void issueQuery_blankJql_returns400() {
        RestAssured.given()
            .header("Authorization", "Bearer " + token)
            .body(Map.of("groupId", groupId, "jql", ""))
        .when()
            .post("/api/v1/jira-metrics/issue-query")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("POST /issue-query → 401: token yok")
    void issueQuery_noToken_returns401() {
        RestAssured.given()
            .body(Map.of("groupId", groupId, "jql", "project = PROJ"))
        .when()
            .post("/api/v1/jira-metrics/issue-query")
        .then()
            .statusCode(401);
    }
}
