package com.spms.backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CriterionScoreRequest(
        @NotNull Long criterionId,
        @NotNull @Min(0) @Max(100) Double score
) {}
