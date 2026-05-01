package com.spms.backend.dto.response;

public record GradingCriteriaDto(
        Long id,
        String deliverableType,
        String gradingType,
        String name,
        String description,
        Double weight
) {}
