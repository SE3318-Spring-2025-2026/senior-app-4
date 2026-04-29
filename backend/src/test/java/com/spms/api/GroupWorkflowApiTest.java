package com.spms.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spms.backend.controller.GroupController;
import com.spms.backend.dto.request.GroupCreateRequestDto;
import com.spms.backend.dto.request.GroupUpdateRequestDto;
import com.spms.backend.dto.request.InviteMemberRequestDto;
import com.spms.backend.dto.response.GroupResponseDto;
import com.spms.backend.dto.response.MemberResponseDto;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.exception.ForbiddenException;
import com.spms.backend.exception.GlobalExceptionHandler;
import com.spms.backend.repository.AuditLogRepository;
import com.spms.backend.service.GroupService;
import com.spms.backend.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Issue #37 — Group Workflow Controller Tests.
 *
 * Pure Mockito unit tests. No InMemory repos, no Spring context,
 * no DB. Scope: exactly the 8 deliverable requirements across 7 tests.
 */
@ExtendWith(MockitoExtension.class)
class GroupWorkflowControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private GroupService groupService;

    @Mock
    private MemberService memberService;

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private GroupController groupController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(groupController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════
    //  Deliverable 1: POST /groups → 201: Creator becomes leader
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("POST /groups → 201: Creator becomes leader")
    void createGroup_creatorBecomesLeader() throws Exception {
        GroupCreateRequestDto request = new GroupCreateRequestDto("Test Group");
        GroupResponseDto response = new GroupResponseDto(
                1L, "Test Group", 1L, null, "FORMING", 1, Instant.now());

        when(groupService.createGroup(any(GroupCreateRequestDto.class), eq(1L)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/groups")
                        .requestAttr("jwt_userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.groupName").value("Test Group"))
                .andExpect(jsonPath("$.leaderId").value(1))
                .andExpect(jsonPath("$.status").value("FORMING"))
                .andExpect(jsonPath("$.memberCount").value(1));

        verify(groupService).createGroup(any(GroupCreateRequestDto.class), eq(1L));
    }

    // ═══════════════════════════════════════════════════════════════
    //  Deliverable 2: GET /groups → 200: Created group appears in list
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("GET /groups → 200: Created group appears in list")
    void listGroups_containsCreatedGroup() throws Exception {
        GroupResponseDto dto = new GroupResponseDto(
                1L, "Existing Group", 1L, null, "FORMING", 1, Instant.now());
        Page<GroupResponseDto> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1);

        when(groupService.getGroups(any(Pageable.class), eq(1L), eq("student"), any(), any(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/groups")
                        .requestAttr("jwt_userId", 1L)
                        .requestAttr("jwt_role", "student"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].groupName").value("Existing Group"));
    }

    // ═══════════════════════════════════════════════════════════════
    //  Deliverable 3 & 4 (Combined): POST /members & GET /members → Correct roles
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Workflow: POST /members & GET /members → Correct roles")
    void memberWorkflow_assignsMemberRole() throws Exception {
        // 1. Deliverable 3: POST /members -> Verify invite sent
        doNothing().when(memberService).inviteMember(eq(1L), eq(2L), eq(1L));
        InviteMemberRequestDto inviteRequest = new InviteMemberRequestDto(2L);

        mockMvc.perform(post("/api/v1/groups/1/members")
                        .requestAttr("jwt_userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inviteRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        // 2. Deliverable 4: GET /members -> Verify roles
        List<MemberResponseDto> members = List.of(
                new MemberResponseDto(1L, "11111111111", "Leader Student", "LEADER"),
                new MemberResponseDto(2L, "22222222222", "Student A", "MEMBER"));

        when(groupService.getGroupMembers(eq(1L))).thenReturn(members);

        mockMvc.perform(get("/api/v1/groups/1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.userId==1)].role").value("LEADER"))
                .andExpect(jsonPath("$[?(@.userId==2)].role").value("MEMBER"));
    }

    // ═══════════════════════════════════════════════════════════════
    //  Deliverable 5: POST /groups → 400: Duplicate student
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("POST /groups → 400: Duplicate student (already in a group)")
    void createGroup_duplicateStudent_returns400() throws Exception {
        when(groupService.createGroup(any(GroupCreateRequestDto.class), eq(1L)))
                .thenThrow(new BadRequestException("This student is already a member of another group."));

        GroupCreateRequestDto request = new GroupCreateRequestDto("Another Group");

        mockMvc.perform(post("/api/v1/groups")
                        .requestAttr("jwt_userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("already a member")));
    }

    // ═══════════════════════════════════════════════════════════════
    //  Deliverable 6: POST /members → 400: Non-existent studentId
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("POST /groups/{id}/members → 400: Non-existent studentId")
    void inviteMember_nonExistentStudent_returns400() throws Exception {
        doThrow(new BadRequestException("Student not found."))
                .when(memberService).inviteMember(eq(1L), eq(999L), eq(1L));

        InviteMemberRequestDto request = new InviteMemberRequestDto(999L);

        mockMvc.perform(post("/api/v1/groups/1/members")
                        .requestAttr("jwt_userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Student not found."));
    }

    // ═══════════════════════════════════════════════════════════════
    //  Deliverable 7: POST /members → 400: Group is full
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("POST /groups/{id}/members → 400: Group is full")
    void inviteMember_groupFull_returns400() throws Exception {
        doThrow(new BadRequestException("Group member limit reached."))
                .when(memberService).inviteMember(eq(1L), eq(2L), eq(1L));

        InviteMemberRequestDto request = new InviteMemberRequestDto(2L);

        mockMvc.perform(post("/api/v1/groups/1/members")
                        .requestAttr("jwt_userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("limit reached")));
    }

    // ═══════════════════════════════════════════════════════════════
    //  Deliverable 8: PUT /groups/{id} → 403: Non-leader cannot update
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("PUT /groups/{id} → 403: Non-leader cannot update")
    void updateGroup_nonLeader_returns403() throws Exception {
        doThrow(new ForbiddenException("Only the group leader can perform this action."))
                .when(groupService).updateGroupName(eq(1L), any(GroupUpdateRequestDto.class), eq(2L));

        GroupUpdateRequestDto request = new GroupUpdateRequestDto("New Name");

        mockMvc.perform(put("/api/v1/groups/1")
                        .requestAttr("jwt_userId", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("group leader")));
    }
}
