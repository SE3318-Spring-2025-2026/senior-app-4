package com.spms.backend.repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * TEST İÇİN — valid_student_ids tablosunun in-memory implementasyonu.
 */
public class InMemoryValidStudentIdRepository implements ValidStudentIdRepository {

    private final Set<String> validIds = ConcurrentHashMap.newKeySet();

    @Override
    public boolean existsByStudentId(String studentId) {
        if (studentId == null) return false;
        return validIds.contains(studentId.trim());
    }

    @Override
    public void save(String studentId) {
        if (studentId != null) {
            validIds.add(studentId.trim());
        }
    }

    @Override
    public Map<String, String> findByStudentId(String studentId) {
        if (studentId == null || !validIds.contains(studentId.trim())) {
            return null;
        }
        return Map.of("studentId", studentId.trim(), "status", "valid");
    }

    @Override
    public List<Map<String, String>> findAll() {
        return validIds.stream()
                .map(id -> Map.of("studentId", id, "status", "valid"))
                .collect(Collectors.toList());
    }

    @Override
    public boolean deleteByStudentId(String studentId) {
        if (studentId == null) return false;
        return validIds.remove(studentId.trim());
    }

    /** Test kolaylığı */
    public void addId(String studentId) {
        save(studentId);
    }
}
