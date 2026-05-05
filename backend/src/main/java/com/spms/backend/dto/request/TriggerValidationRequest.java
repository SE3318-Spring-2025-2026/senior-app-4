package com.spms.backend.dto.request;

import java.util.List;

public record TriggerValidationRequest(
        Long teamId,
        List<String> issueKeys
) {
}
