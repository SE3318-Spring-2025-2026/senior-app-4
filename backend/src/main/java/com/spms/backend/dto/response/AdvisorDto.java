package com.spms.backend.dto.response;

import java.time.Instant;

public record AdvisorDto(
        Long committeeAdvisorId,
        Long userId,
        String fullName,
        String role,
        Instant assignedAt) {}
