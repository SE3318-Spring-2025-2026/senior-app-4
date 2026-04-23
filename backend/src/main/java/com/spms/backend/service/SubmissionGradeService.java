package com.spms.backend.service;

import com.spms.backend.dto.request.GradeSubmissionRequest;
import com.spms.backend.dto.response.GradeSubmissionResponse;
import com.spms.backend.model.Committee;
import com.spms.backend.model.Group;
import com.spms.backend.model.GroupCommitteeAssignment;
import com.spms.backend.model.Submission;
import com.spms.backend.model.Grade;
import com.spms.backend.model.enums.SubmissionStatus;
import com.spms.backend.repository.GroupCommitteeAssignmentRepository;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.GradeRepository;
import com.spms.backend.repository.SubmissionRepository;
import com.spms.backend.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SubmissionGradeService {

    private final SubmissionRepository submissionRepository;
    private final GradeRepository gradeRepository;
    private final GroupCommitteeAssignmentRepository assignmentRepository;
    private final GroupRepository groupRepository;
    private final NotificationService notificationService;

    public SubmissionGradeService(
            SubmissionRepository submissionRepository,
            GradeRepository gradeRepository,
            GroupCommitteeAssignmentRepository assignmentRepository,
            GroupRepository groupRepository,
            NotificationService notificationService) {
        this.submissionRepository = submissionRepository;
        this.gradeRepository = gradeRepository;
        this.assignmentRepository = assignmentRepository;
        this.groupRepository = groupRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public GradeSubmissionResponse submitGrade(Long submissionId, Long professorId, GradeSubmissionRequest request) {

        // 1. Check if submission exists
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalStateException("Submission not found"));

        // 2. Check if this professor already submitted a grade
        if (gradeRepository.existsBySubmissionIdAndProfessorId(submissionId, professorId)) {
            throw new IllegalArgumentException("Professor has already submitted a grade for this submission");
        }

        // Process 1: Fetch GroupCommitteeAssignment and verify professorId is part of
        // the assigned committee (Advisor/Jury).
        GroupCommitteeAssignment assignment = assignmentRepository
                .findTopByGroupIdAndStatusOrderByAssignedAtDesc(submission.getGroupId(), "ASSIGNED")
                .orElseThrow(() -> new IllegalStateException("No active committee assigned to this group"));

        Committee committee = assignment.getCommittee();

        boolean isAuthorized = committee.getAdvisors().stream()
                .anyMatch(adv -> adv.getAdvisor().getUserId().equals(professorId)) ||
                committee.getJuryMembers().stream()
                        .anyMatch(jury -> jury.getJuryMember().getUserId().equals(professorId));

        if (!isAuthorized) {
            throw new SecurityException("Professor is not authorized to grade this submission");
        }

        // 4. Save the grade
        Grade grade = new Grade(submissionId, professorId, request.getGrade(),
                request.getFeedback());
        grade = gradeRepository.save(grade);

        // Process 1: Retrieve the actual total number of committee members assigned to
        // this group.
        int totalCommitteeMembers = committee.getAdvisors().size() + committee.getJuryMembers().size();

        List<Grade> allGrades = gradeRepository.findBySubmissionId(submissionId);

        boolean isGradingComplete = false;
        if (allGrades.size() == totalCommitteeMembers) {
            isGradingComplete = true;
            double average = allGrades.stream()
                    .mapToDouble(Grade::getScore)
                    .average()
                    .orElse(0.0);

            // Process 2: Update Submission model to include finalGrade and set its status
            // to GRADED.
            submission.setFinalGrade(average);
            submission.setStatus(SubmissionStatus.GRADED);
            submissionRepository.save(submission);

            // Process 6: Trigger gradingComplete notification to the group/coordinator via
            // NotificationService.
            String notificationMsg = "Grading is complete for Submission " + submissionId + ". Final grade: "
                    + String.format("%.2f", average);
            
            // Notify Coordinator (FIX-3)
            notificationService.createSystemAlert(committee.getCreatedBy(), notificationMsg, "GRADING_COMPLETE",
                    "{\"submissionId\": " + submissionId + "}");

            // Notify Group Leader (FIX-3)
            Group group = groupRepository.findById(submission.getGroupId())
                    .orElseThrow(() -> new IllegalStateException("Group not found for submission: " + submission.getGroupId()));
            
            if (group.getLeader() != null) {
                notificationService.createSystemAlert(group.getLeader().getUserId(), notificationMsg, "GRADING_COMPLETE",
                        "{\"submissionId\": " + submissionId + "}");
            }
        }

        return new GradeSubmissionResponse("success", "Grade submitted successfully.", grade.getId(), isGradingComplete);
    }
}
