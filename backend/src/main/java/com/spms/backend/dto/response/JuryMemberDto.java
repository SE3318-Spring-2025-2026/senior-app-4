package com.spms.backend.dto.response;

import java.time.Instant;

public record JuryMemberDto(
        Long juryMemberId,
        Long userId,
        String fullName,
        String juryType,
        Instant assignedAt) {}
