package com.spms.backend.repository;

import com.spms.backend.model.IssueValidationResult;
import org.springframework.data.jpa.repository.JpaRepository;
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
    List<IssueValidationResult> findBySprintIdAndTeamIdAndValidationStatus(Long sprintId, Long teamId, String validationStatus);
    Optional<IssueValidationResult> findTopByIssueKeyOrderByEvaluatedAtDesc(String issueKey);
}
