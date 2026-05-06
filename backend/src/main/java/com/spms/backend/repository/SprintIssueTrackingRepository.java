package com.spms.backend.repository;

import com.spms.backend.model.SprintIssueTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SprintIssueTrackingRepository extends JpaRepository<SprintIssueTracking, Long> {

    /**
     * Find all tracking records for a group in a specific sprint
     */
    List<SprintIssueTracking> findByGroup_IdAndSprint_Id(Long groupId, Long sprintId);

    /**
     * Find specific tracking record by group, sprint, and issue key
     */
    Optional<SprintIssueTracking> findByGroup_IdAndSprint_IdAndIssueKey(Long groupId, Long sprintId, String issueKey);

    /**
     * Delete all tracking records for a group in a specific sprint
     */
    void deleteByGroup_IdAndSprint_Id(Long groupId, Long sprintId);

    /**
     * Find all tracking records for a sprint
     */
    List<SprintIssueTracking> findBySprint_Id(Long sprintId);
}
