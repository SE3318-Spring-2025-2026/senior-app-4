package com.spms.api;

import com.spms.backend.controller.ProfessorController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spms.backend.dto.request.AdvisorRequestDecisionDto;
import com.spms.backend.exception.GlobalExceptionHandler;
import com.spms.backend.model.Group;
import com.spms.backend.model.GroupStatus;
import com.spms.backend.model.User;
import com.spms.backend.model.notification.Notification;
import com.spms.backend.model.notification.NotificationStatus;
import com.spms.backend.model.notification.NotificationType;
import com.spms.backend.repository.*;
import com.spms.backend.service.impl.GroupServiceImpl;
import com.spms.backend.service.NotificationService;
import com.spms.backend.service.StudentAuthorizationService;
import com.spms.backend.client.JiraApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Isolated Controller Test for Advisor Approval and Auto-Reject logic.
 * Uses Standalone MockMvc + InMemory Repositories.
 */
class AdvisorApprovalControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    // Repositories
    private InMemoryUserRepository userRepository;
    private InMemoryGroupRepository groupRepository;
    private InMemoryNotificationRepository notificationRepository;
    private InMemoryGroupMemberRepository groupMemberRepository;
    private InMemoryAuditLogRepository auditLogRepository;
    private InMemoryJiraIntegrationRepository jiraRepo;
    private InMemoryGithubIntegrationRepository githubRepo;

    // Services
    private GroupServiceImpl groupService;
    private NotificationService notificationService;
    private StudentAuthorizationService authService;
    private JiraApiClient jiraApiClient;

    // Test Data
    private User professorA;
    private User professorB;
    private User professorC;
    private User leader;
    private Group testGroup;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        // 1. Repositories
        userRepository = new InMemoryUserRepository();
        groupRepository = new InMemoryGroupRepository();
        notificationRepository = new InMemoryNotificationRepository();
        groupMemberRepository = new InMemoryGroupMemberRepository();
        auditLogRepository = new InMemoryAuditLogRepository();
        jiraRepo = new InMemoryJiraIntegrationRepository();
        githubRepo = new InMemoryGithubIntegrationRepository();

        // 2. Mocks & Stubs
        notificationService = Mockito.mock(NotificationService.class);
        
        // Manual stub for JiraApiClient to avoid Mockito/Java 24 ByteBuddy issues
        jiraApiClient = new JiraApiClient(org.springframework.web.client.RestClient.builder()) {
            @Override
            public boolean validateSpaceConnection(String u, String k, String p) { return true; }
        };

        authService = new StudentAuthorizationService(userRepository, groupMemberRepository, groupRepository);

        // 3. Real Service with In-Memory Repos
        groupService = new GroupServiceImpl(
                groupRepository,
                groupMemberRepository,
                userRepository,
                notificationService,
                authService,
                jiraRepo,
                githubRepo,
                jiraApiClient,
                notificationRepository,
                auditLogRepository
        );

        // 4. Controller Setup
        ProfessorController professorController = new ProfessorController(null, groupService);
        mockMvc = MockMvcBuilders.standaloneSetup(professorController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        // 5. Seed Data
        professorA = createAndSaveUser(101L, "Professor A", "professor");
        professorB = createAndSaveUser(102L, "Professor B", "professor");
        professorC = createAndSaveUser(103L, "Professor C", "professor");
        leader = createAndSaveUser(201L, "Leader Student", "student");

        testGroup = new Group();
        testGroup.setGroupName("Test Group");
        testGroup.setLeader(leader);
        testGroup.setStatus(GroupStatus.FORMED);
        groupRepository.save(testGroup);
    }

    private User createAndSaveUser(Long id, String name, String role) {
        User user = new User();
        user.setUserId(id);
        user.setFullName(name);
        user.setRole(role);
        return userRepository.save(user);
    }

    /**
     * AC 1: Auto-Reject Dominos
     * Group has requests to A, B, and C. B approves -> A and C must be REJECTED.
     */
    @Test
    @DisplayName("PATCH /advisor-requests → 200: Auto-rejects other pending requests (ns_f10)")
    void autoRejectDomino_rejectsOtherProfessors() throws Exception {
        // Setup: 3 pending requests
        Notification reqA = createRequest(professorA);
        Notification reqB = createRequest(professorB);
        Notification reqC = createRequest(professorC);

        AdvisorRequestDecisionDto decision = new AdvisorRequestDecisionDto(testGroup.getId(), "approved");

        // Execute: Professor B approves
        mockMvc.perform(patch("/api/v1/professors/advisor-requests")
                        .requestAttr("jwt_userId", professorB.getUserId())
                        .requestAttr("jwt_role", "professor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decision)))
                .andExpect(status().isOk());

        // Verify: reqB is ACCEPTED, reqA and reqC are REJECTED
        assertEquals(NotificationStatus.ACCEPTED, notificationRepository.findById(reqB.getId()).get().getStatus());
        assertEquals(NotificationStatus.REJECTED, notificationRepository.findById(reqA.getId()).get().getStatus());
        assertEquals(NotificationStatus.REJECTED, notificationRepository.findById(reqC.getId()).get().getStatus());
    }

    /**
     * AC 2: Mükerrer İşlem Engeli
     */
    @Test
    @DisplayName("PATCH /advisor-requests → 400: Error when responding to already processed request")
    void approveAlreadyProcessed_returns400() throws Exception {
        Notification req = createRequest(professorB);
        req.setStatus(NotificationStatus.ACCEPTED); // Already processed
        notificationRepository.save(req);

        AdvisorRequestDecisionDto decision = new AdvisorRequestDecisionDto(testGroup.getId(), "approved");

        mockMvc.perform(patch("/api/v1/professors/advisor-requests")
                        .requestAttr("jwt_userId", professorB.getUserId())
                        .requestAttr("jwt_role", "professor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decision)))
                .andExpect(status().isBadRequest());
    }

    /**
     * AC 3: Çift Danışman Engeli
     */
    @Test
    @DisplayName("PATCH /advisor-requests → 400: Error when group already has another advisor")
    void approveGroupWithExistingAdvisor_returns400() throws Exception {
        // Group already has Professor A as advisor
        testGroup.setAdvisor(professorA);
        groupRepository.save(testGroup);

        createRequest(professorB); // Professor B tries to approve

        AdvisorRequestDecisionDto decision = new AdvisorRequestDecisionDto(testGroup.getId(), "approved");

        mockMvc.perform(patch("/api/v1/professors/advisor-requests")
                        .requestAttr("jwt_userId", professorB.getUserId())
                        .requestAttr("jwt_role", "professor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decision)))
                .andExpect(status().isBadRequest());
    }

    /**
     * AC 4: Bildirim Kontrolü
     */
    @Test
    @DisplayName("PATCH /advisor-requests → Verify NotificationService notifications for auto-rejected professors")
    void autoReject_notifiesProfessors() throws Exception {
        createAndSaveUser(101L, "Professor A", "professor");
        Notification reqA = createRequest(professorA);
        createRequest(professorB);

        AdvisorRequestDecisionDto decision = new AdvisorRequestDecisionDto(testGroup.getId(), "approved");

        mockMvc.perform(patch("/api/v1/professors/advisor-requests")
                        .requestAttr("jwt_userId", professorB.getUserId())
                        .requestAttr("jwt_role", "professor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decision)))
                .andExpect(status().isOk());

        // Verify that notificationService.createSystemAlert was called for professorA
        Mockito.verify(notificationService).createSystemAlert(
                eq(professorA.getUserId()),
                argThat(msg -> msg.contains("automatically cancelled")),
                eq("ADVISOR_AUTO_REJECT"),
                anyString()
        );
    }

    /**
     * AC 5: Veri Doğruluğu
     */
    @Test
    @DisplayName("PATCH /advisor-requests → Verify advisor_id is set correctly after approval")
    void approval_updatesAdvisorId() throws Exception {
        createRequest(professorB);

        AdvisorRequestDecisionDto decision = new AdvisorRequestDecisionDto(testGroup.getId(), "approved");

        mockMvc.perform(patch("/api/v1/professors/advisor-requests")
                        .requestAttr("jwt_userId", professorB.getUserId())
                        .requestAttr("jwt_role", "professor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decision)))
                .andExpect(status().isOk());

        Group updatedGroup = groupRepository.findById(testGroup.getId()).get();
        assertEquals(professorB.getUserId(), updatedGroup.getAdvisor().getUserId());
        assertEquals(GroupStatus.ADVISED, updatedGroup.getStatus());
    }

    /**
     * AC 6: Güvenlik/Yetki
     */
    @Test
    @DisplayName("PATCH /advisor-requests → 403: Forbidden if user role is student")
    void studentDecision_returns403() throws Exception {
        createRequest(professorB);
        AdvisorRequestDecisionDto decision = new AdvisorRequestDecisionDto(testGroup.getId(), "approved");

        mockMvc.perform(patch("/api/v1/professors/advisor-requests")
                        .requestAttr("jwt_userId", leader.getUserId())
                        .requestAttr("jwt_role", "student") // WRONG ROLE
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decision)))
                .andExpect(status().isForbidden());
    }

    private Notification createRequest(User toProfessor) {
        Notification req = new Notification();
        req.setGroupId(testGroup.getId());
        req.setType(NotificationType.ADVISOR_REQUEST);
        req.setStatus(NotificationStatus.PENDING);
        req.setToUser(toProfessor);
        req.setFromUser(leader);
        return notificationRepository.save(req);
    }
}
