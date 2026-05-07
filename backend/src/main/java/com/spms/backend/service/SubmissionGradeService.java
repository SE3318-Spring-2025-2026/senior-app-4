package com.spms.backend.service;

import com.spms.backend.dto.request.CriterionScoreRequest;
import com.spms.backend.dto.request.GradeSubmissionRequest;
import com.spms.backend.dto.response.CriteriaScoreDTO;
import com.spms.backend.dto.response.GradeItemDTO;
import com.spms.backend.dto.response.GradeListResponse;
import com.spms.backend.dto.response.GradeSubmissionResponse;
import com.spms.backend.dto.response.SuccessResponse;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.model.Committee;
import com.spms.backend.model.GradeCriterionScore;
import com.spms.backend.model.GradingCriteria;
import com.spms.backend.model.Group;
import com.spms.backend.model.GroupCommitteeAssignment;
import com.spms.backend.model.Submission;
import com.spms.backend.model.SubmissionGrade;
import com.spms.backend.model.User;
import com.spms.backend.model.enums.SubmissionStatus;
import com.spms.backend.repository.GradeCriterionScoreRepository;
import com.spms.backend.repository.GradingCriteriaRepository;
import com.spms.backend.repository.GroupCommitteeAssignmentRepository;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.SubmissionGradeRepository;
import com.spms.backend.repository.SubmissionRepository;
import com.spms.backend.repository.UserRepository;
import com.spms.backend.repository.ScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SubmissionGradeService {

    private final SubmissionGradeRepository gradeRepository;
    private final SubmissionRepository submissionRepository;
    private final GroupCommitteeAssignmentRepository assignmentRepository;
    private final GroupRepository groupRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final GradingCriteriaRepository criteriaRepository;
    private final GradeCriterionScoreRepository criterionScoreRepository;

    public SubmissionGradeService(
            SubmissionGradeRepository gradeRepository,
            SubmissionRepository submissionRepository,
            GroupCommitteeAssignmentRepository assignmentRepository,
            GroupRepository groupRepository,
            NotificationService notificationService,
            UserRepository userRepository,
            ScheduleRepository scheduleRepository,
            GradingCriteriaRepository criteriaRepository,
            GradeCriterionScoreRepository criterionScoreRepository) {
        this.gradeRepository = gradeRepository;
        this.submissionRepository = submissionRepository;
        this.assignmentRepository = assignmentRepository;
        this.groupRepository = groupRepository;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.scheduleRepository = scheduleRepository;
        this.criteriaRepository = criteriaRepository;
        this.criterionScoreRepository = criterionScoreRepository;
    }

    public GradeListResponse getSubmissionGrades(Long submissionId, String userRole) {
        submissionRepository.findById(submissionId)
            .orElseThrow(() -> new IllegalArgumentException("Submission not found with ID: " + submissionId));

        List<SubmissionGrade> grades = gradeRepository.findBySubmissionId(submissionId);

        final int[] totalCommitteeMembers = {3};

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
            gradeItems = grades.stream().map(g -> {
                String name = userRepository.findById(g.getProfessorId())
                        .map(User::getFullName)
                        .orElse("Unknown");

                List<GradeCriterionScore> rawScores = criterionScoreRepository.findByGradeId(g.getId());
                List<CriteriaScoreDTO> criteriaScores = rawScores.stream().map(cs -> {
                    String criterionName = criteriaRepository.findById(cs.getCriterionId())
                            .map(GradingCriteria::getName)
                            .orElse("Unknown");
                    return new CriteriaScoreDTO(cs.getCriterionId(), criterionName, cs.getScore());
                }).collect(Collectors.toList());

                return new GradeItemDTO(
                        g.getId(),
                        g.getProfessorId(),
                        name,
                        g.getScore(),
                        g.getFeedback(),
                        criteriaScores,
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

        if (submission.getStatus() == SubmissionStatus.GRADED) {
            throw new com.spms.backend.exception.ConflictException("Submission is already fully graded.");
        }

        scheduleRepository.findTopByOrderByIdDesc().ifPresent(schedule -> {
            if (schedule.getGradingDeadline() != null
                    && java.time.Instant.now().isAfter(schedule.getGradingDeadline())) {
                throw new com.spms.backend.exception.ForbiddenException("Grading deadline has passed (D10).");
            }
        });

        boolean criterionBased = request.getCriterionScores() != null && !request.getCriterionScores().isEmpty();
        if (!criterionBased && request.getGrade() == null) {
            throw new BadRequestException("Either grade or criterionScores must be provided.");
        }

        if (gradeRepository.existsBySubmissionIdAndProfessorId(submissionId, professorId)) {
            throw new IllegalArgumentException("Professor has already submitted a grade for this submission");
        }

        // Check if professor is the direct group advisor
        boolean isDirectAdvisor = groupRepository.findById(submission.getGroupId())
                .map(g -> g.getAdvisor() != null && g.getAdvisor().getUserId().equals(professorId))
                .orElse(false);

        GroupCommitteeAssignment assignment = assignmentRepository
                .findTopByGroupIdAndStatusOrderByAssignedAtDesc(submission.getGroupId(), "ASSIGNED")
                .orElse(null);

        boolean isAuthorized = isDirectAdvisor;
        Committee committee = null;

        if (assignment != null) {
            committee = assignment.getCommittee();
            isAuthorized = isAuthorized ||
                    committee.getAdvisors().stream()
                            .anyMatch(adv -> adv.getAdvisor().getUserId().equals(professorId)) ||
                    committee.getJuryMembers().stream()
                            .anyMatch(jury -> jury.getJuryMember().getUserId().equals(professorId));
        }

        if (!isAuthorized) {
            throw new SecurityException("Professor is not authorized to grade this submission");
        }

        SubmissionGrade grade = new SubmissionGrade();
        grade.setSubmissionId(submissionId);
        grade.setProfessorId(professorId);
        grade.setScore(criterionBased ? 0.0 : request.getGrade());
        grade.setFeedback(request.getFeedback());
        grade = gradeRepository.save(grade);

        if (criterionBased) {
            grade.setScore(saveCriterionScoresAndComputeWeightedAverage(
                    grade.getId(), submission, request.getCriterionScores()));
            grade = gradeRepository.save(grade);
        }

        int totalCommitteeMembersCount = committee != null
                ? committee.getAdvisors().size() + committee.getJuryMembers().size()
                : 1;
        List<SubmissionGrade> allGrades = gradeRepository.findBySubmissionId(submissionId);

        boolean isGradingComplete = false;
        if (allGrades.size() >= totalCommitteeMembersCount) {
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

            if (committee != null) {
                notificationService.createSystemAlert(committee.getCreatedBy(), notificationMsg, "GRADING_COMPLETE",
                        "{\"submissionId\": " + submissionId + "}");
            }

            Group group = groupRepository.findById(submission.getGroupId())
                    .orElseThrow(() -> new IllegalStateException("Group not found"));

            if (group.getLeader() != null) {
                notificationService.createSystemAlert(group.getLeader().getUserId(), notificationMsg, "GRADING_COMPLETE",
                        "{\"submissionId\": " + submissionId + "}");
            }
        }

        return new GradeSubmissionResponse("success", "Grade submitted successfully.", grade.getId(), isGradingComplete);
    }

    @Transactional
    public SuccessResponse updateGrade(Long submissionId, Long gradeId, Long professorId, GradeSubmissionRequest request) {
        SubmissionGrade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new com.spms.backend.exception.NotFoundException("Grade not found with ID: " + gradeId));

        if (!grade.getSubmissionId().equals(submissionId)) {
            throw new com.spms.backend.exception.NotFoundException("Grade not found for the specified submission");
        }

        if (!grade.getProfessorId().equals(professorId)) {
            throw new com.spms.backend.exception.ForbiddenException("Professor can only update their own grade record");
        }

        scheduleRepository.findTopByOrderByIdDesc().ifPresent(schedule -> {
            if (schedule.getGradingDeadline() != null
                    && java.time.Instant.now().isAfter(schedule.getGradingDeadline())) {
                throw new com.spms.backend.exception.ForbiddenException("Grading deadline has passed (D10).");
            }
        });

        boolean criterionBased = request.getCriterionScores() != null && !request.getCriterionScores().isEmpty();
        if (!criterionBased && request.getGrade() == null) {
            throw new BadRequestException("Either grade or criterionScores must be provided.");
        }

        grade.setFeedback(request.getFeedback());
        grade.setGradedAt(java.time.LocalDateTime.now());

        if (criterionBased) {
            criterionScoreRepository.deleteByGradeId(gradeId);
            Submission submission = submissionRepository.findById(submissionId)
                    .orElseThrow(() -> new com.spms.backend.exception.NotFoundException("Submission not found"));
            grade.setScore(saveCriterionScoresAndComputeWeightedAverage(
                    gradeId, submission, request.getCriterionScores()));
        } else {
            grade.setScore(request.getGrade());
        }

        gradeRepository.save(grade);

        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new com.spms.backend.exception.NotFoundException("Submission not found"));

        assignmentRepository
                .findTopByGroupIdAndStatusOrderByAssignedAtDesc(submission.getGroupId(), "ASSIGNED")
                .ifPresent(assignment -> {
                    Committee committee = assignment.getCommittee();
                    int totalMembers = committee.getAdvisors().size() + committee.getJuryMembers().size();
                    List<SubmissionGrade> allGrades = gradeRepository.findBySubmissionId(submissionId);

                    if (allGrades.size() >= totalMembers) {
                        double average = allGrades.stream()
                                .mapToDouble(SubmissionGrade::getScore)
                                .average()
                                .orElse(0.0);
                        submission.setFinalGrade(average);
                        submission.setStatus(SubmissionStatus.GRADED);
                        submissionRepository.save(submission);
                    }
                });

        return new SuccessResponse("success", "Grade updated successfully.");
    }

    private double saveCriterionScoresAndComputeWeightedAverage(
            Long gradeId, Submission submission, List<CriterionScoreRequest> criterionScores) {

        List<GradingCriteria> criteria = criteriaRepository.findByDeliverableType(submission.getDeliverableType());
        Map<Long, GradingCriteria> criteriaMap = criteria.stream()
                .collect(Collectors.toMap(GradingCriteria::getId, c -> c));

        Set<Long> providedIds = criterionScores.stream()
                .map(CriterionScoreRequest::criterionId)
                .collect(Collectors.toSet());

        Set<Long> expectedIds = criteriaMap.keySet();
        if (!providedIds.containsAll(expectedIds)) {
            Set<Long> missing = expectedIds.stream()
                    .filter(id -> !providedIds.contains(id))
                    .collect(Collectors.toSet());
            throw new BadRequestException("Missing scores for criterion IDs: " + missing);
        }

        double totalWeight = 0.0;
        double weightedSum = 0.0;

        for (CriterionScoreRequest cs : criterionScores) {
            GradingCriteria criterion = criteriaMap.get(cs.criterionId());
            if (criterion == null) continue;

            GradeCriterionScore row = new GradeCriterionScore();
            row.setGradeId(gradeId);
            row.setCriterionId(cs.criterionId());
            row.setScore(cs.score());
            criterionScoreRepository.save(row);

            totalWeight += criterion.getWeight();
            weightedSum += cs.score() * criterion.getWeight();
        }

        return totalWeight > 0 ? weightedSum / totalWeight : 0.0;
    }
}
