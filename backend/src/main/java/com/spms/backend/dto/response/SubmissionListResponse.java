package com.spms.backend.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record SubmissionListResponse(
        String status,
        List<SubmissionSummary> data,
        PaginationMeta pagination) {

    public record SubmissionSummary(
            Long id,
            Long teamId,
            String teamName,
            String deliverableType,
            String status,
            Long assignedCommitteeId,
            Integer revisionNumber,
            LocalDateTime submittedAt,
            LocalDateTime deadline,
            Boolean isOverdue) {
    }

    public record PaginationMeta(
            int totalElements,
            int totalPages,
            int currentPage,
            int pageSize) {
    }
}
