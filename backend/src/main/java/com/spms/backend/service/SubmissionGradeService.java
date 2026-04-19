package com.spms.backend.service;

import com.spms.backend.dto.request.GradeSubmissionRequest;
import com.spms.backend.dto.response.GradeSubmissionResponse;
import com.spms.backend.model.Submission;
import com.spms.backend.model.SubmissionGrade;
import com.spms.backend.repository.SubmissionGradeRepository;
import com.spms.backend.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SubmissionGradeService {

    private final SubmissionRepository submissionRepository;
    private final SubmissionGradeRepository gradeRepository;

    public SubmissionGradeService(
            SubmissionRepository submissionRepository,
            SubmissionGradeRepository gradeRepository) {
        this.submissionRepository = submissionRepository;
        this.gradeRepository = gradeRepository;
    }

    @Transactional
    public GradeSubmissionResponse submitGrade(Long submissionId, Long reviewerId, GradeSubmissionRequest request) {
        
        // 1. Check if submission exists
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalStateException("Submission not found"));

        // 2. Check if this reviewer already submitted a grade
        if (gradeRepository.existsBySubmissionIdAndReviewerId(submissionId, reviewerId)) {
            throw new IllegalArgumentException("Reviewer has already submitted a grade for this submission");
        }

        // TODO: [Process 1] Fetch GroupCommitteeAssignment and verify reviewerId is part of the assigned committee (Advisor/Jury).
        // For now, assuming authorized.
        
        // 4. Save the grade
        SubmissionGrade grade = new SubmissionGrade(submissionId, reviewerId, request.getScore(), request.getComments());
        grade = gradeRepository.save(grade);

        // TODO: [Process 1] Retrieve the actual total number of committee members assigned to this group.
        int totalCommitteeMembers = 3; // Placeholder

        List<SubmissionGrade> allGrades = gradeRepository.findBySubmissionId(submissionId);
        
        GradeSubmissionResponse response = new GradeSubmissionResponse();
        response.setGradeId(grade.getGradeId());
        response.setSubmissionId(submissionId);
        response.setReviewerId(reviewerId);
        response.setScore(grade.getScore());
        response.setComments(grade.getComments());
        response.setGradedAt(grade.getGradedAt());
        response.setIsGradingComplete(false);

        if (allGrades.size() == totalCommitteeMembers) {
            double average = allGrades.stream()
                    .mapToDouble(SubmissionGrade::getScore)
                    .average()
                    .orElse(0.0);

            // TODO: [Process 2] Update Submission model to include finalGrade and set its status to GRADED.
            // submission.setFinalGrade(average);
            // submission.setStatus(SubmissionStatus.GRADED);
            // submissionRepository.save(submission);

            response.setCalculatedAverage(average);
            response.setIsGradingComplete(true);

            // TODO: [Process 6] Trigger gradingComplete notification to the group/coordinator via NotificationService.
            // notificationService.createSystemAlert(...);
        }

        return response;
    }
}
