package com.spms.backend.service;

import com.spms.backend.exception.BadRequestException;
import com.spms.backend.exception.ForbiddenException;
import com.spms.backend.exception.NotFoundException;
import com.spms.backend.model.User;
import com.spms.backend.model.notification.Notification;
import com.spms.backend.model.notification.NotificationStatus;
import com.spms.backend.model.notification.NotificationType;
import com.spms.backend.repository.NotificationRepository;
import com.spms.backend.repository.UserRepository;
import com.spms.backend.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceIssue81Test {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MemberService memberService;

    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl(notificationRepository, userRepository, memberService);
    }

    @Test
    void withdrawAdvisorRequestSetsStatusToRevoked() {
        Notification notification = advisorRequestNotification(18L, 55L, NotificationStatus.PENDING);
        when(notificationRepository.findById(18L)).thenReturn(Optional.of(notification));

        notificationService.withdrawAdvisorRequest(18L, 55L);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertEquals(NotificationStatus.REVOKED, captor.getValue().getStatus());
    }

    @Test
    void withdrawAdvisorRequestRejectsNonOwner() {
        Notification notification = advisorRequestNotification(18L, 55L, NotificationStatus.PENDING);
        when(notificationRepository.findById(18L)).thenReturn(Optional.of(notification));

        assertThrows(
                ForbiddenException.class,
                () -> notificationService.withdrawAdvisorRequest(18L, 77L)
        );
    }

    @Test
    void withdrawAdvisorRequestRejectsNonPendingStatus() {
        Notification notification = advisorRequestNotification(18L, 55L, NotificationStatus.ACCEPTED);
        when(notificationRepository.findById(18L)).thenReturn(Optional.of(notification));

        assertThrows(
                BadRequestException.class,
                () -> notificationService.withdrawAdvisorRequest(18L, 55L)
        );
    }

    @Test
    void withdrawAdvisorRequestThrowsNotFoundWhenMissing() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> notificationService.withdrawAdvisorRequest(99L, 55L)
        );
    }

    private Notification advisorRequestNotification(Long notificationId, Long fromUserId, NotificationStatus status) {
        User fromUser = new User();
        fromUser.setUserId(fromUserId);

        Notification notification = new Notification();
        notification.setId(notificationId);
        notification.setType(NotificationType.ADVISOR_REQUEST);
        notification.setStatus(status);
        notification.setFromUser(fromUser);
        return notification;
    }
}
