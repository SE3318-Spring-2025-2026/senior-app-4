package com.spms.backend.dto.response;

import java.time.Instant;

public record AdvisorRequestDetailDto(
        Long id,
        String status,
        String message,
        Instant requestedAt,
        Instant decidedAt,
        String reason,
        TeamDto team,
        ProfessorDto professor
) {
    public record TeamDto(
            Long id,
            String name,
            int memberCount,
            String leaderName,
            Long leaderId,
            boolean hasCurrentAdvisor
    ) {}

    public record ProfessorDto(
            Long id,
            String fullName,
            String email,
            long currentAdviseeCount
    ) {}
}
