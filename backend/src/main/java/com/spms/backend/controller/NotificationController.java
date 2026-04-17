package com.spms.backend.controller;

import com.spms.backend.dto.request.AdvisorRequestDto;
import com.spms.backend.dto.request.NotificationRespondRequestDto;
import com.spms.backend.dto.response.AdvisorRequestStatusDto;
import com.spms.backend.dto.response.NotificationDto;
import com.spms.backend.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }


    @PostMapping("/api/v1/groups/{groupId}/advisor-request")
    public ResponseEntity<Void> requestAdvisor(
            @PathVariable Long groupId,
            @Valid @RequestBody AdvisorRequestDto request,
            @RequestAttribute("jwt_userId") Object userId) {

        Long leaderId = Long.valueOf(userId.toString());
        notificationService.requestAdvisor(groupId, request, leaderId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }


    @GetMapping("/api/v1/groups/{groupId}/advisor-request")
    public ResponseEntity<AdvisorRequestStatusDto> getAdvisorRequestStatus(
            @PathVariable Long groupId) {

        AdvisorRequestStatusDto status = notificationService.getAdvisorRequestStatus(groupId);
        return ResponseEntity.ok(status);
    }


    @DeleteMapping("/api/v1/groups/{groupId}/advisor-request")
    public ResponseEntity<Void> cancelAdvisorRequest(
            @PathVariable Long groupId,
            @RequestAttribute("jwt_userId") Object userId) {

        // leaderId servise ileride gerekirse eklenebilir; şimdilik groupId yeterli
        notificationService.cancelAdvisorRequest(groupId);
        return ResponseEntity.noContent().build();
    }


    /**
     * withdrawRequest / revokeNotification  (Related: Issue #80)
     * DELETE /api/v1/advisor-requests/{id}
     *
     * Allows a student (group leader) to withdraw a PENDING advisor request
     * by notification ID. Soft-deletes the notification in D8 (status → REVOKED).
     *
     * Returns 204 No Content on success.
     * Returns 404 if the notification does not exist.
     * Returns 400 if the request is not PENDING.
     * Returns 403 if the caller is not the original sender.
     */
    @DeleteMapping("/api/v1/advisor-requests/{id}")
    public ResponseEntity<Void> withdrawAdvisorRequest(
            @PathVariable Long id,
            @RequestAttribute("jwt_userId") Object userId) {

        Long requesterId = Long.valueOf(userId.toString());
        notificationService.withdrawAdvisorRequest(id, requesterId);
        return ResponseEntity.noContent().build();
    }



    @GetMapping("/api/v1/notifications")
    public ResponseEntity<Page<NotificationDto>> getUserNotifications(
            Pageable pageable,
            @RequestAttribute("jwt_userId") Object userId) {

        Long currentUserId = Long.valueOf(userId.toString());
        Page<NotificationDto> notifications = notificationService.getUserNotifications(currentUserId, pageable);
        return ResponseEntity.ok(notifications);
    }


    @DeleteMapping("/api/v1/notifications")
    public ResponseEntity<Void> clearNotifications(
            @RequestParam(required = false) Long id,
            @RequestAttribute("jwt_userId") Object userId) {

        Long currentUserId = Long.valueOf(userId.toString());
        if (id != null) {
            notificationService.clearNotification(id, currentUserId);
        } else {
            notificationService.clearAllNotifications(currentUserId);
        }
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/api/v1/notifications/{notificationId}/respond")
    public ResponseEntity<Void> respondToNotification(
            @PathVariable Long notificationId,
            @Valid @RequestBody NotificationRespondRequestDto request,
            @RequestAttribute("jwt_userId") Object userId) {

        Long currentUserId = Long.valueOf(userId.toString());
        notificationService.respondToNotification(notificationId, request.decision(), currentUserId);
        return ResponseEntity.ok().build();
    }


    @GetMapping("/api/v1/coordinator/system-alerts")
    public ResponseEntity<Page<NotificationDto>> getSystemAlerts(
            Pageable pageable,
            @RequestAttribute("jwt_role") Object role) {

        String userRole = role.toString();
        Page<NotificationDto> alerts = notificationService.getSystemAlerts(pageable, userRole);
        return ResponseEntity.ok(alerts);
    }
}
