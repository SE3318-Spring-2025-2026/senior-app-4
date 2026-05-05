package com.spms.backend.service;

// TODO(parallel: #298, @you, 2026-05-05): GET/PUT /ai-validation/config + validation_config singleton
//   Affects: AiValidationConfigServiceImpl.java, AiValidationConfigController.java
//   Coordinate before editing: check with team

import com.spms.backend.dto.request.ValidationConfigRequest;
import com.spms.backend.dto.response.ValidationConfigResponse;

/**
 * Service interface for reading and updating the AI validation configuration
 * singleton (Process 7, Issue #298).
 */
public interface AiValidationConfigService {

    /**
     * Returns the current AI validation configuration.
     *
     * @return the config wrapped in {@link ValidationConfigResponse}
     */
    ValidationConfigResponse getConfig();

    /**
     * Validates and applies an updated configuration.
     *
     * <p>Business rules enforced:
     * <ul>
     *   <li>{@code reviewWeight + implementationWeight} must equal 100 → {@code INVALID_WEIGHTS}</li>
     *   <li>{@code maxDiffLines} (if provided) must be ≥ 1 → {@code INVALID_MAX_DIFF_LINES}</li>
     *   <li>{@code openaiModel} (if provided) must be one of the whitelisted values
     *       → {@code INVALID_OPENAI_MODEL}</li>
     * </ul>
     * </p>
     *
     * @param request the new configuration values
     * @return the updated config wrapped in {@link ValidationConfigResponse}
     * @throws com.spms.backend.exception.BadRequestException on constraint violations
     */
    ValidationConfigResponse updateConfig(ValidationConfigRequest request);
}
