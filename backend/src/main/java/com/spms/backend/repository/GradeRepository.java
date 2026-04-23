package com.spms.backend.repository;

import com.spms.backend.model.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {
    boolean existsBySubmissionIdAndProfessorId(Long submissionId, Long professorId);
    List<Grade> findBySubmissionId(Long submissionId);
}
