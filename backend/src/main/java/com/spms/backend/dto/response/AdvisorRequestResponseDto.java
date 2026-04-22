package com.spms.backend.dto.response;

import java.time.Instant;

public record AdvisorRequestResponseDto(
        Long notificationId,
        Long groupId,
        String groupName,
        Instant requestedAt
) {}
