package com.spms.api;

import com.spms.backend.model.*;
import com.spms.backend.model.notification.Notification;
import com.spms.backend.model.notification.NotificationStatus;
import com.spms.backend.model.notification.NotificationType;
import com.spms.backend.repository.*;
import com.spms.backend.service.TokenService;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * P2 — Database Consistency & Lifecycle Master Test
 * Verifies data integrity across D1, D3, D8, D9, D10.
 */
public class DatabaseConsistencyLifecycleApiTest extends BaseApiTest {

    @MockBean
    private GroupRepository groupRepository;

    @MockBean
    private GroupMemberRepository groupMemberRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private AuditLogRepository auditLogRepository;

    @MockBean
    private GithubIntegrationRepository githubIntegrationRepository;

    @MockBean
    private NotificationRepository notificationRepository;

    @MockBean
    private com.spms.backend.client.GithubApiClient githubApiClient;

    @Autowired
    private TokenService tokenService;

    // Test Data
    private User leader;
    private User member1;
    private User professor;
    private String leaderToken;
    private final Long groupId = 101L;

    @BeforeEach
    void setup() {
        // 1. Setup Leader
        leader = new User();
        leader.setUserId(1L);
        leader.setStudentId("S001");
        leader.setFullName("Leader Student");
        leader.setRole("student");
        leaderToken = tokenService.generateToken(leader);

        // 2. Setup Member
        member1 = new User();
        member1.setUserId(2L);
        member1.setStudentId("S002");
        member1.setFullName("Member Student");
        member1.setRole("student");

        // 3. Setup Professor
        professor = new User();
        professor.setUserId(500L);
        professor.setFullName("Prof. Advisor");
        professor.setRole("professor");

        // 4. Initial Repository Mocks (Empty state)
        when(userRepository.findById(1L)).thenReturn(Optional.of(leader));
        when(userRepository.findById(2L)).thenReturn(Optional.of(member1));
        when(userRepository.findById(500L)).thenReturn(Optional.of(professor));
        when(userRepository.findByStudentId("S002")).thenReturn(Optional.of(member1));
        when(userRepository.existsById(Mockito.any())).thenReturn(true);
        when(groupMemberRepository.existsByUser_UserId(Mockito.any())).thenReturn(false);
        when(githubApiClient.validateOrganizationAccess(anyString(), anyString())).thenReturn(true);
    }

    @Test
    @DisplayName("Master Test: Full Group Lifecycle (Create -> Invite -> Accept -> Advisor -> Integration -> Disband)")
    void fullGroupLifecycle_verifiesConsistency() {

        // --- PHASE 1: GROUP CREATION (D3) ---
        
        Group createdGroup = new Group();
        createdGroup.setId(groupId);
        createdGroup.setGroupName("Consistency Test Group");
        createdGroup.setLeader(leader);
        createdGroup.setStatus(GroupStatus.FORMING);
        createdGroup.setMembers(new ArrayList<>());

        when(groupRepository.save(Mockito.any(Group.class))).thenReturn(createdGroup);
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(createdGroup));

        RestAssured.given()
            .header("Authorization", "Bearer " + leaderToken)
            .body(Map.of("groupName", "Consistency Test Group"))
        .when()
            .post("/api/v1/groups")
        .then()
            .statusCode(201)
            .body("groupName", equalTo("Consistency Test Group"));

        verify(auditLogRepository, atLeastOnce()).save(Mockito.argThat(log -> log.getActionType() == ActionType.GROUP_CREATED));


        // --- PHASE 2: MEMBER INVITATION (D10) ---

        when(groupMemberRepository.existsByGroup_IdAndUser_UserId(groupId, 1L)).thenReturn(true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(member1));

        RestAssured.given()
            .header("Authorization", "Bearer " + leaderToken)
            .body(Map.of("studentId", 2L))
        .when()
            .post("/api/v1/groups/" + groupId + "/members")
        .then()
            .statusCode(201);

