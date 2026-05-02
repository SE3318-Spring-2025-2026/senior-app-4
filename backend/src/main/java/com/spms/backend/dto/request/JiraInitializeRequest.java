package com.spms.backend.dto.request;

import jakarta.validation.constraints.NotNull;

public record JiraInitializeRequest(
    @NotNull Long groupId
) {}
