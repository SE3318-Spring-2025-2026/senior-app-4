package com.spms.api;

import com.spms.backend.client.GithubApiClient;
import com.spms.backend.client.JiraApiClient;
import com.spms.backend.model.Group;
import com.spms.backend.model.GithubIntegration;
import com.spms.backend.model.JiraIntegration;
import com.spms.backend.model.User;
import com.spms.backend.repository.*;
import com.spms.backend.service.TokenService;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Integration tests for error handling in GitHub and JIRA integration endpoints.
 * 
 * Verifies that endpoints return correct status codes and error messages 
 * as specified in Issue #19 and #15 requirements.
 */
public class IntegrationErrorApiTest extends BaseApiTest {

    @MockBean
    private GithubApiClient githubApiClient;

    @MockBean
    private JiraApiClient jiraApiClient;

    @MockBean
    private GroupRepository groupRepository;

    @MockBean
    private GithubIntegrationRepository githubIntegrationRepository;

    @MockBean
    private JiraIntegrationRepository jiraIntegrationRepository;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private TokenService tokenService;

    private String leaderToken;
    private final Long groupId = 101L;
    private final Long leaderUserId = 1L;

    @BeforeEach
    void setup() {
        // Create a mock leader user
        User leader = new User();
        leader.setUserId(leaderUserId);
        leader.setStudentId("12345678901");
        leader.setRole("student");

        // Mint token for the leader
        leaderToken = tokenService.generateToken(leader);

        // Mock Group lookup
        Group group = new Group();
        group.setId(groupId);
        group.setLeader(leader);
        
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(userRepository.findById(leaderUserId)).thenReturn(Optional.of(leader));
    }

    @Test
    @DisplayName("POST /github → 400: Invalid PAT")
    void bindGithub_invalidPat_returns400() {
        when(githubApiClient.validateOrganizationAccess(anyString(), anyString()))
                .thenReturn(false);

        RestAssured.given()
            .header("Authorization", "Bearer " + leaderToken)
            .body(Map.of(
                "organizationName", "some-org",
                "githubPat", "invalid-pat"
            ))
        .when()
            .post("/api/v1/groups/" + groupId + "/integrations/github")
        .then()
            .statusCode(400)
            .body("error", notNullValue())
            .body("message", containsStringIgnoringCase("Invalid PAT"));
    }

    @Test
    @DisplayName("POST /github → 400: Non-existent Org")
    void bindGithub_nonExistentOrg_returns400() {
        when(githubApiClient.validateOrganizationAccess(anyString(), anyString()))
                .thenReturn(false);

        RestAssured.given()
            .header("Authorization", "Bearer " + leaderToken)
            .body(Map.of(
                "organizationName", "non-existent-org",
                "githubPat", "some-pat"
            ))
        .when()
            .post("/api/v1/groups/" + groupId + "/integrations/github")
        .then()
            .statusCode(400)
            .body("error", notNullValue());
    }

    @Test
    @DisplayName("POST /jira → 400: Invalid URL or Connection Failure")
    void bindJira_connectionFailure_returns400() {
        when(jiraApiClient.validateSpaceConnection(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(false);

        RestAssured.given()
            .header("Authorization", "Bearer " + leaderToken)
            .body(Map.of(
                "jiraSpaceUrl", "http://invalid-jira-url",
                "email", "test@atlassian.net",
                "projectKey", "PROJ",
                "apiKey", "some-key"
            ))
        .when()
            .post("/api/v1/groups/" + groupId + "/integrations/jira")
        .then()
            .statusCode(400)
            .body("error", notNullValue())
            .body("message", containsStringIgnoringCase("Connection failed"));
    }

    @Test
    @DisplayName("POST /test → 400: Partial failure (GitHub OK, JIRA fails)")
    void testIntegrations_partialFailure_returns400() {
        // Setup existing integrations
        GithubIntegration github = new GithubIntegration();
        github.setOrganizationName("org");
        github.setGithubPatEncrypted("pat");
        
        JiraIntegration jira = new JiraIntegration();
        jira.setJiraSpaceUrl("url");
        jira.setProjectKey("key");
        jira.setApiKey("key");

        when(githubIntegrationRepository.findByGroup_Id(groupId)).thenReturn(Optional.of(github));
        when(jiraIntegrationRepository.findByGroup_Id(groupId)).thenReturn(Optional.of(jira));

        // GitHub succeeds, JIRA fails
        when(githubApiClient.validateOrganizationAccess(anyString(), anyString())).thenReturn(true);
        when(jiraApiClient.validateSpaceConnection(anyString(), anyString(), anyString(), anyString())).thenReturn(false);

        RestAssured.given()
            .header("Authorization", "Bearer " + leaderToken)
        .when()
            .post("/api/v1/groups/" + groupId + "/integrations/test")
        .then()
            .statusCode(400)
            .body("github.connected", equalTo(true))
            .body("jira.connected", equalTo(false));
    }

    @Test
    @DisplayName("POST /test → 404: No integrations configured")
    void testIntegrations_noIntegrations_returns404() {
        when(githubIntegrationRepository.findByGroup_Id(groupId)).thenReturn(Optional.empty());
        when(jiraIntegrationRepository.findByGroup_Id(groupId)).thenReturn(Optional.empty());

        RestAssured.given()
            .header("Authorization", "Bearer " + leaderToken)
        .when()
            .post("/api/v1/groups/" + groupId + "/integrations/test")
        .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("DELETE /github → 404: Not bound")
    void unbindGithub_notBound_returns404() {
        when(githubIntegrationRepository.findByGroup_Id(groupId)).thenReturn(Optional.empty());

        RestAssured.given()
            .header("Authorization", "Bearer " + leaderToken)
        .when()
            .delete("/api/v1/groups/" + groupId + "/integrations/github")
        .then()
            .statusCode(404);
    }
}
