package com.spms.backend.service.impl;

import com.spms.backend.dto.request.CreateReviewRequestDto;
import com.spms.backend.dto.response.ReviewDto;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.exception.ForbiddenException;
import com.spms.backend.exception.NotFoundException;
import com.spms.backend.model.GroupMember;
import com.spms.backend.model.Review;
import com.spms.backend.model.Submission;
import com.spms.backend.model.User;
import com.spms.backend.model.enums.ReviewStatus;
import com.spms.backend.model.enums.SubmissionStatus;
import com.spms.backend.repository.GroupMemberRepository;
import com.spms.backend.repository.ReviewRepository;
import com.spms.backend.repository.SubmissionRepository;
import com.spms.backend.repository.UserRepository;
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
    private final UserRepository userRepository;

    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             SubmissionRepository submissionRepository,
                             GroupMemberRepository groupMemberRepository,
                             UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.submissionRepository = submissionRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewDto> getReviewsForSubmission(Long submissionId, Long userId, String role) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new NotFoundException("Submission not found with ID: " + submissionId));

        // C-24: role-based access: STUDENT → group check; PROFESSOR/COORDINATOR → unrestricted
        if ("STUDENT".equalsIgnoreCase(role)) {
            GroupMember member = groupMemberRepository.findByUser_UserId(userId)
                    .orElseThrow(() -> new ForbiddenException("You are not part of any group. Access denied."));
            if (!submission.getGroupId().equals(member.getGroup().getId())) {
                throw new ForbiddenException("You are not authorized to view reviews for this submission.");
            }
        } else if (!"PROFESSOR".equalsIgnoreCase(role) && !"COORDINATOR".equalsIgnoreCase(role)) {
            throw new ForbiddenException("Role '" + role + "' is not authorized to view reviews.");
        }

        return reviewRepository.findBySubmissionId(submissionId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReviewDto createReview(Long submissionId, Long reviewerId, String reviewerRole, CreateReviewRequestDto request) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new NotFoundException("Submission not found with ID: " + submissionId));

        if (!"PROFESSOR".equalsIgnoreCase(reviewerRole) && !"COORDINATOR".equalsIgnoreCase(reviewerRole)) {
            throw new ForbiddenException("Only professors and coordinators can post reviews.");
        }

        ReviewStatus status;
        try {
            status = ReviewStatus.valueOf(request.status().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: " + request.status() + ". Must be APPROVED or REVISION_REQUESTED.");
        }

        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new NotFoundException("Reviewer not found: " + reviewerId));

        Review review = new Review();
        review.setSubmissionId(submissionId);
        review.setReviewerId(reviewerId);
        review.setReviewerName(reviewer.getFullName());
        review.setComment(request.comment());
        review.setStatus(status);

        Review saved = reviewRepository.save(review);

        // Advance submission status based on review decision
        if (status == ReviewStatus.REVISION_REQUESTED) {
            submission.setStatus(SubmissionStatus.REVISION_REQUESTED);
        } else if (status == ReviewStatus.APPROVED) {
            submission.setStatus(SubmissionStatus.APPROVED);
        }
        submissionRepository.save(submission);

        return toDto(saved);
    }

    private ReviewDto toDto(Review review) {
        return new ReviewDto(
                review.getId(),
                review.getSubmissionId(),
                review.getReviewerName(),
                review.getComment(),
                review.getStatus() != null ? review.getStatus().name() : null,
                review.getCreatedAt()
        );
    }
}
