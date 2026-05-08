package com.spms.backend.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record SprintDeliverableWeightResponse(
        String status,
        List<Item> data
) {
    public record Item(
            Long id,
            Long sprintId,
            String sprintName,
            String deliverableType,
            BigDecimal weight
    ) {}
}
