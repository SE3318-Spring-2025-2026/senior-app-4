package com.spms.backend.dto.response;

public record AdvisorDecisionResponseDto(
        String status,
        String message,
        AdvisorDecisionData data
) {
    public record AdvisorDecisionData(
            Long requestId,
            String decision,
            Long teamId,
            Long advisorId
    ) {}
}
