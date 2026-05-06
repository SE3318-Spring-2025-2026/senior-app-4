package com.spms.backend.repository;

import com.spms.backend.model.SprintIssueTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface SprintIssueTrackingRepository extends JpaRepository<SprintIssueTracking, Long> {

    /**
     * Find all tracking records for a group in a specific sprint
     */
    List<SprintIssueTracking> findByGroup_IdAndSprint_Id(Long groupId, Long sprintId);

    /**
     * Delete all tracking records for a group in a specific sprint
     */
    @Transactional
    @Modifying
    @Query("""
                delete from SprintIssueTracking s
                where s.group.id = :groupId
                and s.sprint.id = :sprintId
            """)
    void deleteByGroup_IdAndSprint_Id(@Param("groupId") Long groupId, @Param("sprintId") Long sprintId);

    /**
     * Find all tracking records for a sprint
     */
    List<SprintIssueTracking> findBySprint_Id(Long sprintId);
}
