package com.spms.backend.repository;

import com.spms.backend.model.SubmissionAnnotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmissionAnnotationRepository extends JpaRepository<SubmissionAnnotation, Long> {

    List<SubmissionAnnotation> findBySubmissionIdOrderByStartOffsetAsc(Long submissionId);

    List<SubmissionAnnotation> findBySubmissionIdAndAdvisorId(Long submissionId, Long advisorId);

    void deleteBySubmissionId(Long submissionId);
}
