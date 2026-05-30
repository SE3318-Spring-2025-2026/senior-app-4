package com.spms.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spms.backend.dto.request.AdvisorRequestDecisionDto;
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

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdvisorApprovalVoidApiTest {

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
    @DisplayName("PATCH /advisor-requests → 200: Manual rejection (Void Response)")
    void rejectRequest_triggersServiceCallSuccessfully() throws Exception {
    
        AdvisorRequestDecisionDto rejectDto = new AdvisorRequestDecisionDto(GROUP_ID, "rejected");

        mockMvc.perform(patch("/api/v1/professors/advisor-requests")
                        .requestAttr("jwt_userId", PROFESSOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectDto)))
                .andExpect(status().isOk());

        verify(groupService).handleAdvisorRequestDecision(PROFESSOR_ID, GROUP_ID, "rejected");
    }

    @Test
    @DisplayName("PATCH /advisor-requests → 200: Verify Auto-Reject trigger on Approval")
    void approveRequest_triggersAutoRejectLogicInService() throws Exception {

        AdvisorRequestDecisionDto approveDto = new AdvisorRequestDecisionDto(GROUP_ID, "approved");

        mockMvc.perform(patch("/api/v1/professors/advisor-requests")
                        .requestAttr("jwt_userId", PROFESSOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveDto)))
                .andExpect(status().isOk());

        verify(groupService).handleAdvisorRequestDecision(PROFESSOR_ID, GROUP_ID, "approved");
    }
}