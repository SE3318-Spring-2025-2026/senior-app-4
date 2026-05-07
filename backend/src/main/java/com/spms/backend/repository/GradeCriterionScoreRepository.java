package com.spms.backend.repository;

import com.spms.backend.model.GradeCriterionScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GradeCriterionScoreRepository extends JpaRepository<GradeCriterionScore, Long> {
    List<GradeCriterionScore> findByGradeId(Long gradeId);
    void deleteByGradeId(Long gradeId);
}
