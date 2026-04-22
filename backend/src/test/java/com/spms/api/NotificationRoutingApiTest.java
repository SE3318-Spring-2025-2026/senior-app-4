package com.spms.api;

import com.spms.backend.controller.NotificationController;
import com.spms.backend.controller.GroupController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spms.backend.dto.request.AdvisorRequestDto;
import com.spms.backend.dto.request.InviteMemberRequestDto;
import com.spms.backend.dto.request.NotificationRespondRequestDto;
import com.spms.backend.exception.GlobalExceptionHandler;
import com.spms.backend.model.*;
import com.spms.backend.model.notification.Notification;
import com.spms.backend.model.notification.NotificationStatus;
import com.spms.backend.model.notification.NotificationType;
import com.spms.backend.repository.*;
import com.spms.backend.service.impl.MemberServiceImpl;
import com.spms.backend.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;

import java.util.Collections;
import java.util.Comparator;
import com.spms.backend.exception.UnauthorizedException;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Isolated logic tests for Notification Routing and User Isolation.
 * 
 * Verifies that notifications reach the correct recipients, responses update
 * system state correctly, and users cannot see each other's data.
 */
class NotificationRoutingControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    // Repositories
    private InMemoryUserRepository userRepository;
    private InMemoryNotificationRepository notificationRepository;
    private InMemoryGroupRepository groupRepository;
    private InMemoryGroupMemberRepository groupMemberRepository;
    private InMemoryAuditLogRepository auditLogRepository;

    // Services
    private NotificationServiceImpl notificationService;
    private MemberServiceImpl memberService;

    // Test Data
    private User leader;
    private User studentA;
    private User studentB;
    private User professor;
    private Group group;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        // 1. Repositories
        userRepository = new InMemoryUserRepository();
        notificationRepository = new InMemoryNotificationRepository();
        groupRepository = new InMemoryGroupRepository();
        groupMemberRepository = new InMemoryGroupMemberRepository();
        auditLogRepository = new InMemoryAuditLogRepository();

        // 2. Wiring Services
        // MemberServiceImpl takes repos directly
        memberService = new MemberServiceImpl(
                groupRepository,
                groupMemberRepository,
                userRepository,
                notificationRepository
        );

        // NotificationServiceImpl takes repos + MemberService
        notificationService = new NotificationServiceImpl(
                notificationRepository,
                userRepository,
                memberService
        );

        // 3. Controllers
        NotificationController notificationController = new NotificationController(notificationService);
        GroupController groupController = new GroupController(null, memberService, auditLogRepository);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        // Setup Standalone MockMvc with both controllers
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController, groupController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        // 4. Seed Data
        leader = createStudent(1L, "Leader User");
        studentA = createStudent(2L, "Student A");
        studentB = createStudent(3L, "Student B");
        professor = createUser(4L, "Dr. Professor", "professor");

        group = new Group();
        group.setGroupName("Test Squad");
        group.setLeader(leader);
        group.setStatus(GroupStatus.FORMING);
        groupRepository.save(group);

        // Leader is a member
        GroupMember lm = new GroupMember();
        lm.setGroup(group); lm.setUser(leader); lm.setRole(GroupRole.LEADER);
        groupMemberRepository.save(lm);
        group.getMembers().add(lm);
    }

    private User createStudent(Long id, String name) {
        return createUser(id, name, "student");
    }

    private User createUser(Long id, String name, String role) {
        User u = new User();
        u.setUserId(id);
        u.setFullName(name);
        u.setRole(role);
        u.setEmail(name.replace(" ", ".") + "@test.com");
        return userRepository.save(u);
    }

    // ═══════════════════════════════════════════════════════════════
    //  TEST SCENARIOS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Scenario 1: Routing - Invite Student A -> verify Student A gets it, Student B doesn't.
     */
    @Test
    @DisplayName("Routing: Only invited student sees the MEMBERSHIP_INVITE")
    void invitationRouting_onlyTargetSeesNotification() throws Exception {
        InviteMemberRequestDto inviteRequest = new InviteMemberRequestDto(studentA.getUserId());

        // POST /api/v1/groups/{id}/members (GroupController)
        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/members")
                        .requestAttr("jwt_userId", leader.getUserId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inviteRequest)))
                .andExpect(status().isCreated());

        // Verify Student A sees it
        mockMvc.perform(get("/api/v1/notifications")
                        .requestAttr("jwt_userId", studentA.getUserId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].type").value("membership_invite"));

        // Verify Student B does NOT see it
        mockMvc.perform(get("/api/v1/notifications")
                        .requestAttr("jwt_userId", studentB.getUserId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    /**
     * Scenario 2: Accept Flow - Accept invite -> added to group confirmed by GET /members.
     */
    @Test
    @DisplayName("Accept Flow: POST /respond 'accept' -> Student joins group")
    void acceptInvite_updatesMembership() throws Exception {
        // 1. Invite
        memberService.inviteMember(group.getId(), studentA.getUserId(), leader.getUserId());
        Notification notif = notificationRepository.findAll().get(0);

        // 2. Accept
        NotificationRespondRequestDto respond = new NotificationRespondRequestDto("accept");
        mockMvc.perform(post("/api/v1/notifications/" + notif.getId() + "/respond")
                        .requestAttr("jwt_userId", studentA.getUserId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(respond)))
                .andExpect(status().isOk());

        // 3. Verify membership list (via repo - but we can use GroupController if needed)
        // GroupMember count should be 2 (Leader + A)
        assert(groupMemberRepository.findAll().size() == 2);
        boolean studentAInGroup = groupMemberRepository.findAll().stream()
                .anyMatch(m -> m.getUser().getUserId().equals(studentA.getUserId()));
        assert(studentAInGroup);
    }

    /**
     * Scenario 3: Reject Flow - Reject -> no modify.
     */
    @Test
    @DisplayName("Reject Flow: POST /respond 'reject' -> Membership list unchanged")
    void rejectInvite_noMembershipChange() throws Exception {
        memberService.inviteMember(group.getId(), studentB.getUserId(), leader.getUserId());
        Notification notif = notificationRepository.findAll().get(0);

        NotificationRespondRequestDto respond = new NotificationRespondRequestDto("reject");
        mockMvc.perform(post("/api/v1/notifications/" + notif.getId() + "/respond")
                        .requestAttr("jwt_userId", studentB.getUserId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(respond)))
                .andExpect(status().isOk());

        // Verify membership list still only has leader
        assert(groupMemberRepository.findAll().size() == 1);
        assert(groupMemberRepository.findAll().get(0).getUser().getUserId().equals(leader.getUserId()));
    }

    /**
     * Scenario 4: Advisor Request - Leader requests professor -> verify only professor gets it.
     */
    @Test
    @DisplayName("Advisor Request: Only target professor receives the request")
    void advisorRequest_onlyProfessorSeesNotification() throws Exception {
        AdvisorRequestDto request = new AdvisorRequestDto(professor.getUserId());

        // POST /api/v1/groups/{id}/advisor-request (NotificationController)
        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/advisor-request")
                        .requestAttr("jwt_userId", leader.getUserId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Professor sees 1 notification
        mockMvc.perform(get("/api/v1/notifications")
                        .requestAttr("jwt_userId", professor.getUserId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].type").value("advisor_request"));

        // Student A (not involved) sees 0
        mockMvc.perform(get("/api/v1/notifications")
                        .requestAttr("jwt_userId", studentA.getUserId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    /**
     * Scenario 5: Isolation (Critical) - User A cannot see User B's notifications.
     */
    @Test
    @DisplayName("Isolation: Users A and B have completely separate notification lists")
    void isolation_usersCannotSeeEachOtherNotifications() throws Exception {
        // Invite Student A
        memberService.inviteMember(group.getId(), studentA.getUserId(), leader.getUserId());
        // Invite Student B
        memberService.inviteMember(group.getId(), studentB.getUserId(), leader.getUserId());

        // Student A check
        mockMvc.perform(get("/api/v1/notifications")
                        .requestAttr("jwt_userId", studentA.getUserId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].toUserId").value(studentA.getUserId()));

        // Student B check
        mockMvc.perform(get("/api/v1/notifications")
                        .requestAttr("jwt_userId", studentB.getUserId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].toUserId").value(studentB.getUserId()));
    }

    /**
     * Scenario 6: Cross-User Response Prevention - User A tries to respond to User B's notification.
     */
    @Test
    @DisplayName("Security: User A cannot respond to User B's notification -> 403/Forbidden")
    void crossUserResponse_isForbidden() throws Exception {
        // Invite A
        memberService.inviteMember(group.getId(), studentA.getUserId(), leader.getUserId());
        Notification notifA = notificationRepository.findAll().get(0);

        // Student B tries to accept A's invite
        NotificationRespondRequestDto respond = new NotificationRespondRequestDto("accept");
        mockMvc.perform(post("/api/v1/notifications/" + notifA.getId() + "/respond")
                        .requestAttr("jwt_userId", studentB.getUserId()) // WRONG USER
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(respond)))
                .andExpect(status().isForbidden()); // Matches AC requirement for 403
    }
}
