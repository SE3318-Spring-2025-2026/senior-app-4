package com.spms.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateReviewRequestDto(
        @NotBlank(message = "comment is required")
        String comment,

        @NotNull(message = "status is required")
        String status
) {}
