package com.spms.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record BulkUpdateRequestDto(
        @NotEmpty(message = "Records list cannot be empty")
        List<@Valid BulkUpdateRecordDto> records
) {
}
