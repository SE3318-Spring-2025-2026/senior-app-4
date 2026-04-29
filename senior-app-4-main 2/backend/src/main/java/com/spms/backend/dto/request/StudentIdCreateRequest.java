package com.spms.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record StudentIdCreateRequest(
        @NotBlank(message = "studentId is required.")
        String studentId
) {}
