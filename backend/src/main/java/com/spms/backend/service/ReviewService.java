package com.spms.backend.service;

import com.spms.backend.dto.request.CreateReviewRequestDto;
import com.spms.backend.dto.response.ReviewDto;

import java.util.List;

public interface ReviewService {
    List<ReviewDto> getReviewsForSubmission(Long submissionId, Long userId, String role);
    ReviewDto createReview(Long submissionId, Long reviewerId, String reviewerRole, CreateReviewRequestDto request);
}
