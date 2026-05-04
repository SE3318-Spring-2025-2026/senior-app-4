package com.spms.backend.repository;

import com.spms.backend.model.SprintIssueTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
    void deleteByGroup_IdAndSprint_Id(Long groupId, Long sprintId);

    /**
     * Find all tracking records for a sprint
     */
    List<SprintIssueTracking> findBySprint_Id(Long sprintId);
}
