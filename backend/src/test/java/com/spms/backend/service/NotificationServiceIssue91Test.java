package com.spms.backend.service;

import com.spms.backend.dto.response.NotificationDto;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.exception.ForbiddenException;
import com.spms.backend.exception.NotFoundException;
import com.spms.backend.model.User;
import com.spms.backend.model.notification.Notification;
import com.spms.backend.model.notification.NotificationStatus;
import com.spms.backend.model.notification.NotificationType;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.NotificationRepository;
import com.spms.backend.repository.ScheduleRepository;
import com.spms.backend.repository.UserRepository;
import com.spms.backend.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceIssue91Test {

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

    private NotificationServiceImpl notificationService;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl(notificationRepository, userRepository, groupRepository, scheduleRepository, memberService, messagingTemplate);
        pageable = PageRequest.of(0, 10);
    }

    @Test
    void getNotificationsReturnsUnreadWhenReadStatusUnread() {
        Notification notification = buildNotification(15L, 100L);
        when(notificationRepository.findByToUser_UserIdAndReadStatus(100L, false, pageable))
                .thenReturn(new PageImpl<>(List.of(notification)));

        Page<NotificationDto> result = notificationService.getNotifications(100L, "UNREAD", pageable);

        assertEquals(1, result.getTotalElements());
        verify(notificationRepository).findByToUser_UserIdAndReadStatus(100L, false, pageable);
    }

    @Test
    void getNotificationsReturnsReadWhenReadStatusRead() {
        Notification notification = buildNotification(16L, 100L);
        notification.setReadStatus(true);
        when(notificationRepository.findByToUser_UserIdAndReadStatus(100L, true, pageable))
                .thenReturn(new PageImpl<>(List.of(notification)));

        Page<NotificationDto> result = notificationService.getNotifications(100L, "READ", pageable);

        assertEquals(1, result.getTotalElements());
        verify(notificationRepository).findByToUser_UserIdAndReadStatus(100L, true, pageable);
    }

    @Test
    void getNotificationsRejectsInvalidReadStatus() {
        assertThrows(
                BadRequestException.class,
                () -> notificationService.getNotifications(100L, "NEW_ONLY", pageable)
        );
    }

    @Test
    void markAsReadUpdatesReadStatusForRecipientOnly() {
        Notification notification = buildNotification(22L, 100L);
        when(notificationRepository.findById(22L)).thenReturn(Optional.of(notification));

        notificationService.markAsRead(22L, 100L);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertEquals(true, captor.getValue().isReadStatus());
    }

    @Test
    void markAsReadRejectsOtherRecipients() {
        Notification notification = buildNotification(22L, 100L);
        when(notificationRepository.findById(22L)).thenReturn(Optional.of(notification));

        assertThrows(
                ForbiddenException.class,
                () -> notificationService.markAsRead(22L, 999L)
        );
    }

    @Test
    void markAsReadThrowsNotFoundWhenNotificationMissing() {
        when(notificationRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> notificationService.markAsRead(404L, 100L)
        );
    }

    private Notification buildNotification(Long notificationId, Long recipientId) {
        User recipient = new User();
        recipient.setUserId(recipientId);

        Notification notification = new Notification();
        notification.setId(notificationId);
        notification.setType(NotificationType.SYSTEM_ALERT);
        notification.setStatus(NotificationStatus.PENDING);
        notification.setMessage("system alert");
        notification.setToUser(recipient);
        return notification;
    }
}
