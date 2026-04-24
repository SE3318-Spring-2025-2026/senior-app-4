package com.spms.backend.dto.request;

import jakarta.validation.constraints.*;

public record GradingCriteriaCreateRequestDto(
        @NotBlank(message = "deliverableType is required")
        String deliverableType,

        @NotBlank(message = "name is required")
        @Size(max = 255, message = "name must be at most 255 characters")
        String name,

        String description,

        @NotNull(message = "weight is required")
        @DecimalMin(value = "0.0", message = "weight must be >= 0")
        @DecimalMax(value = "100.0", message = "weight must be <= 100")
        Double weight
) {}
