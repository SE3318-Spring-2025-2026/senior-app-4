package com.spms.backend.dto.response;

import java.util.List;

public record AuditLogListResponse(
        String status,
        List<AuditLogResponseDto> data,
        CommitteePaginationInfo pagination) {}
