package com.spms.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

public record JiraCallbackRequest(
    @NotNull List<Map<String, Object>> issues
) {}
