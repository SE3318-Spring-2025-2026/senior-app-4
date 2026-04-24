package com.spms.backend.service.impl;

import com.spms.backend.dto.response.ReviewDto;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.exception.ForbiddenException;
import com.spms.backend.model.GroupMember;
import com.spms.backend.model.Submission;
import com.spms.backend.repository.GroupMemberRepository;
import com.spms.backend.repository.ReviewRepository;
import com.spms.backend.repository.SubmissionRepository;
import com.spms.backend.service.ReviewService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final SubmissionRepository submissionRepository;
    private final GroupMemberRepository groupMemberRepository;

    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             SubmissionRepository submissionRepository,
                             GroupMemberRepository groupMemberRepository) {
        this.reviewRepository = reviewRepository;
        this.submissionRepository = submissionRepository;
        this.groupMemberRepository = groupMemberRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewDto> getReviewsForSubmission(Long submissionId, Long userId) {
        // 1. Fetch Submission
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new BadRequestException("Submission not found with ID: " + submissionId));

        // 2. Authorization Check: Students can only see their own group's reviews
        // Fetch student's group membership
        GroupMember member = groupMemberRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new ForbiddenException("You are not part of any group. Access denied."));

        // Verify if the submission belongs to the student's group
        if (!submission.getGroup().getId().equals(member.getGroup().getId())) {
            throw new ForbiddenException("You are not authorized to view reviews for this submission as it belongs to another group.");
        }

        // 3. Fetch and return reviews
        return reviewRepository.findBySubmissionId(submissionId).stream()
                .map(review -> new ReviewDto(
                        review.getId(),
                        review.getSubmissionId(),
                        review.getReviewerName(),
                        review.getComment(),
                        review.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }
}
