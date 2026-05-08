package com.spms.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record SprintDeliverableWeightsRequest(
        @NotEmpty List<@Valid Item> items
) {
    public record Item(
            @NotNull Long sprintId,
            @NotBlank String deliverableType,
            @NotNull
            @DecimalMin(value = "0.0", inclusive = false, message = "weight must be > 0")
            @DecimalMax(value = "100.0", message = "weight must be <= 100")
            BigDecimal weight
    ) {}
}
