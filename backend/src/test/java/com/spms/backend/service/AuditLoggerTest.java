package com.spms.backend.service;

import com.spms.backend.annotation.AuditableOperation;
import com.spms.backend.aop.AuditLogAspect;
import com.spms.backend.model.ActionType;
import com.spms.backend.model.AuditLog;
import com.spms.backend.repository.AuditLogRepository;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuditLoggerTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private AuditableOperation auditableOperation;

    @Mock
    private Signature signature;

    @InjectMocks
    private AuditLogAspect auditLogger; // We test the existing AuditLogAspect which serves as the "Audit Logger"

    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    @DisplayName("AuditLogger: Should log GROUP_CREATED action correctly")
    void log_GroupCreated_Success() {
        // Arrange
        request.setAttribute("jwt_userId", "100");
        request.setRemoteAddr("192.168.1.1");
        
        when(auditableOperation.actionType()).thenReturn(ActionType.GROUP_CREATED);
        when(auditableOperation.eventDetails()).thenReturn("New group formed");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("createGroup");

        // Act
        auditLogger.logAuditActivity(joinPoint, auditableOperation, null);

        // Assert
        ArgumentCaptor<AuditLog> logCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(logCaptor.capture());

        AuditLog savedLog = logCaptor.getValue();
        assertEquals(ActionType.GROUP_CREATED, savedLog.getActionType());
        assertEquals(100L, savedLog.getUserId());
        assertEquals("192.168.1.1", savedLog.getIpAddress());
        assertEquals("New group formed", savedLog.getEventDetails());
    }

    @Test
    @DisplayName("AuditLogger: Should log ADVISOR_ASSIGNED action correctly")
    void log_AdvisorAssigned_Success() {
        // Arrange
        request.setAttribute("jwt_userId", "101");
        
        when(auditableOperation.actionType()).thenReturn(ActionType.ADVISOR_ASSIGNED);
        when(auditableOperation.eventDetails()).thenReturn(""); // Empty details fallback check
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("assignAdvisor");

        // Act
        auditLogger.logAuditActivity(joinPoint, auditableOperation, null);

        // Assert
        ArgumentCaptor<AuditLog> logCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(logCaptor.capture());

        AuditLog savedLog = logCaptor.getValue();
        assertEquals(ActionType.ADVISOR_ASSIGNED, savedLog.getActionType());
        assertEquals(101L, savedLog.getUserId());
        assertNotNull(savedLog.getEventDetails());
        assertEquals("ADVISOR_ASSIGNED operation performed by User ID: 101", savedLog.getEventDetails());
    }
}
