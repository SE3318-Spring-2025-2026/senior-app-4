package com.spms.backend.service;

import com.spms.backend.dto.response.NotificationDto;
import com.spms.backend.model.notification.Notification;
import com.spms.backend.model.notification.NotificationType;
import com.spms.backend.model.notification.NotificationStatus;
import com.spms.backend.model.User;
import com.spms.backend.repository.NotificationRepository;
import com.spms.backend.repository.UserRepository;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.ScheduleRepository;
import com.spms.backend.service.MemberService;
import com.spms.backend.service.impl.NotificationServiceImpl;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceProcess5Test {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private ScheduleRepository scheduleRepository;
    @Mock
    private MemberService memberService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    @DisplayName("NotificationService: getNotifications should return mapped NotificationDtos")
    void getNotifications_ReturnsCorrectDtos() {
        // Arrange
        Notification mockNotif = new Notification();
        mockNotif.setId(1L);
        User user = new User();
        user.setUserId(100L);
        mockNotif.setToUser(user);
        mockNotif.setMessage("Test Message");
        mockNotif.setType(NotificationType.SYSTEM_ALERT);
        mockNotif.setReadStatus(false);
        mockNotif.setStatus(NotificationStatus.PENDING);

        Page<Notification> mockPage = new PageImpl<>(List.of(mockNotif));
        
        when(notificationRepository.findByToUser_UserId(eq(100L), any(PageRequest.class)))
                .thenReturn(mockPage);

        // Act
        Page<NotificationDto> result = notificationService.getNotifications(100L, "ALL", PageRequest.of(0, 10));

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        NotificationDto dto = result.getContent().get(0);
        assertEquals(1L, dto.id());
        assertEquals("system_alert", dto.type());
        assertEquals("Test Message", dto.message());
        assertEquals("pending", dto.status()); 
    }

    @Test
    @DisplayName("NotificationService: Type validation functions correctly when sending a notification (createSystemAlert)")
    void createSystemAlert_ValidatesTypeAndSaves() {
        // Arrange
        Long userId = 100L;
        String message = "System update";
        String alertType = "SYSTEM_ALERT";

        User user = new User();
        user.setUserId(userId);

        Notification savedMock = new Notification();
        savedMock.setId(10L);
        savedMock.setType(NotificationType.SYSTEM_ALERT);
        savedMock.setMessage(message);
        savedMock.setToUser(user);
        
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedMock);

        // Act
        Notification result = notificationService.createSystemAlert(userId, message, alertType, null);

        // Assert
        assertNotNull(result);
        assertEquals(NotificationType.SYSTEM_ALERT, result.getType());
        assertEquals(message, result.getMessage());
        assertEquals(userId, result.getToUser().getUserId());
        
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }
}
