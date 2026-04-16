package com.spms.backend.repository;

import com.spms.backend.model.ValidStudentId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ValidStudentIdRepository extends JpaRepository<ValidStudentId, Long> {

    boolean existsByStudentId(String studentId);

    Optional<ValidStudentId> findByStudentId(String studentId);

    default void saveStudentId(String studentId) {
        save(new ValidStudentId(studentId));
    }

    default boolean deleteByStudentId(String studentId) {
        Optional<ValidStudentId> entity = findByStudentId(studentId);
        if (entity.isEmpty()) return false;
        delete(entity.get());
        return true;
    }
}
