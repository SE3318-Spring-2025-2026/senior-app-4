package com.spms.backend.dto.response;

// TODO(parallel: #298, @you, 2026-05-05): GET/PUT /ai-validation/config + validation_config singleton
//   Affects: AiValidationConfigController.java, AiValidationConfigServiceImpl.java
//   Coordinate before editing: check with team

import java.util.List;

/**
 * Response DTO for {@code GET /api/v1/ai-validation/config} and
 * {@code PUT /api/v1/ai-validation/config}.
 *
 * <p>Spec shape (P7-AI-Validation-api.yaml §ValidationConfigResponse):
 * <pre>
 * {
 *   "status": "success",
 *   "data": {
 *     "reviewWeight": 40,
 *     "implementationWeight": 60,
 *     "openaiModel": "gpt-4o",
 *     "maxDiffLines": 500,
 *     "excludedFilePatterns": []
 *   }
 * }
 * </pre>
 * </p>
 */
public record ValidationConfigResponse(
        String status,
        ValidationConfigData data
) {

    public record ValidationConfigData(
            int reviewWeight,
            int implementationWeight,
            String openaiModel,
            int maxDiffLines,
            List<String> excludedFilePatterns
    ) {
    }
}
