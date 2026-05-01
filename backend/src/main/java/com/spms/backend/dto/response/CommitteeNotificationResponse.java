package com.spms.backend.dto.response;

import java.time.Instant;

/**
 * P5.7 — Response body for POST /api/v1/committees/{committeeId}/notify
 */
public record CommitteeNotificationResponse(
        Long notificationId,
        String status,
        int recipientCount,
        Instant sentAt
) {}
