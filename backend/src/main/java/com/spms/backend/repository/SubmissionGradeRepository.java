package com.spms.backend.repository;

import com.spms.backend.model.SubmissionGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmissionGradeRepository extends JpaRepository<SubmissionGrade, Long> {
    boolean existsBySubmissionIdAndReviewerId(Long submissionId, Long reviewerId);
    List<SubmissionGrade> findBySubmissionId(Long submissionId);
}
