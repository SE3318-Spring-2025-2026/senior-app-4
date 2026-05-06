package com.spms.backend.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for {@code GET /api/v1/ai-validation/sprints/{sprintId}/results}.
 *
 * <p>Spec shape (P7-AI-Validation-api.yaml §SprintValidationResultsResponse):
 * <pre>
 * {
 *   "status": "success",
 *   "data": {
 *     "sprintId": "...",
 *     "evaluatedAt": "...",
 *     "teams": [ { TeamValidationResult } ]
 *   }
 * }
 * </pre>
 * </p>
 *
 * <p>DFD: 7.6 Store Validation Results (read side) — Issue #296.</p>
 */
public record SprintValidationResultsResponse(
        String status,
        SprintResultsData data
) {

    public record SprintResultsData(
            Long sprintId,
            Instant evaluatedAt,
            List<TeamResult> teams
    ) {}

    public record TeamResult(
            Long teamId,
            String teamName,
            double overallSprintScore,
            int issueCount,
            List<IssueSummary> issues
    ) {}

    public record IssueSummary(
            String issueKey,
            String issueTitle,
            String assignee,
            Integer prNumber,
            Boolean prMerged,
            Double reviewVerificationScore,
            String reviewQuality,
            Double implementationMatchScore,
            Double compositeScore,
            String status
    ) {}
}
