package com.spms.backend.service;

import com.spms.backend.dto.response.GradeItemDTO;
import com.spms.backend.dto.response.CriteriaScoreDTO;
import com.spms.backend.dto.response.GradeListResponse;
import com.spms.backend.dto.request.CriteriaScoreRequest;
import com.spms.backend.dto.request.GradeSubmissionRequest;
import com.spms.backend.dto.response.GradeSubmissionResponse;
import com.spms.backend.dto.response.SuccessResponse;
import com.spms.backend.model.Committee;
import com.spms.backend.model.Group;
import com.spms.backend.model.GroupCommitteeAssignment;
import com.spms.backend.model.Submission;
import com.spms.backend.model.SubmissionGrade;
import com.spms.backend.model.SubmissionGradeCriterionScore;
import com.spms.backend.model.User;
import com.spms.backend.model.GradingCriteria;
import com.spms.backend.model.enums.SubmissionStatus;
import com.spms.backend.model.enums.GradingType;
import com.spms.backend.repository.GroupCommitteeAssignmentRepository;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.GradingCriteriaRepository;
import com.spms.backend.repository.SubmissionGradeRepository;
import com.spms.backend.repository.SubmissionGradeCriterionScoreRepository;
import com.spms.backend.repository.SubmissionRepository;
import com.spms.backend.repository.UserRepository;
import com.spms.backend.service.NotificationService;
import com.spms.backend.repository.ScheduleRepository;
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
    private final ScheduleRepository scheduleRepository;
    private final GradingCriteriaRepository criteriaRepository;
    private final SubmissionGradeCriterionScoreRepository criterionScoreRepository;
    private final FinalGradeCalculationService finalGradeCalculationService;

    public SubmissionGradeService(
            SubmissionGradeRepository gradeRepository,
            SubmissionRepository submissionRepository,
            GroupCommitteeAssignmentRepository assignmentRepository,
            GroupRepository groupRepository,
            NotificationService notificationService,
            UserRepository userRepository,
            ScheduleRepository scheduleRepository,
            GradingCriteriaRepository criteriaRepository,
            SubmissionGradeCriterionScoreRepository criterionScoreRepository,
            FinalGradeCalculationService finalGradeCalculationService) {
        this.gradeRepository = gradeRepository;
        this.submissionRepository = submissionRepository;
        this.assignmentRepository = assignmentRepository;
        this.groupRepository = groupRepository;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.scheduleRepository = scheduleRepository;
        this.criteriaRepository = criteriaRepository;
        this.criterionScoreRepository = criterionScoreRepository;
        this.finalGradeCalculationService = finalGradeCalculationService;
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
                return new GradeItemDTO(
                        g.getId(),
                        g.getProfessorId(),
                        name,
                        g.getScore(),
                        g.getFeedback(),
                        loadCriteriaScores(g.getId()),
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

        if (gradeRepository.existsBySubmissionIdAndProfessorId(submissionId, professorId)) {
            throw new IllegalArgumentException("Professor has already submitted a grade for this submission");
        }

        RubricGrade rubricGrade = resolveRubricGrade(submission, request);

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
        grade.setScore(rubricGrade.score());
        grade.setFeedback(request.getFeedback());
        
        grade = gradeRepository.save(grade);
        saveCriterionScores(grade, rubricGrade.criterionScores());

        int totalCommitteeMembersCount = committee.getAdvisors().size() + committee.getJuryMembers().size();
        List<SubmissionGrade> allGrades = gradeRepository.findBySubmissionId(submissionId);

        boolean isGradingComplete = false;
        if (allGrades.size() >= totalCommitteeMembersCount) {
            isGradingComplete = true;
            // The submission grade is the committee average; the PDF final grade
            // formula is calculated at the team/student level by FinalGradeCalculationService.
            double average = allGrades.stream()
                    .mapToDouble(SubmissionGrade::getScore)
                    .average()
                    .orElse(0.0);

            submission.setFinalGrade(average);
            submission.setStatus(SubmissionStatus.GRADED);
            submissionRepository.save(submission);
            if (finalGradeCalculationService != null) {
                finalGradeCalculationService.recalculateGroupIfReady(submission.getGroupId());
            }

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

    @Transactional
    public SuccessResponse updateGrade(Long submissionId, Long gradeId, Long professorId, GradeSubmissionRequest request) {
        // 1. Validate grade exists → 404
        SubmissionGrade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new com.spms.backend.exception.NotFoundException("Grade not found with ID: " + gradeId));

        // 2. Validate grade belongs to specified submission → 404
        if (!grade.getSubmissionId().equals(submissionId)) {
            throw new com.spms.backend.exception.NotFoundException("Grade not found for the specified submission");
        }

        // 3. Ownership check: professor can only update own grade → 403
        if (!grade.getProfessorId().equals(professorId)) {
            throw new com.spms.backend.exception.ForbiddenException("Professor can only update their own grade record");
        }

        // 4. D10 Schedule Deadline Check → 403
        scheduleRepository.findTopByOrderByIdDesc().ifPresent(schedule -> {
            if (schedule.getGradingDeadline() != null
                    && java.time.Instant.now().isAfter(schedule.getGradingDeadline())) {
                throw new com.spms.backend.exception.ForbiddenException("Grading deadline has passed (D10).");
            }
        });

        // 5. Update the grade fields
        RubricGrade rubricGrade = resolveRubricGrade(submissionRepository.findById(submissionId)
                .orElseThrow(() -> new com.spms.backend.exception.NotFoundException("Submission not found")), request);
        grade.setScore(rubricGrade.score());
        grade.setFeedback(request.getFeedback());
        grade.setGradedAt(java.time.LocalDateTime.now());
        gradeRepository.save(grade);
        if (criterionScoreRepository != null) {
            criterionScoreRepository.deleteByGrade_Id(gradeId);
            saveCriterionScores(grade, rubricGrade.criterionScores());
        }

        // 6. Recalculate average if grading is complete
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
                        if (finalGradeCalculationService != null) {
                            finalGradeCalculationService.recalculateGroupIfReady(submission.getGroupId());
                        }
                    }
                });

        return new SuccessResponse("success", "Grade updated successfully.");
    }

    private RubricGrade resolveRubricGrade(Submission submission, GradeSubmissionRequest request) {
        if (request.getCriteriaScores() == null || request.getCriteriaScores().isEmpty()) {
            if (request.getGrade() == null) {
                throw new IllegalArgumentException("Either grade or criteriaScores must be provided");
            }
            requireScoreRange(request.getGrade(), "grade");
            return new RubricGrade(request.getGrade(), List.of());
        }

        if (criteriaRepository == null) {
            throw new IllegalStateException("Grading criteria repository is not available");
        }

        List<GradingCriteria> criteria = resolveCriteriaForSubmission(submission);
        if (criteria.isEmpty()) {
            throw new IllegalArgumentException("No grading criteria configured for " + submission.getDeliverableType());
        }

        double totalWeight = criteria.stream().mapToDouble(c -> c.getWeight() != null ? c.getWeight() : 0.0).sum();
        if (Math.abs(totalWeight - 100.0) > 0.01) {
            throw new IllegalArgumentException("Grading criteria weights for " + submission.getDeliverableType()
                    + " must sum to 100. Current total: " + totalWeight);
        }

        var criteriaById = criteria.stream().collect(Collectors.toMap(GradingCriteria::getId, c -> c));
        var submittedCriterionIds = new java.util.LinkedHashSet<Long>();
        List<SubmissionGradeCriterionScore> scores = new ArrayList<>();
        double finalScore = 0.0;
        for (CriteriaScoreRequest scoreRequest : request.getCriteriaScores()) {
            if (!submittedCriterionIds.add(scoreRequest.getCriterionId())) {
                throw new IllegalArgumentException("Duplicate criterion score: " + scoreRequest.getCriterionId());
            }
            GradingCriteria criterion = criteriaById.get(scoreRequest.getCriterionId());
            if (criterion == null) {
                throw new IllegalArgumentException("Criterion does not belong to this deliverable: "
                        + scoreRequest.getCriterionId());
            }
            double rawScore = resolveCriterionScore(criterion, scoreRequest);
            double weighted = rawScore * criterion.getWeight() / 100.0;

            SubmissionGradeCriterionScore entity = new SubmissionGradeCriterionScore();
            entity.setCriterion(criterion);
            entity.setRawScore(rawScore);
            entity.setWeightedScore(weighted);
            scores.add(entity);
            finalScore += weighted;
        }

        if (submittedCriterionIds.size() != criteriaById.size()) {
            var missing = new java.util.LinkedHashSet<>(criteriaById.keySet());
            missing.removeAll(submittedCriterionIds);
            throw new IllegalArgumentException("Missing scores for criteria: " + missing);
        }

        return new RubricGrade(Math.max(0.0, Math.min(100.0, finalScore)), scores);
    }

    private List<GradingCriteria> resolveCriteriaForSubmission(Submission submission) {
        List<GradingCriteria> criteria = criteriaRepository.findByDeliverableType(submission.getDeliverableType());
        if (criteria.isEmpty() && submission.getDeliverableType() == com.spms.backend.model.enums.DeliverableType.REVISED_PROPOSAL) {
            return criteriaRepository.findByDeliverableType(com.spms.backend.model.enums.DeliverableType.PROPOSAL);
        }
        return criteria;
    }

    private double resolveCriterionScore(GradingCriteria criterion, CriteriaScoreRequest scoreRequest) {
        if (scoreRequest.getScore() != null) {
            requireScoreRange(scoreRequest.getScore(), "criterion " + criterion.getId());
            if (criterion.getGradingType() == GradingType.BINARY
                    && scoreRequest.getScore() != 0.0
                    && scoreRequest.getScore() != 100.0) {
                throw new IllegalArgumentException("Binary criterion " + criterion.getId() + " must be 0 or 100");
            }
            return scoreRequest.getScore();
        }
        String grade = scoreRequest.getGrade();
        if (grade == null || grade.isBlank()) {
            throw new IllegalArgumentException("score or grade is required for criterion " + criterion.getId());
        }
        String normalized = grade.trim().toUpperCase();
        if (criterion.getGradingType() == GradingType.BINARY) {
            return switch (normalized) {
                case "S" -> 100.0;
                case "F" -> 0.0;
                default -> throw new IllegalArgumentException("Binary grade must be S or F for criterion " + criterion.getId());
            };
        }
        return switch (normalized) {
            case "A" -> 100.0;
            case "B" -> 80.0;
            case "C" -> 60.0;
            case "D" -> 50.0;
            case "F" -> 0.0;
            default -> throw new IllegalArgumentException("Soft grade must be A, B, C, D, or F for criterion " + criterion.getId());
        };
    }

    private void requireScoreRange(Double score, String label) {
        if (score == null || score < 0.0 || score > 100.0) {
            throw new IllegalArgumentException(label + " must be between 0 and 100");
        }
    }

    private void saveCriterionScores(SubmissionGrade grade, List<SubmissionGradeCriterionScore> scores) {
        if (criterionScoreRepository == null || scores == null || scores.isEmpty()) return;
        scores.forEach(score -> score.setGrade(grade));
        criterionScoreRepository.saveAll(scores);
    }

    private List<CriteriaScoreDTO> loadCriteriaScores(Long gradeId) {
        if (criterionScoreRepository == null) return new ArrayList<>();
        return criterionScoreRepository.findByGrade_Id(gradeId).stream()
                .map(score -> new CriteriaScoreDTO(
                        score.getCriterion().getId(),
                        score.getCriterion().getName(),
                        score.getRawScore()))
                .collect(Collectors.toList());
    }

    private record RubricGrade(Double score, List<SubmissionGradeCriterionScore> criterionScores) {}
}
