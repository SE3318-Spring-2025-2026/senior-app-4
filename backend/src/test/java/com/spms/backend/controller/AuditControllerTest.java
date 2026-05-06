package com.spms.backend.controller;

import com.spms.backend.model.ActionType;
import com.spms.backend.model.AuditLog;
import com.spms.backend.repository.AuditLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@org.junit.jupiter.api.Disabled("AuditController is not yet implemented")
public class AuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // We mock the repository directly. Once the AuditController and Service are implemented,
    // they should wire up to this repository. This allows the test to compile now and fail gracefully (404).
    @MockBean
    private AuditLogRepository auditLogRepository;

    @Test
    @DisplayName("GET /api/v1/audit-logs - Should return paginated audit logs for ADMIN")
    void getAuditLogs_AsAdmin_Returns200AndData() throws Exception {
        // Arrange
        AuditLog log1 = new AuditLog();
        log1.setId(1L);
        log1.setActionType(ActionType.GROUP_CREATED);
        log1.setUserId(100L);
        log1.setEventDetails("Group Created by Admin");

        AuditLog log2 = new AuditLog();
        log2.setId(2L);
        log2.setActionType(ActionType.ADVISOR_ASSIGNED);
        log2.setUserId(101L);
        log2.setEventDetails("Advisor Assigned");

        Mockito.when(auditLogRepository.findAll(any(PageRequest.class)))
               .thenReturn(new PageImpl<>(List.of(log1, log2)));

        // Act & Assert
        // This will currently return 404 until the backend team implements the AuditController
        mockMvc.perform(get("/api/v1/audit-logs")
                .param("page", "0")
                .param("size", "10")
                .requestAttr("jwt_role", "admin")
                .requestAttr("jwt_userId", 100L)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].actionType").value("GROUP_CREATED"))
                .andExpect(jsonPath("$.content[1].actionType").value("ADVISOR_ASSIGNED"));
    }

    @Test
    @DisplayName("GET /api/v1/audit-logs - Filtering by ActionType")
    void getAuditLogs_FilterByActionType_ReturnsFilteredData() throws Exception {
        // Arrange
        AuditLog log = new AuditLog();
        log.setId(1L);
        log.setActionType(ActionType.ADVISOR_REJECTED);
        
        // Assuming the repository or service handles the filtering logic, we mock the expected behavior
        Mockito.when(auditLogRepository.findByActionType(Mockito.eq(ActionType.ADVISOR_REJECTED), any(PageRequest.class)))
               .thenReturn(new PageImpl<>(List.of(log)));

        // Act & Assert
        mockMvc.perform(get("/api/v1/audit-logs")
                .param("actionType", "ADVISOR_REJECTED")
                .requestAttr("jwt_role", "coordinator")
                .requestAttr("jwt_userId", 101L)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].actionType").value("ADVISOR_REJECTED"));
    }

    @Test
    @DisplayName("GET /api/v1/audit-logs - Should deny access for STUDENT")
    void getAuditLogs_AsStudent_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/audit-logs")
                .requestAttr("jwt_role", "student")
                .requestAttr("jwt_userId", 102L)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
