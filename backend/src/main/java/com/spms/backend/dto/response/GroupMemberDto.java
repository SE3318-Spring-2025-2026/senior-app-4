package com.spms.backend.dto.response;

import java.time.Instant;
public record GroupMemberDto(
        Long userId,
        String fullName,
        String role,
        Instant joinedAt
) {}