package com.spms.backend.repository;

import com.spms.backend.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByParentSubmissionIdOrderByIdAsc(Long parentSubmissionId);
}
