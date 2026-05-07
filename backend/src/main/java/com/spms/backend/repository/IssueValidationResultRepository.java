package com.spms.backend.repository;

import com.spms.backend.model.IssueValidationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IssueValidationResultRepository extends JpaRepository<IssueValidationResult, Long> {
    List<IssueValidationResult> findByJob_JobId(Long jobId);
    Optional<IssueValidationResult> findByJob_JobIdAndIssueKey(Long jobId, String issueKey);
    List<IssueValidationResult> findByJob_JobIdAndValidationStatus(Long jobId, String validationStatus);

    List<IssueValidationResult> findBySprintId(Long sprintId);
    List<IssueValidationResult> findBySprintIdAndTeamId(Long sprintId, Long teamId);
    Optional<IssueValidationResult> findTopByIssueKeyOrderByEvaluatedAtDesc(String issueKey);

    @Query("SELECT r FROM IssueValidationResult r WHERE r.sprintId = :sprintId AND r.teamId = :teamId " +
           "AND r.validationStatus = 'VALIDATED' AND r.evaluatedAt = " +
           "(SELECT MAX(r2.evaluatedAt) FROM IssueValidationResult r2 WHERE r2.sprintId = :sprintId " +
           "AND r2.teamId = :teamId AND r2.issueKey = r.issueKey AND r2.validationStatus = 'VALIDATED')")
    List<IssueValidationResult> findLatestValidatedBySprintIdAndTeamId(@Param("sprintId") Long sprintId,
                                                                        @Param("teamId") Long teamId);

    @Query("SELECT r.issueKey FROM IssueValidationResult r WHERE r.sprintId = :sprintId " +
           "AND r.teamId = :teamId AND r.validationStatus = 'VALIDATED'")
    List<String> findValidatedIssueKeysBySprintIdAndTeamId(@Param("sprintId") Long sprintId,
                                                            @Param("teamId") Long teamId);
}
