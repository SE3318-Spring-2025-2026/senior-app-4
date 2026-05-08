package com.spms.backend.repository;

import com.spms.backend.model.SprintAdvisorGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SprintAdvisorGradeRepository extends JpaRepository<SprintAdvisorGrade, Long> {
    Optional<SprintAdvisorGrade> findByGroup_IdAndSprint_IdAndAdvisor_UserId(Long groupId, Long sprintId, Long advisorId);
    List<SprintAdvisorGrade> findByGroup_IdAndSprint_Id(Long groupId, Long sprintId);
    List<SprintAdvisorGrade> findByGroup_IdAndSprint_IdIn(Long groupId, Collection<Long> sprintIds);
    List<SprintAdvisorGrade> findByGroup_Id(Long groupId);
}
