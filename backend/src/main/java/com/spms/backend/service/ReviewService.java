package com.spms.backend.service;

import com.spms.backend.dto.response.ReviewDto;
import java.util.List;

public interface ReviewService {
    /**
     * Retrieves all feedback and comments for a specific submission.
     * Access Control: Students can only view reviews for submissions belonging to their own group.
     */
    List<ReviewDto> getReviewsForSubmission(Long submissionId, Long userId);
}
