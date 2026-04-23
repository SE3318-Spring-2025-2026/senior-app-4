package com.spms.backend.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.spms.backend.model.Group;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
    
    // --- Takım arkadaşlarının eski metotları (Korundu) ---
    java.util.List<Group> findByStatus(com.spms.backend.model.GroupStatus status);
    java.util.List<Group> findByAdvisorIsNullAndStatusNot(com.spms.backend.model.GroupStatus status);

    @Query("SELECT g FROM Group g WHERE g.advisor.userId = :advisorId")
    Page<Group> findByAdvisorId(@Param("advisorId") Long advisorId, Pageable pageable);

    @Query("SELECT g.status, COUNT(g) FROM Group g GROUP BY g.status")
    List<Object[]> countGroupsByStatus();

    @Query("SELECT g FROM Group g LEFT JOIN g.members m " +
           "ORDER BY CASE WHEN m.user.userId = :studentId THEN 0 ELSE 1 END, g.id ASC")
    Page<Group> findAllWithStudentGroupFirst(@Param("studentId") Long studentId, Pageable pageable);

    long countByAdvisor_UserId(Long advisorId);
}