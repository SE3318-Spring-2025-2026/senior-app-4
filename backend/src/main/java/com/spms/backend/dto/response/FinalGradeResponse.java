package com.spms.backend.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record FinalGradeResponse(
        String status,
        Data data
) {
    public record Data(
            Long groupId,
            String groupName,
            BigDecimal teamGrade,
            boolean published,
            Instant computedAt,
            Instant publishedAt,
            List<DeliverableBreakdown> deliverables,
            List<StudentGrade> students
    ) {}

    public record DeliverableBreakdown(
            String deliverableType,
            BigDecimal rawGrade,
            BigDecimal scrumAverage,
            BigDecimal codeReviewAverage,
            BigDecimal scalar,
            BigDecimal scaledGrade,
            BigDecimal finalWeight,
            BigDecimal contribution
    ) {}

    public record StudentGrade(
            Long userId,
            String fullName,
            String githubUsername,
            BigDecimal finalGrade,
            BigDecimal spRatio,
            boolean published
    ) {}

    public record GroupGradeSummary(
            Long groupId,
            String groupName,
            BigDecimal teamGrade,
            boolean published,
            java.time.Instant publishedAt,
            int studentCount
    ) {}
}
