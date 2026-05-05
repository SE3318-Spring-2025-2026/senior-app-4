package com.spms.backend.dto.request;

// TODO(parallel: #298, @you, 2026-05-05): GET/PUT /ai-validation/config + validation_config singleton
//   Affects: AiValidationConfigController.java, AiValidationConfigServiceImpl.java
//   Coordinate before editing: check with team

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request body for {@code PUT /api/v1/ai-validation/config}.
 *
 * <p>Spec: {@code ValidationConfigRequest} in P7-AI-Validation-api.yaml §Schemas.</p>
 *
 * <p>Required fields: {@code reviewWeight}, {@code implementationWeight}.
 * Optional: {@code openaiModel}, {@code maxDiffLines}, {@code excludedFilePatterns}.</p>
 */
public record ValidationConfigRequest(

        @NotNull(message = "reviewWeight is required")
        Integer reviewWeight,

        @NotNull(message = "implementationWeight is required")
        Integer implementationWeight,

        String openaiModel,

        Integer maxDiffLines,

        List<String> excludedFilePatterns
) {
}
