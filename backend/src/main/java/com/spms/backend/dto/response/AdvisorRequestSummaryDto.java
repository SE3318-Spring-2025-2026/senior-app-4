package com.spms.backend.dto.response;

import java.time.Instant;
import java.util.UUID;

public record AdvisorRequestSummaryDto(
        Long id,
        Long teamId,
        String teamName,
        Long professorId,
        String professorName,
        String status,
        Instant requestedAt,
        Instant decidedAt
) {}
