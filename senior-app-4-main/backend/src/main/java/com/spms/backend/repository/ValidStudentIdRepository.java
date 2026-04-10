package com.spms.backend.repository;

import java.util.List;
import java.util.Map;

/**
 * D2 Data Store — Geçerli Öğrenci ID'leri deposu.
 *
 * Koordinatör tarafından CSV ile yüklenen öğrenci ID'lerini saklar.
 * Student validation işlemi bu repository üzerinden yapılır.
 */
public interface ValidStudentIdRepository {

    boolean existsByStudentId(String studentId);

    void save(String studentId);

    Map<String, String> findByStudentId(String studentId);

    List<Map<String, String>> findAll();

    boolean deleteByStudentId(String studentId);
}
