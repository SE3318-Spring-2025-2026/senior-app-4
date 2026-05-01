package com.spms.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public record BulkUpdateRecordDto(
        @NotNull(message = "Student ID cannot be null")
        @NotBlank(message = "Student ID cannot be empty")
        String studentId
) {
}
