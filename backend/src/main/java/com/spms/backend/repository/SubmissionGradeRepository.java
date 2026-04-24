package com.spms.backend.repository;

import com.spms.backend.model.SubmissionGrade;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubmissionGradeRepository extends JpaRepository<SubmissionGrade, Long> {
    List<SubmissionGrade> findBySubmissionId(Long submissionId);
    boolean existsBySubmissionIdAndProfessorId(Long submissionId, Long professorId);
}
