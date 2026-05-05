package com.spms.backend.repository;

// TODO(parallel: #298, @you, 2026-05-05): GET/PUT /ai-validation/config + validation_config singleton
//   Affects: ValidationConfig.java, AiValidationConfigServiceImpl.java
//   Coordinate before editing: check with team

import com.spms.backend.model.ValidationConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for the {@code validation_config} singleton table.
 * Always use {@code findById(1L)} to retrieve the sole row.
 */
@Repository
public interface ValidationConfigRepository extends JpaRepository<ValidationConfig, Long> {
}
