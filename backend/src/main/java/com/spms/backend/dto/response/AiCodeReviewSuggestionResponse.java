package com.spms.backend.dto.response;

import java.math.BigDecimal;

public record AiCodeReviewSuggestionResponse(
        String status,
        Data data
) {
    public record Data(
            Long groupId,
            Long sprintId,
            BigDecimal aiScore,
            String suggestedGrade
    ) {}
}
