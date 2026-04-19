package com.spms.backend.repository;

import com.spms.backend.model.Submission;
import com.spms.backend.model.enums.DeliverableType;
import java.util.Optional;

public interface SubmissionRepository {

    Optional<Submission> findTopByGroupIdAndDeliverableTypeOrderByCreatedAtDesc(Long groupId, DeliverableType type);

    Submission save(Submission submission);
    
}
