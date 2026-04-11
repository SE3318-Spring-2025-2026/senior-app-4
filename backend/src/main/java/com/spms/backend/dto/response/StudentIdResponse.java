package com.spms.backend.dto.response;

import java.util.Map;

public record StudentIdResponse(
        String message,
        Map<String, String> data
) {}
