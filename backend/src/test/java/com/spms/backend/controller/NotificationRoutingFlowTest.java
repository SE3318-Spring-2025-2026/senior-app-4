package com.spms.backend.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spms.backend.dto.request.AdvisorRequestCreateDto;
import com.spms.backend.dto.request.InviteMemberRequestDto;
import com.spms.backend.dto.request.NotificationRespondRequestDto;
import com.spms.backend.dto.response.NotificationDto;
import com.spms.backend.dto.response.MemberResponseDto;
import com.spms.backend.exception.GlobalExceptionHandler;
import com.spms.backend.exception.UnauthorizedException;
import com.spms.backend.service.NotificationService;
import com.spms.backend.service.MemberService;
import com.spms.backend.service.GroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class NotificationRoutingFlowTest {

    private MockMvc mockMvc;

    @Mock private NotificationService notificationService;
    @Mock private MemberService memberService;
    @Mock private GroupService groupService;

    @InjectMocks private NotificationController notificationController;
    @InjectMocks private GroupController groupController;

    private ObjectMapper objectMapper;

    private final Long LEADER_ID = 1L;
    private final Long STUDENT_A_ID = 2L;
    private final Long STUDENT_B_ID = 3L;
    private final Long PROFESSOR_ID = 5L;
    private final Long NOTIF_ID = 10L;
    private final Long GROUP_ID = 42L;
    private final Long OTHER_USER_ID = 99L;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController, groupController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Invite -> Notification goes only to target student, added to group upon acceptance")
    void membershipInvite_Accept_FullFlow() throws Exception {
        // 1. Leader invites Student A (201 Created)
        InviteMemberRequestDto inviteReq = new InviteMemberRequestDto(STUDENT_A_ID);
        mockMvc.perform(post("/api/v1/groups/{groupId}/members", GROUP_ID)
                        .requestAttr("jwt_userId", LEADER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inviteReq)))
                .andExpect(status().isCreated());

        // 2. Student A sees their notifications (1 MEMBERSHIP_INVITE)
        NotificationDto inviteNotif = new NotificationDto(
                1L, "MEMBERSHIP_INVITE", "Invitation message", "PENDING",
                false, LEADER_ID, "Leader", STUDENT_A_ID, GROUP_ID, Instant.now()
        );

        when(notificationService.getNotifications(eq(STUDENT_A_ID), nullable(String.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(inviteNotif), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/notifications")
                        .param("page", "0")
                        .param("size", "10")
                        .requestAttr("jwt_userId", STUDENT_A_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].type").value("MEMBERSHIP_INVITE"));

        // 3. Student A accepts the invitation
        NotificationRespondRequestDto acceptReq = new NotificationRespondRequestDto("accept");
        mockMvc.perform(post("/api/v1/notifications/{id}/respond", NOTIF_ID)
                        .requestAttr("jwt_userId", STUDENT_A_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(acceptReq)))
                .andExpect(status().isOk());
        verify(notificationService).respondToNotification(NOTIF_ID, "accept", STUDENT_A_ID);

        // 4. Leader views group member list, Student A is listed as "member"
        MemberResponseDto member = new MemberResponseDto(STUDENT_A_ID, "11111111111", "Student A", "member");
        when(groupService.getGroupMembers(GROUP_ID)).thenReturn(List.of(member));

        mockMvc.perform(get("/api/v1/groups/{id}/members", GROUP_ID)
                        .requestAttr("jwt_userId", LEADER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userId").value(STUDENT_A_ID.intValue()))
                .andExpect(jsonPath("$[0].role").value("member"));
    }

    @Test
    @DisplayName("Rejecting invite does not change the member list")
    void rejectInvite_doesNotAddMember() throws Exception {
        NotificationRespondRequestDto rejectReq = new NotificationRespondRequestDto("reject");
        mockMvc.perform(post("/api/v1/notifications/{id}/respond", NOTIF_ID)
                        .requestAttr("jwt_userId", STUDENT_B_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectReq)))
                .andExpect(status().isOk());
        verify(notificationService).respondToNotification(NOTIF_ID, "reject", STUDENT_B_ID);

        when(groupService.getGroupMembers(GROUP_ID)).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/groups/{id}/members", GROUP_ID)
                        .requestAttr("jwt_userId", LEADER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Advisor request goes only to the target professor")
    void advisorRequest_FullFlow() throws Exception {
        // Leader sends advisor request (POST /api/v1/advisor-requests)
        AdvisorRequestCreateDto advisorReq = new AdvisorRequestCreateDto(GROUP_ID.toString(), PROFESSOR_ID, "Would you be our advisor?");

        mockMvc.perform(post("/api/v1/advisor-requests")
                        .requestAttr("jwt_userId", LEADER_ID)
                        .requestAttr("jwt_role", "student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(advisorReq)))
                .andExpect(status().isCreated());

        // Professor receives the ADVISOR_REQUEST notification
        NotificationDto advisorNotif = new NotificationDto(
                2L, "ADVISOR_REQUEST", "Advisor request", "PENDING",
                false, LEADER_ID, "Leader", PROFESSOR_ID, GROUP_ID, Instant.now()
        );

        when(notificationService.getNotifications(eq(PROFESSOR_ID), nullable(String.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(advisorNotif), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/notifications")
                        .param("page", "0")
                        .param("size", "10")
                        .requestAttr("jwt_userId", PROFESSOR_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].type").value("ADVISOR_REQUEST"));

        // Student A should not see this notification
        when(notificationService.getNotifications(eq(STUDENT_A_ID), nullable(String.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
        
        mockMvc.perform(get("/api/v1/notifications")
                        .param("page", "0")
                        .param("size", "10")
                        .requestAttr("jwt_userId", STUDENT_A_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    @DisplayName("Notification isolation: User cannot see other users' notifications")
    void notificationIsolation_Guard() throws Exception {
        when(notificationService.getNotifications(eq(STUDENT_B_ID), nullable(String.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/api/v1/notifications")
                        .param("page", "0")
                        .param("size", "10")
                        .requestAttr("jwt_userId", STUDENT_B_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    @DisplayName("Unauthorized user cannot respond to notification (401)")
    void respondToNotification_Unauthorized() throws Exception {
        doThrow(new UnauthorizedException("You do not have permission to access this notification."))
                .when(notificationService).respondToNotification(anyLong(), anyString(), eq(OTHER_USER_ID));

        NotificationRespondRequestDto body = new NotificationRespondRequestDto("accept");
        mockMvc.perform(post("/api/v1/notifications/{id}/respond", NOTIF_ID)
                        .requestAttr("jwt_userId", OTHER_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }
}