package com.spms.backend.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.spms.backend.model.Group;
import com.spms.backend.model.GroupStatus;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long>, JpaSpecificationExecutor<Group> {

    // --- Takım arkadaşlarının eski metotları (Korundu) ---
    java.util.List<Group> findByStatus(com.spms.backend.model.GroupStatus status);
    java.util.List<Group> findByAdvisorIsNullAndStatusNot(com.spms.backend.model.GroupStatus status);

    @Query("SELECT g FROM Group g WHERE g.advisor.userId = :advisorId")
    Page<Group> findByAdvisorId(@Param("advisorId") Long advisorId, Pageable pageable);

    @Query("SELECT g FROM Group g WHERE g.advisor.userId = :advisorId")
    List<Group> findByAdvisorId(@Param("advisorId") Long advisorId);

    @Query("SELECT g.status, COUNT(g) FROM Group g GROUP BY g.status")
    List<Object[]> countGroupsByStatus();

    @Query("SELECT g FROM Group g " +
            "ORDER BY CASE WHEN EXISTS " +
            "(SELECT 1 FROM GroupMember m WHERE m.group = g AND m.user.userId = :studentId) " +
            "THEN 0 ELSE 1 END, g.id ASC")
    Page<Group> findAllWithStudentGroupFirst(@Param("studentId") Long studentId, Pageable pageable);

    /** All non-disbanded groups with advisor and leader loaded (P4-ASSIGN-1 list). */
    @Query("SELECT DISTINCT g FROM Group g LEFT JOIN FETCH g.advisor LEFT JOIN FETCH g.leader "
            + "WHERE g.status <> :disbanded")
    List<Group> findAllNonDisbandedWithAdvisorAndLeaderFetched(@Param("disbanded") GroupStatus disbanded);

    long countByAdvisor_UserId(Long advisorId);
}
