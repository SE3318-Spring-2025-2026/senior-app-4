package com.spms.backend.model;

// TODO(parallel: #298, @you, 2026-05-05): GET/PUT /ai-validation/config + validation_config singleton
//   Affects: V12 migration, ValidationConfigRepository.java, AiValidationConfigServiceImpl.java,
//            StringListConverter.java
//   Coordinate before editing: check with team

import com.spms.backend.converter.StringListConverter;
import jakarta.persistence.*;

import java.util.Collections;
import java.util.List;

/**
 * JPA entity for the {@code validation_config} singleton table.
 * <p>
 * There is exactly one row in this table (id = 1). The entity is never
 * constructed directly by application code — it is loaded via
 * {@link com.spms.backend.repository.ValidationConfigRepository#findById(Object)}.
 * </p>
 *
 * <p>DFD reference: Process 7 / Issue #298 — "Config GET/PUT Endpoints".</p>
 */
@Entity
@Table(name = "validation_config")
public class ValidationConfig {

    @Id
    private Long id;

    @Column(name = "review_weight", nullable = false)
    private int reviewWeight;

    @Column(name = "implementation_weight", nullable = false)
    private int implementationWeight;

    @Column(name = "openai_model", nullable = false, length = 50)
    private String openaiModel;

    @Column(name = "max_diff_lines", nullable = false)
    private int maxDiffLines;

    /**
     * Stored as a comma-delimited TEXT column; converted via
     * {@link StringListConverter}.  Always returns a non-null list.
     */
    @Convert(converter = StringListConverter.class)
    @Column(name = "excluded_file_patterns", nullable = false)
    private List<String> excludedFilePatterns = Collections.emptyList();

    // ── Getters / setters ──────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public int getReviewWeight() { return reviewWeight; }
    public void setReviewWeight(int reviewWeight) { this.reviewWeight = reviewWeight; }

    public int getImplementationWeight() { return implementationWeight; }
    public void setImplementationWeight(int implementationWeight) { this.implementationWeight = implementationWeight; }

    public String getOpenaiModel() { return openaiModel; }
    public void setOpenaiModel(String openaiModel) { this.openaiModel = openaiModel; }

    public int getMaxDiffLines() { return maxDiffLines; }
    public void setMaxDiffLines(int maxDiffLines) { this.maxDiffLines = maxDiffLines; }

    public List<String> getExcludedFilePatterns() {
        return excludedFilePatterns == null ? Collections.emptyList() : excludedFilePatterns;
    }
    public void setExcludedFilePatterns(List<String> excludedFilePatterns) {
        this.excludedFilePatterns = excludedFilePatterns == null ? Collections.emptyList() : excludedFilePatterns;
    }
}
