package com.spms.backend.service.impl;

import com.spms.backend.dto.request.AdvisorRequestDto;
import com.spms.backend.dto.response.AdvisorRequestStatusDto;
import com.spms.backend.dto.response.NotificationDto;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.exception.UnauthorizedException;
import com.spms.backend.model.notification.Notification;
import com.spms.backend.model.notification.NotificationStatus;
import com.spms.backend.model.notification.NotificationType;
import com.spms.backend.model.User;
import com.spms.backend.repository.NotificationRepository;
import com.spms.backend.repository.UserRepository;
import com.spms.backend.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void requestAdvisor(Long groupId, AdvisorRequestDto request, Long leaderId) {

        Optional<Notification> existingPendingRequest = notificationRepository
                .findByGroupIdAndTypeAndStatus(groupId, NotificationType.ADVISOR_REQUEST, NotificationStatus.PENDING);

        if (existingPendingRequest.isPresent()) {
            throw new BadRequestException("This group already has a pending advisor request.");
        }

        User leader = userRepository.findById(leaderId)
                .orElseThrow(() -> new BadRequestException("Leader user not found."));

        User professor = userRepository.findById(request.professorId())
                .orElseThrow(() -> new BadRequestException("Professor not found."));


        Notification notification = new Notification();
        notification.setType(NotificationType.ADVISOR_REQUEST);
        notification.setStatus(NotificationStatus.PENDING);
        notification.setMessage(leader.getFullName() + " has requested you as an advisor for their group.");
        notification.setGroupId(groupId);
        notification.setFromUser(leader);
        notification.setToUser(professor);

        notificationRepository.save(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public AdvisorRequestStatusDto getAdvisorRequestStatus(Long groupId) {
        Notification lastRequest = notificationRepository
                .findByGroupIdAndTypeAndStatus(groupId, NotificationType.ADVISOR_REQUEST, NotificationStatus.PENDING)
                .orElseThrow(() -> new BadRequestException("No pending advisor request found."));

        return new AdvisorRequestStatusDto(
                lastRequest.getId(),
                lastRequest.getToUser() != null ? lastRequest.getToUser().getFullName() : "Unknown",
                lastRequest.getStatus().name()
        );
    }

    @Override
    @Transactional
    public void cancelAdvisorRequest(Long groupId) {
        Notification request = notificationRepository
                .findByGroupIdAndTypeAndStatus(groupId, NotificationType.ADVISOR_REQUEST, NotificationStatus.PENDING)
                .orElseThrow(() -> new BadRequestException("No pending advisor request found to cancel."));

        request.setStatus(NotificationStatus.CLEARED);
        notificationRepository.save(request);

        // TODO: (Optional) If Process P2.9 requires saving to an Audit Log table, add it here
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDto> getUserNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByToUser_UserId(userId, pageable)
                .map(this::mapToDto);
    }

    @Override
    @Transactional
    public void clearNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BadRequestException("Notification not found."));


        if (!notification.getToUser().getUserId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to clear this notification.");
        }

        notification.setStatus(NotificationStatus.CLEARED);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void clearAllNotifications(Long userId) {
        notificationRepository.deleteByToUser_UserId(userId);
    }

    @Override
    @Transactional
    public void respondToNotification(Long notificationId, String decision, Long userId) {

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BadRequestException("Notification not found."));

        if (!notification.getToUser().getUserId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to respond to this request.");
        }


        if ("accept".equalsIgnoreCase(decision)) {
            notification.setStatus(NotificationStatus.ACCEPTED);
            // TODO: (Issue #30) P2.2 Process: Student accepted the group. Call 'GroupMembership' logic here.
        } else if ("reject".equalsIgnoreCase(decision)) {
            notification.setStatus(NotificationStatus.REJECTED);
        } else {
            throw new BadRequestException("Invalid decision value! Only 'accept' or 'reject' are supported.");
        }

        notificationRepository.save(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDto> getSystemAlerts(Pageable pageable, String role) {
        // Validation: Only users with the COORDINATOR role can view system alerts
        if (!"COORDINATOR".equalsIgnoreCase(role)) {
            throw new UnauthorizedException("You are not authorized to view system alerts! Required role: COORDINATOR.");
        }

        return Page.empty(pageable);
    }


    private NotificationDto mapToDto(Notification notif) {
        return new NotificationDto(
                notif.getId(),
                notif.getType().name(),
                notif.getMessage(),
                notif.getStatus().name(),
                notif.getFromUser() != null ? notif.getFromUser().getUserId() : null,
                notif.getToUser() != null ? notif.getToUser().getUserId() : null,
                notif.getGroupId(),
                notif.getCreatedAt()
        );
    }
}
