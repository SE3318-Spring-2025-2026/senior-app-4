package com.spms.backend.repository;

import com.spms.backend.model.Submission;
import com.spms.backend.model.enums.DeliverableType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    Optional<Submission> findTopByGroupIdAndDeliverableTypeOrderByCreatedAtDesc(Long groupId, DeliverableType type);

    Page<Submission> findByGroupId(Long groupId, Pageable pageable);
}
