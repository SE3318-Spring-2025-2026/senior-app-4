package com.spms.backend.repository;

import com.spms.backend.model.GroupCommitteeAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroupCommitteeAssignmentRepository extends JpaRepository<GroupCommitteeAssignment, Long> {

    Optional<GroupCommitteeAssignment> findTopByGroupIdAndStatusOrderByAssignedAtDesc(Long groupId, String status);

    Optional<GroupCommitteeAssignment> findByCommittee_CommitteeIdAndGroupId(Long committeeId, Long groupId);

    List<GroupCommitteeAssignment> findByCommittee_CommitteeId(Long committeeId);

    List<GroupCommitteeAssignment> findByGroupId(Long groupId);

    boolean existsByCommittee_CommitteeIdAndGroupId(Long committeeId, Long groupId);

    /**
     * P5.5 — Find all assignments for a committee whose examDate falls within
     * [from, to] (inclusive). Used to detect schedule conflicts within a ±2-hour window.
     */
    @Query("SELECT a FROM GroupCommitteeAssignment a "
            + "WHERE a.committee.committeeId = :committeeId "
            + "AND a.examDate IS NOT NULL "
            + "AND a.examDate BETWEEN :from AND :to "
            + "AND (:excludeAssignmentId IS NULL OR a.assignmentId <> :excludeAssignmentId)")
    List<GroupCommitteeAssignment> findConflictingByCommitteeAndWindow(
            @Param("committeeId") Long committeeId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("excludeAssignmentId") Long excludeAssignmentId);

    /**
     * Global conflict scan across any committee for a given window. Used by
     * ScheduleValidator.getConflictingAssignments(date).
     */
    @Query("SELECT a FROM GroupCommitteeAssignment a "
            + "WHERE a.examDate IS NOT NULL "
            + "AND a.examDate BETWEEN :from AND :to")
    List<GroupCommitteeAssignment> findConflictingByWindow(
            @Param("from") Instant from,
            @Param("to") Instant to);
}
