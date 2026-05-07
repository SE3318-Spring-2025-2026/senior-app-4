package com.spms.backend.repository;

import com.spms.backend.model.Submission;
import com.spms.backend.model.enums.DeliverableType;
import com.spms.backend.model.enums.SubmissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    Optional<Submission> findTopByGroupIdAndDeliverableTypeOrderByCreatedAtDesc(Long groupId, DeliverableType type);

    Optional<Submission> findTopByGroupIdAndDeliverableTypeAndStatusOrderByCreatedAtDesc(
            Long groupId, DeliverableType type, SubmissionStatus status);
    
    boolean existsByGroupIdAndDeliverableTypeAndParentSubmissionIdIsNull(Long groupId, DeliverableType deliverableType);

    Page<Submission> findByGroupId(Long groupId, Pageable pageable);

    List<Submission> findAllByGroupIdAndDeliverableType(Long groupId, DeliverableType deliverableType);

    List<Submission> findByGroupIdAndStatus(Long groupId, SubmissionStatus status);

    @Query("SELECT s FROM Submission s WHERE s.id = :rootId OR s.parentSubmissionId = :rootId ORDER BY s.version ASC")
    List<Submission> findRevisionChain(@Param("rootId") Long rootId);

    Optional<Submission> findTopByParentSubmissionIdOrderByVersionDesc(Long parentSubmissionId);

    // C-26: filter-aware queries
    @Query("SELECT s FROM Submission s WHERE " +
           "(:groupId IS NULL OR s.groupId = :groupId) AND " +
           "(:status IS NULL OR s.status = :status) AND " +
           "(:committeeId IS NULL OR s.committeeId = :committeeId) AND " +
           "(:deliverableType IS NULL OR s.deliverableType = :deliverableType)")
    Page<Submission> findWithFilters(
            @Param("groupId") Long groupId,
            @Param("status") SubmissionStatus status,
            @Param("committeeId") Long committeeId,
            @Param("deliverableType") DeliverableType deliverableType,
            Pageable pageable);

    // C-25: submissions for groups assigned to a committee (professor view)
    @Query("SELECT s FROM Submission s WHERE s.committeeId IN :committeeIds")
    Page<Submission> findByCommitteeIdIn(@Param("committeeIds") List<Long> committeeIds, Pageable pageable);

    @Query("SELECT s FROM Submission s WHERE s.committeeId IN :committeeIds AND " +
           "(:groupId IS NULL OR s.groupId = :groupId) AND " +
           "(:status IS NULL OR s.status = :status) AND " +
           "(:deliverableType IS NULL OR s.deliverableType = :deliverableType)")
    Page<Submission> findByCommitteeIdInWithFilters(
            @Param("committeeIds") List<Long> committeeIds,
            @Param("groupId") Long groupId,
            @Param("status") SubmissionStatus status,
            @Param("deliverableType") DeliverableType deliverableType,
            Pageable pageable);
}
