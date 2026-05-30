package com.spms.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spms.backend.dto.request.AdvisorRequestDecisionDto;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.exception.ForbiddenException;
import com.spms.backend.exception.GlobalExceptionHandler; 
import com.spms.backend.service.GroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdvisorApprovalControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GroupService groupService;

    @InjectMocks
    private ProfessorController professorController;

    private ObjectMapper objectMapper;
    
    private final Long PROFESSOR_ID = 123L; 
    private final Long GROUP_ID = 41L;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        
        mockMvc = MockMvcBuilders.standaloneSetup(professorController)
                .setControllerAdvice(new GlobalExceptionHandler()) 
                .build();
    }

    @Test
    @DisplayName("PATCH /advisor-requests → 200: Successful approval triggers service call")
    void approveRequest_triggersServiceCall() throws Exception {
        AdvisorRequestDecisionDto requestDto = new AdvisorRequestDecisionDto(GROUP_ID, "approved");

        mockMvc.perform(patch("/api/v1/professors/advisor-requests")
                        .requestAttr("jwt_userId", PROFESSOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk());

        verify(groupService).handleAdvisorRequestDecision(PROFESSOR_ID, GROUP_ID, "approved");
    }

    @Test
    @DisplayName("PATCH /advisor-requests → 400: Double approve returns clear error")
    void doubleApprove_returns400() throws Exception {
        AdvisorRequestDecisionDto requestDto = new AdvisorRequestDecisionDto(GROUP_ID, "approved");

        doThrow(new BadRequestException("This request has already been processed."))
                .when(groupService).handleAdvisorRequestDecision(PROFESSOR_ID, GROUP_ID, "approved");

        mockMvc.perform(patch("/api/v1/professors/advisor-requests")
                        .requestAttr("jwt_userId", PROFESSOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("already been processed")));
    }

    @Test
    @DisplayName("PATCH /advisor-requests → 400: Already rejected request cannot be approved")
    void alreadyRejected_returns400() throws Exception {
        AdvisorRequestDecisionDto requestDto = new AdvisorRequestDecisionDto(GROUP_ID, "approved");

        doThrow(new BadRequestException("Cannot approve a rejected request."))
                .when(groupService).handleAdvisorRequestDecision(PROFESSOR_ID, GROUP_ID, "approved");

        mockMvc.perform(patch("/api/v1/professors/advisor-requests")
                        .requestAttr("jwt_userId", PROFESSOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("rejected request")));
    }

    @Test
    @DisplayName("PATCH /advisor-requests → 400: Group already has an advisor")
    void approveGroupWithAdvisor_returns400() throws Exception {
        AdvisorRequestDecisionDto requestDto = new AdvisorRequestDecisionDto(GROUP_ID, "approved");

        doThrow(new BadRequestException("This group already has an advisor."))
                .when(groupService).handleAdvisorRequestDecision(PROFESSOR_ID, GROUP_ID, "approved");

        mockMvc.perform(patch("/api/v1/professors/advisor-requests")
                        .requestAttr("jwt_userId", PROFESSOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("already has an advisor")));
    }

    @Test
    @DisplayName("PATCH /advisor-requests → 403: Student cannot make decision")
    void studentDecision_returns403() throws Exception {
        Long STUDENT_ID = 999L;
        AdvisorRequestDecisionDto requestDto = new AdvisorRequestDecisionDto(GROUP_ID, "approved");

        doThrow(new ForbiddenException("Only professors can approve advisor requests."))
                .when(groupService).handleAdvisorRequestDecision(STUDENT_ID, GROUP_ID, "approved");

        mockMvc.perform(patch("/api/v1/professors/advisor-requests")
                        .requestAttr("jwt_userId", STUDENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("Only professors")));
    }
}