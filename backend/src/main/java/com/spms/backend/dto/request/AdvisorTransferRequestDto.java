package com.spms.backend.dto.request;

import jakarta.validation.constraints.NotNull;

public record AdvisorTransferRequestDto(
        @NotNull(message = "Professor ID is required")
        Long professorId
) {}
