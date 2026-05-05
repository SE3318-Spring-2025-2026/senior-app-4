package com.spms.backend.service;

// TODO(parallel: #298, @you, 2026-05-05): GET/PUT /ai-validation/config + validation_config singleton
//   Affects: AiValidationConfigServiceImpl.java, ValidationConfig.java, ValidationConfigRepository.java
//   Coordinate before editing: check with team

import com.spms.backend.dto.request.ValidationConfigRequest;
import com.spms.backend.dto.response.ValidationConfigResponse;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.model.ValidationConfig;
import com.spms.backend.repository.ValidationConfigRepository;
import com.spms.backend.service.impl.AiValidationConfigServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AiValidationConfigServiceImpl}.
 *
 * <p>Spec: P7-AI-Validation-api.yaml §7.0 Configuration — Issue #298.</p>
 *
 * <p>Each test pins one acceptance criterion from the issue AC list.
 * Structural assertions are used (field values, exception types) — no
 * string-matching on human-readable messages per §9 of CONTRIBUTING.</p>
 */
@ExtendWith(MockitoExtension.class)
class AiValidationConfigServiceTest {

    @Mock
    private ValidationConfigRepository configRepository;

    @InjectMocks
    private AiValidationConfigServiceImpl service;

    /** Default singleton with sensible values (as inserted by V12 migration). */
    private ValidationConfig defaultConfig;

    @BeforeEach
    void setUp() {
        defaultConfig = new ValidationConfig();
        defaultConfig.setId(1L);
        defaultConfig.setReviewWeight(40);
        defaultConfig.setImplementationWeight(60);
        defaultConfig.setOpenaiModel("gpt-4o");
        defaultConfig.setMaxDiffLines(500);
        defaultConfig.setExcludedFilePatterns(Collections.emptyList());
    }

    // ── AC: GET returns current config ───────────────────────────────────

    @Test
    @DisplayName("getConfig: returns config with status=success and correct data fields")
    void getConfig_returnsCurrentConfig() {
        when(configRepository.findById(1L)).thenReturn(Optional.of(defaultConfig));

        ValidationConfigResponse response = service.getConfig();

        assertEquals("success", response.status());
        assertNotNull(response.data());
        assertEquals(40, response.data().reviewWeight());
        assertEquals(60, response.data().implementationWeight());
        assertEquals("gpt-4o", response.data().openaiModel());
        assertEquals(500, response.data().maxDiffLines());
        assertNotNull(response.data().excludedFilePatterns()); // never null
        assertTrue(response.data().excludedFilePatterns().isEmpty());
    }

    // ── AC: Valid PUT → 200 with updated body ────────────────────────────

    @Test
    @DisplayName("updateConfig: valid request returns updated ValidationConfigResponse")
    void updateConfig_validRequest_returnsUpdated() {
        when(configRepository.findById(1L)).thenReturn(Optional.of(defaultConfig));
        when(configRepository.save(any(ValidationConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        ValidationConfigRequest req = new ValidationConfigRequest(
                30, 70, "gpt-4o-mini", 300, List.of("package-lock.json")
        );

        ValidationConfigResponse response = service.updateConfig(req);

        assertEquals("success", response.status());
        assertEquals(30, response.data().reviewWeight());
        assertEquals(70, response.data().implementationWeight());
        assertEquals("gpt-4o-mini", response.data().openaiModel());
        assertEquals(300, response.data().maxDiffLines());
        assertEquals(1, response.data().excludedFilePatterns().size());
        assertEquals("package-lock.json", response.data().excludedFilePatterns().get(0));
    }

    // ── AC: reviewWeight + implementationWeight ≠ 100 → 400 INVALID_WEIGHTS ──

    @Test
    @DisplayName("updateConfig: weights sum != 100 → BadRequestException with INVALID_WEIGHTS code")
    void updateConfig_invalidWeights_throwsBadRequest() {
        ValidationConfigRequest req = new ValidationConfigRequest(
                60, 60, null, null, null
        );

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.updateConfig(req));
        assertTrue(ex.getMessage().startsWith("INVALID_WEIGHTS"),
                "Message should begin with errorCode INVALID_WEIGHTS but was: " + ex.getMessage());
        // Repository must NOT be called — validation should short-circuit
        verify(configRepository, never()).save(any());
    }

    // ── AC: maxDiffLines = 0 → 400 INVALID_MAX_DIFF_LINES ───────────────

    @Test
    @DisplayName("updateConfig: maxDiffLines=0 → BadRequestException with INVALID_MAX_DIFF_LINES code")
    void updateConfig_maxDiffLinesZero_throwsBadRequest() {
        ValidationConfigRequest req = new ValidationConfigRequest(
                40, 60, null, 0, null
        );

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.updateConfig(req));
        assertTrue(ex.getMessage().startsWith("INVALID_MAX_DIFF_LINES"),
                "Message should begin with INVALID_MAX_DIFF_LINES but was: " + ex.getMessage());
        verify(configRepository, never()).save(any());
    }

    // ── AC: openaiModel not in whitelist → 400 INVALID_OPENAI_MODEL ─────

    @Test
    @DisplayName("updateConfig: openaiModel=gpt-3.5-fake → BadRequestException with INVALID_OPENAI_MODEL code")
    void updateConfig_invalidModel_throwsBadRequest() {
        ValidationConfigRequest req = new ValidationConfigRequest(
                40, 60, "gpt-3.5-fake", null, null
        );

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.updateConfig(req));
        assertTrue(ex.getMessage().startsWith("INVALID_OPENAI_MODEL"),
                "Message should begin with INVALID_OPENAI_MODEL but was: " + ex.getMessage());
        verify(configRepository, never()).save(any());
    }

