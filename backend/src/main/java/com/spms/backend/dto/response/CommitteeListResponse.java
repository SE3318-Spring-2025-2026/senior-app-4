package com.spms.backend.dto.response;

import java.util.List;

public record CommitteeListResponse(
        String status,
        List<CommitteeDetailDto> data,
        CommitteePaginationInfo pagination) {}
