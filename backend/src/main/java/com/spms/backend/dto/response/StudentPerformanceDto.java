package com.spms.backend.dto.response;

public record StudentPerformanceDto(
    Long studentId,
    String name,
    Integer assignedSp,
    Integer accomplishedSp,
    Double ratio
) {}
