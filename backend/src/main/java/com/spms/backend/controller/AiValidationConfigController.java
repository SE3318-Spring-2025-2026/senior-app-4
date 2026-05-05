package com.spms.backend.controller;

// TODO(parallel: #298, @you, 2026-05-05): GET/PUT /ai-validation/config + validation_config singleton
//   Affects: AiValidationConfigService.java, ValidationConfigRequest.java,
//            ValidationConfigResponse.java, P7-AI-Validation-api.yaml §7.0 Configuration
//   Coordinate before editing: check with team

import com.spms.backend.dto.request.ValidationConfigRequest;
import com.spms.backend.dto.response.ValidationConfigResponse;
import com.spms.backend.exception.ForbiddenException;
import com.spms.backend.service.AiValidationConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for AI validation configuration endpoints.
 *
 * <pre>
 * GET  /api/v1/ai-validation/config  — Read current config  (Coordinator only)
 * PUT  /api/v1/ai-validation/config  — Update config        (Coordinator only)
 * </pre>
 *
 * <p>DFD: Process 7.0 Configuration — Issue #298.<br>
 * Spec: P7-AI-Validation-api.yaml §7.0 Configuration tag.</p>
 */
@Tag(name = "7.0 Configuration", description = "Configure AI validation weights, thresholds, and OpenAI model parameters")
@RestController
@RequestMapping("/api/v1/ai-validation/config")
public class AiValidationConfigController {

    private final AiValidationConfigService configService;

    public AiValidationConfigController(AiValidationConfigService configService) {
        this.configService = configService;
    }

    /**
     * GET /api/v1/ai-validation/config
     *
     * <p>Returns the current AI validation configuration.</p>
     *
     * <p>Authorization: COORDINATOR only. Advisor/Student → 403.</p>
     */
    @Operation(summary = "Get AI validation configuration (Coordinator only)")
    @GetMapping
    public ResponseEntity<ValidationConfigResponse> getConfig(HttpServletRequest httpReq) {
        requireCoordinator(httpReq);
        return ResponseEntity.ok(configService.getConfig());
    }

    /**
     * PUT /api/v1/ai-validation/config
     *
     * <p>Updates the AI validation configuration. Changes apply to future
     * validation runs only — existing results are not recalculated.</p>
     *
     * <p>Authorization: COORDINATOR only. Advisor/Student → 403.</p>
     *
     * <p>Validation errors → 400 with errorCode encoded in the message field:
     * {@code INVALID_WEIGHTS}, {@code INVALID_MAX_DIFF_LINES}, {@code INVALID_OPENAI_MODEL}.</p>
     */
    @Operation(summary = "Update AI validation configuration (Coordinator only)")
    @PutMapping
    public ResponseEntity<ValidationConfigResponse> updateConfig(
            @Valid @RequestBody ValidationConfigRequest request,
            HttpServletRequest httpReq) {
        requireCoordinator(httpReq);
        return ResponseEntity.ok(configService.updateConfig(request));
    }

    // ── Private helpers ─────────────────────────────────────────────────

    /**
     * Reads {@code jwt_role} from the request attributes (set by the JWT filter)
     * and throws {@link ForbiddenException} if the caller is not a coordinator.
     *
     * <p>The spec says: "Advisor / Student token → 403 FORBIDDEN".</p>
     */
    private void requireCoordinator(HttpServletRequest httpReq) {
        Object role = httpReq.getAttribute("jwt_role");
        if (!"coordinator".equals(role)) {
            throw new ForbiddenException("FORBIDDEN: Only coordinators can access the AI validation configuration.");
        }
    }
}
