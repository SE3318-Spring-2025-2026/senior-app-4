package com.spms.backend.dto.response;

import java.time.Instant;
public record NotificationDto(
        Long id,
        String type,
        String message,
        String status,
        boolean readStatus,
        Long fromUserId,
        String fromUserName,
        Long toUserId,
        Long groupId,
        Instant createdAt
) {}