package com.spms.backend.controller;

import com.spms.backend.dto.request.AdvisorRequestCreateDto;
import com.spms.backend.dto.request.AdvisorRequestDto;
import com.spms.backend.dto.request.NotificationRespondRequestDto;
import com.spms.backend.dto.response.AdvisorRequestCreateResponseDto;
import com.spms.backend.dto.response.AdvisorRequestStatusDto;
import com.spms.backend.dto.response.NotificationDto;
import com.spms.backend.dto.response.SuccessResponse;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.exception.ForbiddenException;
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


    /**
     * P4.1 — Submit Advisor Request
     * Canonical endpoint defined in Process 4 API specs.
     */
    @PostMapping("/api/v1/advisor-requests")
    public ResponseEntity<AdvisorRequestCreateResponseDto> createAdvisorRequest(
            @Valid @RequestBody AdvisorRequestCreateDto request,
            @RequestAttribute("jwt_userId") Object userId,
            @RequestAttribute("jwt_role") Object role) {

        String requesterRole = role.toString();
        if (!"student".equalsIgnoreCase(requesterRole)) {
            throw new ForbiddenException("Only students can send advisor requests.");
        }

        Long leaderId = Long.valueOf(userId.toString());
        Long teamId;
        try {
            teamId = Long.valueOf(request.teamId());
        } catch (NumberFormatException ex) {
            throw new BadRequestException("teamId must be a numeric string.");
        }
        Long requestId = notificationService.requestAdvisor(
                teamId,
                new AdvisorRequestDto(request.professorId(), request.message()),
                leaderId
        );
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new AdvisorRequestCreateResponseDto(requestId, "PENDING"));
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
            @RequestParam(required = false, defaultValue = "ALL") String readStatus,
            @RequestAttribute("jwt_userId") Object userId) {

        Long currentUserId = Long.valueOf(userId.toString());
        Page<NotificationDto> notifications = notificationService.getNotifications(currentUserId, readStatus, pageable);
        return ResponseEntity.ok(notifications);
    }

    @PatchMapping("/api/v1/notifications/{notificationId}/read")
    public ResponseEntity<SuccessResponse> markAsRead(
            @PathVariable Long notificationId,
            @RequestAttribute("jwt_userId") Object userId) {

        Long currentUserId = Long.valueOf(userId.toString());
        notificationService.markAsRead(notificationId, currentUserId);
        return ResponseEntity.ok(new SuccessResponse("success", "Notification marked as read."));
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



}
