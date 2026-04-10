package com.spms.backend.dto.response;

import java.util.List;
import java.util.Map;

public record StudentIdListResponse(
        String message,
        int count,
        List<Map<String, String>> data
) {}
