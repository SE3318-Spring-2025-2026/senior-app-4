package com.spms.backend.service;

import com.spms.backend.dto.request.CommitteeNotifyRequestDto;
import com.spms.backend.dto.response.CommitteeNotificationResponse;
import com.spms.backend.dto.response.NotificationDto;

import java.util.List;

/**
 * P5.7 — Committee Notification Service
 * Handles sending notifications to committee members upon P5.2/P5.3/P5.4 triggers.
 */
public interface CommitteeNotificationService {

    /**
     * POST /api/v1/committees/{committeeId}/notify
     * Coordinator gönderir; recipients boşsa tüm üyeler alır.
     */
    CommitteeNotificationResponse sendCommitteeNotification(Long committeeId,
                                                            CommitteeNotifyRequestDto request,
                                                            Long senderUserId);

    /**
     * GET /api/v1/committees/{committeeId}/notifications
     */
    List<NotificationDto> getCommitteeNotifications(Long committeeId);

    /**
     * GET /api/v1/committees/notifications — caller'ın aldığı committee bildirimleri
     */
    List<NotificationDto> getMyCommitteeNotifications(Long userId);

    /**
     * P5.2 trigger: Advisor assignment yapıldığında committee üyelerini bilgilendir.
     */
    void notifyAdvisorAssignment(Long committeeId, Long assignedUserId, Long actorUserId);

    /**
     * P5.3 trigger: Jury assignment yapıldığında committee üyelerini bilgilendir.
     */
    void notifyJuryAssignment(Long committeeId, Long assignedUserId, Long actorUserId);

    /**
     * P5.4 trigger: Group assignment yapıldığında committee üyelerini bilgilendir.
     */
    void notifyGroupAssignment(Long committeeId, Long groupId, Long actorUserId);
}
