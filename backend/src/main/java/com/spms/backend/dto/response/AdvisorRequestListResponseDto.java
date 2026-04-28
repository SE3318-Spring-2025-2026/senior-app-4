package com.spms.backend.dto.response;

import java.util.List;

public record AdvisorRequestListResponseDto(
        String status,
        List<AdvisorRequestSummaryDto> data
) {}
