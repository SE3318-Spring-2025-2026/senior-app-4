package com.spms.backend.repository;

import com.spms.backend.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    /**
     * Finds all reviews for a specific submission.
     */
    List<Review> findBySubmissionId(Long submissionId);
}
