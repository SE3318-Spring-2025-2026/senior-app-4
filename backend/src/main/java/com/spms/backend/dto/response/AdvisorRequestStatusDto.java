package com.spms.backend.dto.response;

public record AdvisorRequestStatusDto(
        Long requestId,
        String professorName,
        String status
) {}