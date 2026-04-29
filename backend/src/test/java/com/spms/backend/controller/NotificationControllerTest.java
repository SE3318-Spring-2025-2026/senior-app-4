package com.spms.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spms.backend.dto.response.NotificationDto;
import com.spms.backend.service.NotificationService;
import com.spms.backend.service.JwtValidationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = NotificationController.class)
@AutoConfigureMockMvc(addFilters = false) // Ignore security filters for pure controller logic testing
public class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private JwtValidationService jwtValidationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/v1/notifications - Should return paginated notifications")
    void getNotifications_Returns200AndData() throws Exception {
        // Arrange
        NotificationDto dto = new NotificationDto(
                1L, "TEST_TYPE", "Test Message", "PENDING", true,
                100L, "John Doe", 200L, 300L, Instant.now()
        );

        Mockito.when(notificationService.getNotifications(eq(10L), eq("ALL"), any(Pageable.class)))
               .thenReturn(new PageImpl<>(List.of(dto)));

        // Act & Assert
        mockMvc.perform(get("/api/v1/notifications")
                .param("page", "0")
                .param("size", "10")
                .param("readStatus", "ALL")
                .requestAttr("jwt_userId", 10L)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].type").value("TEST_TYPE"))
                .andExpect(jsonPath("$.content[0].message").value("Test Message"));
    }

    @Test
    @DisplayName("PATCH /api/v1/notifications/{id}/read - Should mark notification as read")
    void markAsRead_Returns200() throws Exception {
        // Arrange
        Mockito.doNothing().when(notificationService).markAsRead(1L, 10L);

        // Act & Assert
        mockMvc.perform(patch("/api/v1/notifications/1/read")
                .requestAttr("jwt_userId", 10L)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }
}