    // ── AC: excludedFilePatterns=[] → stored as empty, GET returns [] not null ──

    @Test
    @DisplayName("updateConfig: excludedFilePatterns=[] → response returns empty list, not null")
    void updateConfig_emptyPatterns_returnsEmptyNotNull() {
        when(configRepository.findById(1L)).thenReturn(Optional.of(defaultConfig));
        when(configRepository.save(any(ValidationConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        ValidationConfigRequest req = new ValidationConfigRequest(
                40, 60, null, null, Collections.emptyList()
        );

        ValidationConfigResponse response = service.updateConfig(req);

        assertNotNull(response.data().excludedFilePatterns(),
                "excludedFilePatterns must never be null");
        assertTrue(response.data().excludedFilePatterns().isEmpty(),
                "excludedFilePatterns should be an empty list, not null");
    }

    // ── AC: null excludedFilePatterns in request → existing value preserved ──

    @Test
    @DisplayName("updateConfig: null excludedFilePatterns in request → existing patterns preserved")
    void updateConfig_nullPatterns_preservesExisting() {
        defaultConfig.setExcludedFilePatterns(List.of("*.svg"));
        when(configRepository.findById(1L)).thenReturn(Optional.of(defaultConfig));
        when(configRepository.save(any(ValidationConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        ValidationConfigRequest req = new ValidationConfigRequest(
                40, 60, null, null, null // null → keep existing
        );

        ValidationConfigResponse response = service.updateConfig(req);
        assertEquals(List.of("*.svg"), response.data().excludedFilePatterns());
    }

    // ── All three allowed models pass validation ──────────────────────────

    @Test
    @DisplayName("updateConfig: all whitelisted openaiModels are accepted")
    void updateConfig_allAllowedModels_succeed() {
        List<String> allowed = List.of("gpt-4o", "gpt-4o-mini", "gpt-4-turbo");
        when(configRepository.findById(1L)).thenReturn(Optional.of(defaultConfig));
        when(configRepository.save(any(ValidationConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        for (String model : allowed) {
            ValidationConfigRequest req = new ValidationConfigRequest(40, 60, model, null, null);
            assertDoesNotThrow(() -> service.updateConfig(req),
                    "Model " + model + " should be accepted");
        }
    }

    // ── maxDiffLines=1 is the minimum allowed ────────────────────────────

    @Test
    @DisplayName("updateConfig: maxDiffLines=1 is accepted (minimum valid value)")
    void updateConfig_maxDiffLinesOne_isAccepted() {
        when(configRepository.findById(1L)).thenReturn(Optional.of(defaultConfig));
        when(configRepository.save(any(ValidationConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        ValidationConfigRequest req = new ValidationConfigRequest(40, 60, null, 1, null);
        assertDoesNotThrow(() -> service.updateConfig(req));
    }
}
