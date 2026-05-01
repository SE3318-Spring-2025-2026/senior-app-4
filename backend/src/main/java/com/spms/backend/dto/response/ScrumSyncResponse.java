package com.spms.backend.dto.response;

import java.time.Instant;

/**
 * Response DTO for POST /api/v1/scrum-sync/trigger
 */
public record ScrumSyncResponse(
    String status,
    String message,
    String syncId,
    Instant startedAt
) {}
