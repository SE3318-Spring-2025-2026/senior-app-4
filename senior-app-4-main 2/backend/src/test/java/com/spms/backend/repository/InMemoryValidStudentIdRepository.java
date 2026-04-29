package com.spms.backend.repository;

import com.spms.backend.model.ValidStudentId;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TEST İÇİN — valid_student_ids tablosunun in-memory implementasyonu.
 */
public class InMemoryValidStudentIdRepository extends AbstractStubJpaRepository<ValidStudentId, Long> implements ValidStudentIdRepository {

    private final Set<String> validIds = ConcurrentHashMap.newKeySet();

    @Override
    public boolean existsByStudentId(String studentId) {
        if (studentId == null) return false;
        return validIds.contains(studentId.trim());
    }

    @Override
    public Optional<ValidStudentId> findByStudentId(String studentId) {
        if (studentId == null || !validIds.contains(studentId.trim())) return Optional.empty();
        ValidStudentId entity = new ValidStudentId(studentId.trim());
        return Optional.of(entity);
    }

    @Override
    public ValidStudentId save(ValidStudentId entity) {
        if (entity != null && entity.getStudentId() != null) {
            validIds.add(entity.getStudentId().trim());
        }
        return entity;
    }

    @Override
    public List<ValidStudentId> findAll() {
        return validIds.stream()
                .map(ValidStudentId::new)
                .toList();
    }

    @Override
    public boolean deleteByStudentId(String studentId) {
        if (studentId == null) return false;
        return validIds.remove(studentId.trim());
    }

    @Override
    public Optional<ValidStudentId> findById(Long id) { return Optional.empty(); }

    @Override
    public void deleteById(Long id) { /* not used in tests */ }

    @Override
    public void delete(ValidStudentId entity) { if (entity != null) validIds.remove(entity.getStudentId()); }

    @Override
    public void deleteAll() { validIds.clear(); }

    @Override
    public long count() { return validIds.size(); }

    /** Test kolaylığı */
    public void addId(String studentId) {
        validIds.add(studentId);
    }
}
