package com.spms.backend.repository;

import com.spms.backend.model.StudentFinalGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentFinalGradeRepository extends JpaRepository<StudentFinalGrade, Long> {
    List<StudentFinalGrade> findByGroup_IdOrderByUser_FullNameAsc(Long groupId);
    Optional<StudentFinalGrade> findByGroup_IdAndUser_UserId(Long groupId, Long userId);
    List<StudentFinalGrade> findByUser_UserId(Long userId);
}
