package com.spms.backend.dto.response;

public record GradingCriteriaDto(
        Long id,
        String deliverableType,
        String name,
        String description,
        Double weight
) {}
