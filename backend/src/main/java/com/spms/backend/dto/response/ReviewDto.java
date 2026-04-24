package com.spms.backend.dto.response;

import java.time.Instant;

public record ReviewDto(
    Long id,
    Long submissionId,
    String reviewerName,
    String comment,
    Instant createdAt
) {}