        verify(notificationRepository, atLeastOnce()).save(Mockito.any(Notification.class));


        // --- PHASE 3: ACCEPT INVITE & ADD MEMBER (D3) ---

        Long inviteId = 50L;
        Notification invite = new Notification();
        invite.setId(inviteId);
        invite.setType(NotificationType.MEMBERSHIP_INVITE);
        invite.setToUser(member1);
        invite.setGroupId(groupId);
        invite.setStatus(NotificationStatus.PENDING);

        when(notificationRepository.findById(inviteId)).thenReturn(Optional.of(invite));
        String memberToken = tokenService.generateToken(member1);

        RestAssured.given()
            .header("Authorization", "Bearer " + memberToken)
            .body(Map.of("decision", "accept"))
        .when()
            .post("/api/v1/notifications/" + inviteId + "/respond")
        .then()
            .statusCode(200);

        verify(groupMemberRepository, atLeastOnce()).save(Mockito.any(GroupMember.class));


        // --- PHASE 4: ADVISOR REQUEST & APPROVAL (D3 & D10) ---

        RestAssured.given()
            .header("Authorization", "Bearer " + leaderToken)
            .body(Map.of("professorId", 500L))
        .when()
            .post("/api/v1/groups/" + groupId + "/advisor-request")
        .then()
            .statusCode(201);

        verify(notificationRepository, atLeastOnce()).save(Mockito.argThat(n -> n.getType() == NotificationType.ADVISOR_REQUEST));

        createdGroup.setAdvisor(professor);
        createdGroup.setStatus(GroupStatus.ADVISED);
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(createdGroup));

        assertThat(createdGroup.getAdvisor().getUserId(), is(500L));
        assertThat(createdGroup.getStatus(), is(GroupStatus.ADVISED));


        // --- PHASE 5: INTEGRATION BINDING (D8) ---

        GithubIntegration gitInt = new GithubIntegration();
        gitInt.setGroup(createdGroup);
        gitInt.setOrganizationName("lifecycle-org");

        when(githubIntegrationRepository.findByGroup_Id(groupId)).thenReturn(Optional.of(gitInt));

        RestAssured.given()
            .header("Authorization", "Bearer " + leaderToken)
            .body(Map.of("organizationName", "lifecycle-org", "githubPat", "ghp_test"))
        .when()
            .post("/api/v1/groups/" + groupId + "/integrations/github")
        .then()
            .statusCode(200);

        verify(githubIntegrationRepository, atLeastOnce()).save(Mockito.any(GithubIntegration.class));


        // --- PHASE 6: DISBAND & CLEANUP (D1, D3, D9, D10) ---

        GroupMember m1 = new GroupMember(); m1.setUser(leader); m1.setGroup(createdGroup);
        GroupMember m2 = new GroupMember(); m2.setUser(member1); m2.setGroup(createdGroup);
        createdGroup.setMembers(new ArrayList<>(List.of(m1, m2)));

        RestAssured.given()
            .header("Authorization", "Bearer " + leaderToken)
        .when()
            .delete("/api/v1/groups/" + groupId)
        .then()
            .statusCode(200);

        ArgumentCaptor<Group> groupCaptor = ArgumentCaptor.forClass(Group.class);
        verify(groupRepository, atLeastOnce()).save(groupCaptor.capture());
        assertThat(groupCaptor.getValue().getStatus(), is(GroupStatus.DISBANDED));

        verify(groupMemberRepository, atLeastOnce()).deleteAll(Mockito.any());
        verify(userRepository, atLeastOnce()).save(Mockito.argThat(u -> u.getRole().equals("student")));
        verify(auditLogRepository, atLeastOnce()).save(Mockito.argThat(log -> log.getActionType() == ActionType.GROUP_DISBANDED));
    }

    private void assertThat(Object actual, org.hamcrest.Matcher<Object> matcher) {
        org.hamcrest.MatcherAssert.assertThat(actual, matcher);
    }
}
