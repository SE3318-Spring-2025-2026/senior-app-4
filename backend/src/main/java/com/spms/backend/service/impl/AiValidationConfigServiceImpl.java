package com.spms.backend.service.impl;

// TODO(parallel: #298, @you, 2026-05-05): GET/PUT /ai-validation/config + validation_config singleton
//   Affects: AiValidationConfigService.java, ValidationConfig.java, ValidationConfigRepository.java,
//            ValidationConfigRequest.java, ValidationConfigResponse.java
//   Coordinate before editing: check with team

import com.spms.backend.dto.request.ValidationConfigRequest;
import com.spms.backend.dto.response.ValidationConfigResponse;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.model.ValidationConfig;
import com.spms.backend.repository.ValidationConfigRepository;
import com.spms.backend.service.AiValidationConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Implementation of {@link AiValidationConfigService}.
 *
 * <p>DFD reference: Process 7.0 Configuration — Issue #298.</p>
 *
 * <p><strong>Security note:</strong> log only IDs and counts, never raw
 * model names or pattern lists, to prevent accidental PII / key leakage
 * via log aggregation.</p>
 */
@Service
public class AiValidationConfigServiceImpl implements AiValidationConfigService {

    private static final Logger logger = LoggerFactory.getLogger(AiValidationConfigServiceImpl.class);

    /** Singleton row ID — always 1. */
    private static final long CONFIG_ID = 1L;

    /** Allowed OpenAI model identifiers (spec-specified whitelist). */
    private static final Set<String> ALLOWED_MODELS = Set.of(
            "gpt-4o",
            "gpt-4o-mini",
            "gpt-4-turbo"
    );

    private final ValidationConfigRepository configRepository;

    public AiValidationConfigServiceImpl(ValidationConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    // ── GET ──────────────────────────────────────────────────────────────

    @Override
    public ValidationConfigResponse getConfig() {
        ValidationConfig config = loadSingleton();
        logger.info("AI validation config retrieved (id={})", CONFIG_ID);
        return toResponse(config);
    }

    // ── PUT ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ValidationConfigResponse updateConfig(ValidationConfigRequest request) {
        validate(request);

        ValidationConfig config = loadSingleton();

        config.setReviewWeight(request.reviewWeight());
        config.setImplementationWeight(request.implementationWeight());

        if (request.openaiModel() != null) {
            config.setOpenaiModel(request.openaiModel());
        }
        if (request.maxDiffLines() != null) {
            config.setMaxDiffLines(request.maxDiffLines());
        }

        // excludedFilePatterns: null in request → keep existing; empty list → store empty
        if (request.excludedFilePatterns() != null) {
            config.setExcludedFilePatterns(request.excludedFilePatterns());
        }

        ValidationConfig saved = configRepository.save(config);

        // Log pattern count, not raw patterns (no PII leakage)
        logger.info("AI validation config updated (id={}, reviewWeight={}, implementationWeight={}, " +
                        "patternCount={})",
                CONFIG_ID,
                saved.getReviewWeight(),
                saved.getImplementationWeight(),
                saved.getExcludedFilePatterns().size());

        return toResponse(saved);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    /**
     * Loads the singleton config row or throws if missing (should never happen
     * because the V12 migration inserts the default row).
     */
    private ValidationConfig loadSingleton() {
        return configRepository.findById(CONFIG_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "validation_config singleton row (id=1) is missing — check V12 migration."));
    }

    /**
     * Enforces all spec-mandated business rules and throws
     * {@link BadRequestException} with the spec error code encoded in the
     * message (option-2 decision: errorCode encoded in message field).
     */
    private void validate(ValidationConfigRequest request) {
        // Rule 1: weights must sum to 100
        if (request.reviewWeight() + request.implementationWeight() != 100) {
            throw new BadRequestException("INVALID_WEIGHTS: reviewWeight + implementationWeight must equal 100.");
        }

        // Rule 2: maxDiffLines, if provided, must be >= 1
        if (request.maxDiffLines() != null && request.maxDiffLines() < 1) {
            throw new BadRequestException("INVALID_MAX_DIFF_LINES: maxDiffLines must be at least 1.");
        }

        // Rule 3: openaiModel, if provided, must be in the whitelist
        if (request.openaiModel() != null && !ALLOWED_MODELS.contains(request.openaiModel())) {
            throw new BadRequestException(
                    "INVALID_OPENAI_MODEL: openaiModel must be one of " + ALLOWED_MODELS + ".");
        }
    }

    /** Maps a {@link ValidationConfig} entity to the spec response envelope. */
    private static ValidationConfigResponse toResponse(ValidationConfig config) {
        List<String> patterns = config.getExcludedFilePatterns();
        return new ValidationConfigResponse(
                "success",
                new ValidationConfigResponse.ValidationConfigData(
                        config.getReviewWeight(),
                        config.getImplementationWeight(),
                        config.getOpenaiModel(),
                        config.getMaxDiffLines(),
                        patterns == null ? Collections.emptyList() : patterns
                )
        );
    }
}
