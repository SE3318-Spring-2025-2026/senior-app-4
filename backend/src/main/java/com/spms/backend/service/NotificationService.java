package com.spms.backend.service;

import com.spms.backend.dto.request.AdvisorRequestDto;
import com.spms.backend.dto.response.AdvisorRequestStatusDto;
import com.spms.backend.dto.response.NotificationDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationService {
    Long requestAdvisor(Long groupId, AdvisorRequestDto request, Long leaderId);
    AdvisorRequestStatusDto getAdvisorRequestStatus(Long groupId);
    void cancelAdvisorRequest(Long groupId);
    /** withdrawRequest / revokeNotification — sets notification status to REVOKED (soft-delete). */
    void withdrawAdvisorRequest(Long notificationId, Long requesterId);

    /** Enhanced notification fetching with UNREAD/ALL filters. */
    Page<NotificationDto> getNotifications(Long userId, String readStatus, Pageable pageable);

    /** Sets readStatus = true. [PATCH /notifications/{id}/read] */
    void markAsRead(Long notificationId, Long userId);

    void clearNotification(Long notificationId, Long userId);
    void clearAllNotifications(Long userId);
    void respondToNotification(Long notificationId, String decision, Long userId);
    void sendGroupDisbandedNotification(Long groupId, Long actorUserId, String groupName, List<Long> memberIds);
    com.spms.backend.model.notification.Notification createSystemAlert(Long toUserId, String message, String alertType, String metadata);
    List<com.spms.backend.model.notification.Notification> getSystemAlertsByUserId(Long userId);
    boolean systemAlertExists(Long toUserId, String alertType);
    void sendMembershipInvite(Long toUserId, Long groupId, String groupName);
}
