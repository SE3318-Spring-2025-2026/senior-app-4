package com.spms.backend.dto.response;

import java.util.List;

public record GroupAssignmentListResponse(
        int count,
        List<GroupAssignmentResponse> data
) {}
