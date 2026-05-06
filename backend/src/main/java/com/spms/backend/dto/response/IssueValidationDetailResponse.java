package com.spms.backend.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for {@code GET /api/v1/ai-validation/issues/{issueKey}/details}.
 *
 * <p>Spec shape (P7-AI-Validation-api.yaml §IssueValidationDetailResponse):
 * <pre>
 * {
 *   "status": "success",
 *   "data": {
 *     "issueKey": "PROJ-123",
 *     "issueDescription": "...",
 *     "prNumber": 42,
 *     "prUrl": "https://github.com/...",
 *     "evaluatedAt": "...",
 *     "reviewVerification": { ... },
 *     "implementationValidation": { ... }
 *   }
 * }
 * </pre>
 * </p>
 *
 * <p>DFD: 7.4 AI Review Verification + 7.5 AI Implementation Validation — Issue #296.</p>
 */
public record IssueValidationDetailResponse(
        String status,
        IssueDetailData data
) {

    public record IssueDetailData(
            String issueKey,
            String issueDescription,
            Integer prNumber,
            String prUrl,
            Instant evaluatedAt,
            ReviewVerification reviewVerification,
            ImplementationValidation implementationValidation
    ) {}

    public record ReviewVerification(
            Double score,
            Boolean hasReview,
            String reviewQuality,
            Integer reviewerCount,
            Boolean hasChangeRequests,
            Boolean hasSubstantiveComments,
            String aiFeedback
    ) {}

    public record ImplementationValidation(
            Double score,
            Boolean isValid,
            List<CoverageArea> coverageAreas,
            List<String> missingRequirements,
            String aiFeedback,
            Integer filesAnalyzed,
            Boolean diffTruncated
    ) {}

    public record CoverageArea(
            String requirement,
            Boolean covered
    ) {}
}
