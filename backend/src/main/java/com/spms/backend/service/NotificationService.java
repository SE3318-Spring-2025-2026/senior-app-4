package com.spms.backend.service;

import com.spms.backend.dto.request.AdvisorRequestDto;
import com.spms.backend.dto.response.AdvisorRequestStatusDto;
import com.spms.backend.dto.response.NotificationDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    void requestAdvisor(Long groupId, AdvisorRequestDto request, Long leaderId);
    AdvisorRequestStatusDto getAdvisorRequestStatus(Long groupId);
    void cancelAdvisorRequest(Long groupId);
    Page<NotificationDto> getUserNotifications(Long userId, Pageable pageable);
    void clearNotification(Long notificationId, Long userId);
    void clearAllNotifications(Long userId);
    void respondToNotification(Long notificationId, String decision, Long userId);
    Page<NotificationDto> getSystemAlerts(Pageable pageable, String role);
}