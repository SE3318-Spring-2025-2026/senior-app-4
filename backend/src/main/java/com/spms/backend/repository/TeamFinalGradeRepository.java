package com.spms.backend.repository;

import com.spms.backend.model.TeamFinalGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamFinalGradeRepository extends JpaRepository<TeamFinalGrade, Long> {
    Optional<TeamFinalGrade> findByGroup_Id(Long groupId);
}
