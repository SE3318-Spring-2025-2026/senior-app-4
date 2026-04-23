package com.spms.backend.dto.request;

import jakarta.validation.constraints.NotNull;

public record InviteMemberRequestDto(
        @NotNull(message = "studentId must not be null")
        Long studentId
) {}
