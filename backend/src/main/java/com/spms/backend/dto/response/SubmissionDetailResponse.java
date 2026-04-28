package com.spms.backend.dto.response;

import java.time.LocalDateTime;

public record SubmissionDetailResponse(
        Long id,
        Long groupId,
        String teamName,
        String deliverableType,
        String status,
        String content,
        String fileUrl,
        Long committeeId,
        Double finalGrade,
        Long parentSubmissionId,
        Integer version,
        LocalDateTime createdAt
) {}
