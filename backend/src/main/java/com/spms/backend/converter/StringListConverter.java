package com.spms.backend.converter;

// TODO(parallel: #298, @you, 2026-05-05): GET/PUT /ai-validation/config + validation_config singleton
//   Affects: ValidationConfig.java, AiValidationConfigServiceImpl.java, V12 migration
//   Coordinate before editing: check with team

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Converts {@code List<String>} ↔ comma-separated {@code TEXT} column.
 * <p>
 * An empty list is stored as an empty string {@code ""} and is read back
 * as an empty list (never {@code null}), satisfying the spec rule:
 * "excludedFilePatterns: [] → stored as empty array; subsequent GET returns [] (never null)".
 * </p>
 */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final String DELIMITER = ",";

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "";
        }
        return attribute.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(DELIMITER));
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(dbData.split(DELIMITER, -1))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
