package com.spms.backend.dto.response;

import com.spms.backend.model.ActionType;
import java.time.Instant;

public record AuditLogResponseDto(
        Long id,
        Long userId,
        ActionType actionType,
        String eventDetails,
        Long groupId,
        Long committeeId,
        String ipAddress,
        Instant createdAt
) {}
