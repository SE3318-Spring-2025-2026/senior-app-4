package com.spms.backend.dto.response;

import java.time.Instant;
public record GroupResponseDto(
        Long id,
        String groupName,
        Long leaderId,
        Long advisorId,
        String status,
        int memberCount,
        Instant createdAt,
        boolean githubBound,
        boolean jiraBound
) {}