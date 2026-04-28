package com.spms.backend.dto.request;

import jakarta.validation.constraints.NotNull;

public record AdvisorRequestCreateDto(
        @NotNull String teamId,
        @NotNull Long professorId,
        String message
) {}
