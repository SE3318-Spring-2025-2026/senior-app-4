package com.spms.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record GroupUpdateRequestDto(
        @NotBlank(message = "Group name cannot be blank")
        @Size(min = 3, max = 100, message = "Group name must be between 3 and 100 characters")
        String groupName
) {}