package com.spms.backend.repository;

import com.spms.backend.model.SubmissionGradeCriterionScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmissionGradeCriterionScoreRepository extends JpaRepository<SubmissionGradeCriterionScore, Long> {
    List<SubmissionGradeCriterionScore> findByGrade_Id(Long gradeId);
    List<SubmissionGradeCriterionScore> findByGrade_SubmissionId(Long submissionId);
    void deleteByGrade_Id(Long gradeId);
}
