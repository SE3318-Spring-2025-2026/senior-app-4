package com.spms.backend.repository;

import com.spms.backend.model.Submission;
import com.spms.backend.model.enums.DeliverableType;
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

    Page<Submission> findByGroupId(Long groupId, Pageable pageable);

    List<Submission> findAllByGroupIdAndDeliverableType(Long groupId, DeliverableType deliverableType);

    /** Returns the full revision chain: the root + all its direct/indirect revisions ordered by version */
    @Query("SELECT s FROM Submission s WHERE s.id = :rootId OR s.parentSubmissionId = :rootId ORDER BY s.version ASC")
    List<Submission> findRevisionChain(@Param("rootId") Long rootId);

    Optional<Submission> findTopByParentSubmissionIdOrderByVersionDesc(Long parentSubmissionId);
}
