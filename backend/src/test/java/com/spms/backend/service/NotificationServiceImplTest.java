package com.spms.backend.service;

import com.spms.backend.dto.request.AdvisorRequestDto;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.exception.ConflictException;
import com.spms.backend.model.Group;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

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

    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl(
                notificationRepository,
                userRepository,
                groupRepository,
                scheduleRepository,
                memberService
        );
    }

    @Test
    void requestAdvisorReturnsConflictWhenGroupAlreadyHasActiveAdvisor() {
        Group group = baseGroup();
        group.setAdvisor(new User());
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));

        assertThrows(
                ConflictException.class,
                () -> notificationService.requestAdvisor(10L, new AdvisorRequestDto(99L), 1L)
        );

        verify(notificationRepository, never()).save(org.mockito.ArgumentMatchers.any(Notification.class));
    }

    @Test
    void requestAdvisorReturnsBadRequestWhenPendingRequestAlreadyExists() {
        Group group = baseGroup();
        Notification existingPending = new Notification();
        existingPending.setType(NotificationType.ADVISOR_REQUEST);
        existingPending.setStatus(NotificationStatus.PENDING);
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(scheduleRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        when(notificationRepository.findByGroupIdAndTypeAndStatus(
                10L, NotificationType.ADVISOR_REQUEST, NotificationStatus.PENDING))
                .thenReturn(Optional.of(existingPending));

        assertThrows(
                BadRequestException.class,
                () -> notificationService.requestAdvisor(10L, new AdvisorRequestDto(99L), 1L)
        );

        verify(notificationRepository, never()).save(org.mockito.ArgumentMatchers.any(Notification.class));
    }

    @Test
    void requestAdvisorCreatesUnreadNotificationForProfessor() {
        Group group = baseGroup();
        User leader = buildUser(1L, "leader", "STUDENT");
        User professor = buildUser(99L, "prof", "PROFESSOR");

        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(scheduleRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        when(notificationRepository.findByGroupIdAndTypeAndStatus(
                10L, NotificationType.ADVISOR_REQUEST, NotificationStatus.PENDING))
                .thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(leader));
        when(userRepository.findById(99L)).thenReturn(Optional.of(professor));
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.requestAdvisor(10L, new AdvisorRequestDto(99L), 1L);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());

        Notification saved = notificationCaptor.getValue();
        assertEquals(NotificationType.ADVISOR_REQUEST, saved.getType());
        assertEquals(NotificationStatus.PENDING, saved.getStatus());
        assertEquals(10L, saved.getGroupId());
        assertEquals(1L, saved.getFromUser().getUserId());
        assertEquals(99L, saved.getToUser().getUserId());
    }

    @Test
    void requestAdvisorReturnsBadRequestWhenTargetUserIsNotProfessor() {
        Group group = baseGroup();
        User leader = buildUser(1L, "leader", "STUDENT");
        User targetUser = buildUser(99L, "not-prof", "STUDENT");

        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(scheduleRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        when(notificationRepository.findByGroupIdAndTypeAndStatus(
                10L, NotificationType.ADVISOR_REQUEST, NotificationStatus.PENDING))
                .thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(leader));
        when(userRepository.findById(99L)).thenReturn(Optional.of(targetUser));

        assertThrows(
                BadRequestException.class,
                () -> notificationService.requestAdvisor(10L, new AdvisorRequestDto(99L), 1L)
        );

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    private Group baseGroup() {
        Group group = new Group();
        group.setId(10L);
        group.setLeader(buildUser(1L, "leader", "STUDENT"));
        return group;
    }

    private User buildUser(Long userId, String fullName, String role) {
        User user = new User();
        user.setUserId(userId);
        user.setFullName(fullName);
        user.setRole(role);
        return user;
    }
}
