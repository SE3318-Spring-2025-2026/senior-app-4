package com.spms.backend.controller;

import com.spms.backend.dto.request.CommitteeNotifyRequestDto;
import com.spms.backend.dto.response.CommitteeNotificationResponse;
import com.spms.backend.dto.response.NotificationDto;
import com.spms.backend.exception.ForbiddenException;
import com.spms.backend.service.CommitteeNotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * P5.7 — Committee Notification REST Controller
 *
 * POST  /api/v1/committees/{committeeId}/notify       → bildirim gönder (coordinator)
 * GET   /api/v1/committees/{committeeId}/notifications → committee bildirimleri
 * GET   /api/v1/committees/notifications               → kullanıcının aldığı bildirimler
 */
@RestController
@RequestMapping("/api/v1/committees")
public class CommitteeNotificationController {

    private final CommitteeNotificationService committeeNotificationService;

    public CommitteeNotificationController(CommitteeNotificationService committeeNotificationService) {
        this.committeeNotificationService = committeeNotificationService;
    }

    /**
     * POST /api/v1/committees/{committeeId}/notify
     * Yetki: sadece coordinator rolü
     * Body: { message, notificationType, recipients? }
     */
    @PostMapping("/{committeeId}/notify")
    public ResponseEntity<CommitteeNotificationResponse> sendNotification(
            @PathVariable Long committeeId,
            @Valid @RequestBody CommitteeNotifyRequestDto request,
            @RequestAttribute("jwt_userId") Object userId,
            @RequestAttribute("jwt_role") Object role) {

        if (!"coordinator".equalsIgnoreCase(role.toString())) {
            throw new ForbiddenException("Only coordinators can send committee notifications.");
        }

        Long senderUserId = Long.valueOf(userId.toString());
        CommitteeNotificationResponse response =
                committeeNotificationService.sendCommitteeNotification(committeeId, request, senderUserId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/committees/{committeeId}/notifications
     * Yetki: authenticated user
     * Output: committee'ye ait tüm bildirimler (yeniden eskiye)
     */
    @GetMapping("/{committeeId}/notifications")
    public ResponseEntity<List<NotificationDto>> getCommitteeNotifications(
            @PathVariable Long committeeId,
            @RequestAttribute("jwt_userId") Object userId) {

        List<NotificationDto> notifications =
                committeeNotificationService.getCommitteeNotifications(committeeId);

        return ResponseEntity.ok(notifications);
    }

    /**
     * GET /api/v1/committees/notifications
     * Yetki: authenticated user
     * Output: kullanıcının aldığı tüm committee bildirimleri
     */
    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationDto>> getMyCommitteeNotifications(
            @RequestAttribute("jwt_userId") Object userId) {

        Long currentUserId = Long.valueOf(userId.toString());
        List<NotificationDto> notifications =
                committeeNotificationService.getMyCommitteeNotifications(currentUserId);

        return ResponseEntity.ok(notifications);
    }
}
