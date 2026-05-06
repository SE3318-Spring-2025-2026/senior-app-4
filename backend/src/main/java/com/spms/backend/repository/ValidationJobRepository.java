package com.spms.backend.repository;

import com.spms.backend.model.ValidationJob;
import com.spms.backend.model.ValidationJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface ValidationJobRepository extends JpaRepository<ValidationJob, Long> {
    boolean existsBySprint_IdAndTeam_IdAndJobStatusIn(Long sprintId, Long teamId, Collection<ValidationJobStatus> statuses);
    boolean existsBySprint_IdAndTeamIsNullAndJobStatusIn(Long sprintId, Collection<ValidationJobStatus> statuses);
    Optional<ValidationJob> findFirstBySprint_IdAndTeam_IdAndJobStatusInOrderByStartedAtDesc(Long sprintId, Long teamId, Collection<ValidationJobStatus> statuses);
    Optional<ValidationJob> findFirstBySprint_IdAndTeamIsNullAndJobStatusInOrderByStartedAtDesc(Long sprintId, Collection<ValidationJobStatus> statuses);
    Optional<ValidationJob> findFirstByParentJob_JobIdAndJobStatusIn(Long parentJobId, Collection<ValidationJobStatus> statuses);
    Optional<ValidationJob> findFirstBySprint_IdAndJobStatusIn(Long sprintId, Collection<ValidationJobStatus> statuses);
}
