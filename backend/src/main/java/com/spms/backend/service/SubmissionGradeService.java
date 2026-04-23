package com.spms.backend.service;

import com.spms.backend.dto.response.GradeItemDTO;
import com.spms.backend.dto.response.GradeListResponse;
import com.spms.backend.dto.request.GradeSubmissionRequest;
import com.spms.backend.dto.response.GradeSubmissionResponse;
import com.spms.backend.model.Committee;
import com.spms.backend.model.Group;
import com.spms.backend.model.GroupCommitteeAssignment;
import com.spms.backend.model.Submission;
import com.spms.backend.model.SubmissionGrade;
import com.spms.backend.model.User;
import com.spms.backend.model.enums.SubmissionStatus;
import com.spms.backend.repository.GroupCommitteeAssignmentRepository;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.SubmissionGradeRepository;
import com.spms.backend.repository.SubmissionRepository;
import com.spms.backend.repository.UserRepository;
import com.spms.backend.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SubmissionGradeService {

    private final SubmissionGradeRepository gradeRepository;
    private final SubmissionRepository submissionRepository;
    private final GroupCommitteeAssignmentRepository assignmentRepository;
    private final GroupRepository groupRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public SubmissionGradeService(
            SubmissionGradeRepository gradeRepository,
            SubmissionRepository submissionRepository,
            GroupCommitteeAssignmentRepository assignmentRepository,
            GroupRepository groupRepository,
            NotificationService notificationService,
            UserRepository userRepository) {
        this.gradeRepository = gradeRepository;
        this.submissionRepository = submissionRepository;
        this.assignmentRepository = assignmentRepository;
        this.groupRepository = groupRepository;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    public GradeListResponse getSubmissionGrades(Long submissionId, String userRole) {
        submissionRepository.findById(submissionId)
            .orElseThrow(() -> new IllegalArgumentException("Submission not found with ID: " + submissionId));
            
        List<SubmissionGrade> grades = gradeRepository.findBySubmissionId(submissionId);
        
        final int[] totalCommitteeMembers = {3}; // Default to 3, wrapped in array to be effectively final for lambda
        
        Submission sub = submissionRepository.findById(submissionId).orElse(null);
        if (sub != null) {
            assignmentRepository.findTopByGroupIdAndStatusOrderByAssignedAtDesc(sub.getGroupId(), "ASSIGNED")
                .ifPresent(assignment -> {
                    Committee committee = assignment.getCommittee();
                    totalCommitteeMembers[0] = committee.getAdvisors().size() + committee.getJuryMembers().size();
                });
        }

        int gradeCount = grades.size();
        boolean isGradingComplete = (gradeCount >= totalCommitteeMembers[0]);
        
        Double averageGrade = null;
        if (!grades.isEmpty() && isGradingComplete) {
            averageGrade = grades.stream().mapToDouble(SubmissionGrade::getScore).average().orElse(0.0);
        }

        List<GradeItemDTO> gradeItems = new ArrayList<>();
        
        if (!"STUDENT".equalsIgnoreCase(userRole)) {
            // BUG-1 Fix: Resolve professorName at read time using UserRepository
            gradeItems = grades.stream().map(g -> {
                String name = userRepository.findById(g.getProfessorId())
                        .map(User::getFullName)
                        .orElse("Unknown");
                return new GradeItemDTO(
                        g.getId(),
                        g.getProfessorId(),
                        name,
                        g.getScore(),
                        g.getFeedback(),
                        new ArrayList<>(),
                        g.getGradedAt()
                );
            }).collect(Collectors.toList());
            
            if (!isGradingComplete && !grades.isEmpty()) {
                averageGrade = grades.stream().mapToDouble(SubmissionGrade::getScore).average().orElse(0.0);
            }
        }

        GradeListResponse.GradeListData data = new GradeListResponse.GradeListData(
                averageGrade,
                gradeCount,
                totalCommitteeMembers[0],
                isGradingComplete,
                gradeItems
        );

        return new GradeListResponse("success", data);
    }

    @Transactional
    public GradeSubmissionResponse submitGrade(Long submissionId, Long professorId, GradeSubmissionRequest request) {

        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalStateException("Submission not found"));

        if (gradeRepository.existsBySubmissionIdAndProfessorId(submissionId, professorId)) {
            throw new IllegalArgumentException("Professor has already submitted a grade for this submission");
        }

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

        SubmissionGrade grade = new SubmissionGrade();
        grade.setSubmissionId(submissionId);
        grade.setProfessorId(professorId);
        grade.setScore(request.getGrade());
        grade.setFeedback(request.getFeedback());
        
        grade = gradeRepository.save(grade);

        int totalCommitteeMembers = committee.getAdvisors().size() + committee.getJuryMembers().size();
        List<SubmissionGrade> allGrades = gradeRepository.findBySubmissionId(submissionId);

        boolean isGradingComplete = false;
        if (allGrades.size() >= totalCommitteeMembers) {
            isGradingComplete = true;
            double average = allGrades.stream()
                    .mapToDouble(SubmissionGrade::getScore)
                    .average()
                    .orElse(0.0);

            submission.setFinalGrade(average);
            submission.setStatus(SubmissionStatus.GRADED);
            submissionRepository.save(submission);

            String notificationMsg = "Grading is complete for Submission " + submissionId + ". Final grade: "
                    + String.format("%.2f", average);
            
            notificationService.createSystemAlert(committee.getCreatedBy(), notificationMsg, "GRADING_COMPLETE",
                    "{\"submissionId\": " + submissionId + "}");

            Group group = groupRepository.findById(submission.getGroupId())
                    .orElseThrow(() -> new IllegalStateException("Group not found"));
            
            if (group.getLeader() != null) {
                notificationService.createSystemAlert(group.getLeader().getUserId(), notificationMsg, "GRADING_COMPLETE",
                        "{\"submissionId\": " + submissionId + "}");
            }
        }

        return new GradeSubmissionResponse("success", "Grade submitted successfully.", grade.getId(), isGradingComplete);
    }
}
