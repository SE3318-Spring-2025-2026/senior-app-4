package com.spms.backend.service;

import com.spms.backend.exception.BadRequestException;
import com.spms.backend.exception.ForbiddenException;
import com.spms.backend.exception.NotFoundException;
import com.spms.backend.model.AuditLog;
import com.spms.backend.model.Group;
import com.spms.backend.model.User;
import com.spms.backend.repository.AuditLogRepository;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.service.impl.AdvisorAssignmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdvisorAssignmentServiceImplTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private NotificationService notificationService;

    private AdvisorAssignmentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdvisorAssignmentServiceImpl(groupRepository, auditLogRepository, notificationService);
    }

    @Test
    void releaseAdvisor_rejectsNonProfessorRole() {
        assertThrows(ForbiddenException.class,
                () -> service.releaseAdvisor(10L, 100L, "student"));

        verify(groupRepository, never()).findById(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void releaseAdvisor_rejectsNullRole() {
        assertThrows(ForbiddenException.class,
                () -> service.releaseAdvisor(10L, 100L, null));
    }

    @Test
    void releaseAdvisor_returns404WhenGroupMissing() {
        when(groupRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.releaseAdvisor(99L, 100L, "professor"));

        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void releaseAdvisor_returns400WhenGroupHasNoAdvisor() {
        Group group = groupWith(10L, leader(200L), null);
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));

        assertThrows(BadRequestException.class,
                () -> service.releaseAdvisor(10L, 100L, "professor"));

        verify(groupRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void releaseAdvisor_returns403WhenRequesterIsNotCurrentAdvisor() {
        User currentAdvisor = advisor(300L);
        Group group = groupWith(10L, leader(200L), currentAdvisor);
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));

        assertThrows(ForbiddenException.class,
                () -> service.releaseAdvisor(10L, 999L, "professor"));

        assertEquals(currentAdvisor, group.getAdvisor());
        verify(groupRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void releaseAdvisor_happyPath_clearsAdvisorWritesAuditAndNotifies() {
        User advisor = advisor(300L);
        Group group = groupWith(10L, leader(200L), advisor);
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));

        service.releaseAdvisor(10L, 300L, "PROFESSOR");

        assertNull(group.getAdvisor());
        verify(groupRepository).save(group);

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        AuditLog saved = auditCaptor.getValue();
        assertEquals("ADVISOR_RELEASED", saved.getAction());
        assertEquals("GROUP", saved.getEntityType());
        assertEquals(10L, saved.getEntityId());
        assertEquals(300L, saved.getUserId());

        verify(notificationService).createSystemAlert(
                eq(200L),
                eq("Your advisor has released your group. You may submit a new advisor request."),
                eq("ADVISOR_RELEASED"),
                eq("groupId=10"));
    }

    @Test
    void releaseAdvisor_stillSucceedsWhenNotificationFails() {
        User advisor = advisor(300L);
        Group group = groupWith(10L, leader(200L), advisor);
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(notificationService.createSystemAlert(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("notification backend down"));

        service.releaseAdvisor(10L, 300L, "professor");

        assertNull(group.getAdvisor());
        verify(groupRepository).save(group);
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    // ─── Helpers ──────────────────────────────────────────────────────

    private Group groupWith(Long groupId, User leader, User advisor) {
        Group group = new Group();
        group.setId(groupId);
        group.setLeader(leader);
        group.setAdvisor(advisor);
        return group;
    }

    private User leader(Long userId) {
        User user = new User();
        user.setUserId(userId);
        return user;
    }

    private User advisor(Long userId) {
        User user = new User();
        user.setUserId(userId);
        return user;
    }
}
