package com.spms.backend.service;

import com.spms.backend.dto.request.SprintAdvisorGradeRequest;
import com.spms.backend.dto.request.SprintConfigRequest;
import com.spms.backend.dto.request.SprintDeliverableWeightsRequest;
import com.spms.backend.dto.response.FinalGradeResponse;
import com.spms.backend.dto.response.AiCodeReviewSuggestionResponse;
import com.spms.backend.dto.response.SprintConfigResponse;
import com.spms.backend.dto.response.SprintAdvisorGradeResponse;
import com.spms.backend.dto.response.SprintDeliverableWeightResponse;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.exception.NotFoundException;
import com.spms.backend.model.*;
import com.spms.backend.model.enums.DeliverableType;
import com.spms.backend.model.enums.SubmissionStatus;
import com.spms.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FinalGradeCalculationService {

    private static final Logger logger = LoggerFactory.getLogger(FinalGradeCalculationService.class);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final String VALIDATED = "VALIDATED";

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final SprintRepository sprintRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final SubmissionRepository submissionRepository;
    private final SprintAdvisorGradeRepository sprintAdvisorGradeRepository;
    private final SprintDeliverableWeightRepository sprintDeliverableWeightRepository;
    private final TeamFinalGradeRepository teamFinalGradeRepository;
    private final StudentFinalGradeRepository studentFinalGradeRepository;
    private final SprintIssueTrackingRepository sprintIssueTrackingRepository;
    private final IssueValidationResultRepository issueValidationResultRepository;
    private final NotificationService notificationService;

    public FinalGradeCalculationService(
            GroupRepository groupRepository,
            UserRepository userRepository,
            SprintRepository sprintRepository,
            GroupMemberRepository groupMemberRepository,
            SubmissionRepository submissionRepository,
            SprintAdvisorGradeRepository sprintAdvisorGradeRepository,
            SprintDeliverableWeightRepository sprintDeliverableWeightRepository,
            TeamFinalGradeRepository teamFinalGradeRepository,
            StudentFinalGradeRepository studentFinalGradeRepository,
            SprintIssueTrackingRepository sprintIssueTrackingRepository,
            IssueValidationResultRepository issueValidationResultRepository,
            NotificationService notificationService) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.sprintRepository = sprintRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.submissionRepository = submissionRepository;
        this.sprintAdvisorGradeRepository = sprintAdvisorGradeRepository;
        this.sprintDeliverableWeightRepository = sprintDeliverableWeightRepository;
        this.teamFinalGradeRepository = teamFinalGradeRepository;
        this.studentFinalGradeRepository = studentFinalGradeRepository;
        this.sprintIssueTrackingRepository = sprintIssueTrackingRepository;
        this.issueValidationResultRepository = issueValidationResultRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<SprintConfigResponse> listSprints() {
        return sprintRepository.findAllByOrderByStartDateAscIdAsc()
                .stream()
                .map(this::toSprintConfigResponse)
                .toList();
    }

    @Transactional
    public SprintConfigResponse createSprint(SprintConfigRequest request) {
        validateSprintRequest(request);
        Sprint sprint = new Sprint();
        applySprintRequest(sprint, request);
        return toSprintConfigResponse(sprintRepository.save(sprint));
    }

    @Transactional
    public SprintConfigResponse updateSprint(Long sprintId, SprintConfigRequest request) {
        validateSprintRequest(request);
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new NotFoundException("Sprint not found: " + sprintId));
        applySprintRequest(sprint, request);
        Sprint saved = sprintRepository.save(sprint);
        groupRepository.findAll().forEach(group -> recalculateGroupIfReady(group.getId()));
        return toSprintConfigResponse(saved);
    }

    @Transactional
    public SprintAdvisorGradeResponse saveSprintAdvisorGrade(SprintAdvisorGradeRequest request, Long advisorId) {
        Group group = groupRepository.findById(request.groupId())
                .orElseThrow(() -> new NotFoundException("Group not found: " + request.groupId()));
        Sprint sprint = sprintRepository.findById(request.sprintId())
                .orElseThrow(() -> new NotFoundException("Sprint not found: " + request.sprintId()));
        User advisor = userRepository.findById(advisorId)
                .orElseThrow(() -> new NotFoundException("Advisor not found: " + advisorId));

        String scrum = normalizeSoftGrade(request.scrumGrade());
        String codeReview = normalizeSoftGrade(request.codeReviewGrade());

        SprintAdvisorGrade grade = sprintAdvisorGradeRepository
                .findByGroup_IdAndSprint_IdAndAdvisor_UserId(group.getId(), sprint.getId(), advisorId)
                .orElseGet(SprintAdvisorGrade::new);
        grade.setGroup(group);
        grade.setSprint(sprint);
        grade.setAdvisor(advisor);
        grade.setScrumGrade(scrum);
        grade.setCodeReviewGrade(codeReview);

        SprintAdvisorGrade saved = sprintAdvisorGradeRepository.save(grade);
        recalculateGroupIfReady(group.getId());
        return toSprintAdvisorGradeResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SprintAdvisorGradeResponse> getSprintAdvisorGrades(Long groupId) {
        groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found: " + groupId));
        return sprintAdvisorGradeRepository.findByGroup_Id(groupId).stream()
                .map(this::toSprintAdvisorGradeResponse)
                .toList();
    }

    @Transactional
    public SprintDeliverableWeightResponse replaceSprintDeliverableWeights(SprintDeliverableWeightsRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new BadRequestException("At least one sprint deliverable weight is required.");
        }

        BigDecimal total = request.items().stream()
                .map(SprintDeliverableWeightsRequest.Item::weight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        requireTotalWeight(total, "Sprint deliverable weights");

        Set<String> seen = new HashSet<>();
        List<SprintDeliverableWeight> entities = new ArrayList<>();
        for (SprintDeliverableWeightsRequest.Item item : request.items()) {
            DeliverableType type = parseDeliverableType(item.deliverableType());
            String key = item.sprintId() + "::" + type.name();
            if (!seen.add(key)) {
                throw new BadRequestException("Duplicate sprint/deliverable weight: " + key);
            }
            Sprint sprint = sprintRepository.findById(item.sprintId())
                    .orElseThrow(() -> new NotFoundException("Sprint not found: " + item.sprintId()));
            SprintDeliverableWeight weight = new SprintDeliverableWeight();
            weight.setSprint(sprint);
            weight.setDeliverableType(type);
            weight.setWeight(scale2(item.weight()));
            entities.add(weight);
        }

        sprintDeliverableWeightRepository.deleteAllInBatch();
        sprintDeliverableWeightRepository.saveAll(entities);
        groupRepository.findAll().forEach(group -> recalculateGroupIfReady(group.getId()));
        return getSprintDeliverableWeights();
    }

    @Transactional(readOnly = true)
    public SprintDeliverableWeightResponse getSprintDeliverableWeights() {
        List<SprintDeliverableWeightResponse.Item> items = sprintDeliverableWeightRepository
                .findAllByOrderByDeliverableTypeAscSprint_IdAsc()
                .stream()
                .map(w -> new SprintDeliverableWeightResponse.Item(
                        w.getId(),
                        w.getSprint().getId(),
                        w.getSprint().getSprintName(),
                        w.getDeliverableType().name(),
                        w.getWeight()))
                .toList();
        return new SprintDeliverableWeightResponse("success", items);
    }

    @Transactional(readOnly = true)
    public AiCodeReviewSuggestionResponse getAiCodeReviewSuggestion(Long groupId, Long sprintId) {
        groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found: " + groupId));
        sprintRepository.findById(sprintId)
                .orElseThrow(() -> new NotFoundException("Sprint not found: " + sprintId));
        BigDecimal score = resolveAiCodeReviewScore(groupId, sprintId);
        if (score == null) {
            throw new NotFoundException("No AI validation result exists for this group/sprint.");
        }
        return new AiCodeReviewSuggestionResponse("success",
                new AiCodeReviewSuggestionResponse.Data(groupId, sprintId, scale2(score), scoreToSoftGrade(score)));
    }

    @Transactional
    public FinalGradeResponse recalculateGroup(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found: " + groupId));

        List<SprintDeliverableWeight> weights = sprintDeliverableWeightRepository.findAllByOrderByDeliverableTypeAscSprint_IdAsc();
        if (weights.isEmpty()) {
            throw new BadRequestException("Coordinator must configure sprint deliverable weights first.");
        }
        requireTotalWeight(weights.stream().map(SprintDeliverableWeight::getWeight).reduce(BigDecimal.ZERO, BigDecimal::add),
                "Sprint deliverable weights");

        Map<DeliverableType, List<SprintDeliverableWeight>> byDeliverable = weights.stream()
                .collect(Collectors.groupingBy(SprintDeliverableWeight::getDeliverableType,
                        () -> new EnumMap<>(DeliverableType.class), Collectors.toList()));

        List<FinalGradeResponse.DeliverableBreakdown> breakdowns = new ArrayList<>();
        BigDecimal teamGrade = BigDecimal.ZERO;

        for (Map.Entry<DeliverableType, List<SprintDeliverableWeight>> entry : byDeliverable.entrySet()) {
            DeliverableType deliverableType = entry.getKey();
            List<SprintDeliverableWeight> deliverableWeights = entry.getValue();
            BigDecimal finalWeight = sumWeights(deliverableWeights);
            BigDecimal rawGrade = resolveRawDeliverableGrade(groupId, deliverableType);
            BigDecimal scrumAverage = resolveWeightedSprintAverage(groupId, deliverableWeights, SprintMetric.SCRUM);
            BigDecimal codeReviewAverage = resolveWeightedSprintAverage(groupId, deliverableWeights, SprintMetric.CODE_REVIEW);
            BigDecimal scalar = scrumAverage.add(codeReviewAverage)
                    .divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP)
                    .divide(HUNDRED, 4, RoundingMode.HALF_UP);
            BigDecimal scaledGrade = rawGrade.multiply(scalar).setScale(2, RoundingMode.HALF_UP);
            BigDecimal contribution = scaledGrade.multiply(finalWeight)
                    .divide(HUNDRED, 2, RoundingMode.HALF_UP);

            teamGrade = teamGrade.add(contribution);
            breakdowns.add(new FinalGradeResponse.DeliverableBreakdown(
                    deliverableType.name(),
                    scale2(rawGrade),
                    scale2(scrumAverage),
                    scale2(codeReviewAverage),
                    scalar.setScale(4, RoundingMode.HALF_UP),
                    scaledGrade,
                    scale2(finalWeight),
                    contribution));
        }

        teamGrade = scale2(teamGrade);
        TeamFinalGrade teamFinalGrade = upsertTeamGrade(group, teamGrade);
        StudentGradeUpdate studentUpdate = upsertStudentGrades(group, breakdowns, byDeliverable,
                !teamFinalGrade.isPublished());
        if (studentUpdate.changed() && teamFinalGrade.isPublished()) {
            teamFinalGrade.setPublished(false);
            teamFinalGrade.setPublishedAt(null);
            teamFinalGrade = teamFinalGradeRepository.save(teamFinalGrade);
        }

        return new FinalGradeResponse("success", new FinalGradeResponse.Data(
                group.getId(),
                group.getGroupName(),
                teamFinalGrade.getTeamGrade(),
                teamFinalGrade.isPublished(),
                teamFinalGrade.getComputedAt(),
                teamFinalGrade.getPublishedAt(),
                breakdowns,
                studentUpdate.grades()));
    }

    @Transactional
    public void recalculateGroupIfReady(Long groupId) {
        try {
            recalculateGroup(groupId);
        } catch (RuntimeException ex) {
            logger.debug("Final grade not recalculated for group {} yet: {}", groupId, ex.getMessage());
        }
    }

    @Transactional
    public FinalGradeResponse publishGroup(Long groupId) {
        TeamFinalGrade team = teamFinalGradeRepository.findByGroup_Id(groupId)
                .orElseThrow(() -> new NotFoundException("Final grade has not been computed for group: " + groupId));
        Instant now = Instant.now();
        team.setPublished(true);
        team.setPublishedAt(now);
        teamFinalGradeRepository.save(team);

        List<StudentFinalGrade> students = studentFinalGradeRepository.findByGroup_IdOrderByUser_FullNameAsc(groupId);
        students.forEach(s -> s.setPublished(true));
        studentFinalGradeRepository.saveAll(students);

        String groupName = team.getGroup().getGroupName();
        for (StudentFinalGrade s : students) {
            String grade = s.getFinalGrade() != null ? scale2(s.getFinalGrade()).toPlainString() : "-";
            String message = "Your final grade for project \"" + groupName + "\" has been published: " + grade + " / 100";
            try {
                notificationService.createSystemAlert(s.getUser().getUserId(), message, "FINAL_GRADE_PUBLISHED", null);
            } catch (Exception ex) {
                logger.warn("Could not send final grade notification to user {}: {}", s.getUser().getUserId(), ex.getMessage());
            }
        }

        return getGroupFinalGrades(groupId, true);
    }

    @Transactional(readOnly = true)
    public List<FinalGradeResponse.GroupGradeSummary> listAllGroupGrades() {
        // Single COUNT query — no N+1 from lazy group access
        Map<Long, Integer> studentCounts = studentFinalGradeRepository.countStudentsPerGroup()
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Long) row[1]).intValue()));

        return teamFinalGradeRepository.findAllWithGroup().stream()
                .map(t -> new FinalGradeResponse.GroupGradeSummary(
                        t.getGroup().getId(),
                        t.getGroup().getGroupName(),
                        t.getTeamGrade(),
                        t.isPublished(),
                        t.getPublishedAt(),
                        studentCounts.getOrDefault(t.getGroup().getId(), 0)))
                .toList();
    }

    @Transactional(readOnly = true)
    public FinalGradeResponse getGroupFinalGrades(Long groupId, boolean includeUnpublished) {
        TeamFinalGrade team = teamFinalGradeRepository.findByGroup_Id(groupId)
                .orElseThrow(() -> new NotFoundException("Final grade has not been computed for group: " + groupId));
        if (!team.isPublished() && !includeUnpublished) {
            throw new BadRequestException("Final grade is not published yet.");
        }

        List<FinalGradeResponse.StudentGrade> students = studentFinalGradeRepository
                .findByGroup_IdOrderByUser_FullNameAsc(groupId)
                .stream()
                .filter(s -> includeUnpublished || s.isPublished())
                .map(this::toStudentGrade)
                .toList();

        Group group = team.getGroup();
        List<FinalGradeResponse.DeliverableBreakdown> deliverables = computeDeliverablesReadOnly(groupId);
        return new FinalGradeResponse("success", new FinalGradeResponse.Data(
                group.getId(),
                group.getGroupName(),
                team.getTeamGrade(),
                team.isPublished(),
                team.getComputedAt(),
                team.getPublishedAt(),
                deliverables,
                students));
    }

    private List<FinalGradeResponse.DeliverableBreakdown> computeDeliverablesReadOnly(Long groupId) {
        try {
            List<SprintDeliverableWeight> weights = sprintDeliverableWeightRepository.findAllByOrderByDeliverableTypeAscSprint_IdAsc();
            if (weights.isEmpty()) return List.of();

            // Pre-fetch all sprints and all advisor grades for this group in one shot
            List<Sprint> allSprints = sprintRepository.findAllByOrderByStartDateAscIdAsc();
            List<SprintAdvisorGrade> allAdvisorGrades = sprintAdvisorGradeRepository.findByGroup_Id(groupId);

            // Index advisor grades by sprintId for O(1) lookup
            Map<Long, List<SprintAdvisorGrade>> advisorBySprintId = allAdvisorGrades.stream()
                    .collect(Collectors.groupingBy(g -> g.getSprint().getId()));

            Map<DeliverableType, List<SprintDeliverableWeight>> byDeliverable = weights.stream()
                    .collect(Collectors.groupingBy(SprintDeliverableWeight::getDeliverableType,
                            () -> new EnumMap<>(DeliverableType.class), Collectors.toList()));

            List<FinalGradeResponse.DeliverableBreakdown> breakdowns = new ArrayList<>();
            for (Map.Entry<DeliverableType, List<SprintDeliverableWeight>> entry : byDeliverable.entrySet()) {
                DeliverableType type = entry.getKey();
                List<SprintDeliverableWeight> rows = entry.getValue();
                BigDecimal finalWeight = sumWeights(rows);
                BigDecimal rawGrade = resolveRawDeliverableGrade(groupId, type);
                BigDecimal scrumAvg = resolveWeightedSprintAverageFast(rows, allSprints, advisorBySprintId, SprintMetric.SCRUM);
                BigDecimal codeReviewAvg = resolveWeightedSprintAverageFast(rows, allSprints, advisorBySprintId, SprintMetric.CODE_REVIEW);
                BigDecimal scalar = scrumAvg.add(codeReviewAvg)
                        .divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP)
                        .divide(HUNDRED, 4, RoundingMode.HALF_UP);
                BigDecimal scaledGrade = rawGrade.multiply(scalar).setScale(2, RoundingMode.HALF_UP);
                BigDecimal contribution = scaledGrade.multiply(finalWeight).divide(HUNDRED, 2, RoundingMode.HALF_UP);
                breakdowns.add(new FinalGradeResponse.DeliverableBreakdown(
                        type.name(), scale2(rawGrade), scale2(scrumAvg), scale2(codeReviewAvg),
                        scalar.setScale(4, RoundingMode.HALF_UP), scaledGrade, scale2(finalWeight), contribution));
            }
            return breakdowns;
        } catch (Exception e) {
            logger.debug("Could not compute deliverable breakdown for group {}: {}", groupId, e.getMessage());
            return List.of();
        }
    }

    private BigDecimal resolveWeightedSprintAverageFast(
            List<SprintDeliverableWeight> rows,
            List<Sprint> allSprints,
            Map<Long, List<SprintAdvisorGrade>> advisorBySprintId,
            SprintMetric metric) {

        java.time.LocalDate latestDate = rows.stream()
                .map(w -> w.getSprint().getStartDate())
                .max(Comparator.naturalOrder())
                .orElseThrow(() -> new BadRequestException("Deliverable has no sprint associations."));

        List<Sprint> cumulativeSprints = allSprints.stream()
                .filter(s -> !s.getStartDate().isAfter(latestDate))
                .toList();

        if (cumulativeSprints.isEmpty()) {
            throw new BadRequestException("No sprints found up to the associated sprint date.");
        }

        BigDecimal total = BigDecimal.ZERO;
        for (Sprint sprint : cumulativeSprints) {
            List<SprintAdvisorGrade> grades = advisorBySprintId.getOrDefault(sprint.getId(), List.of());
            List<Integer> scores = grades.stream()
                    .map(g -> metric == SprintMetric.SCRUM ? g.getScrumGrade() : g.getCodeReviewGrade())
                    .map(this::softGradeToScore)
                    .toList();
            if (scores.isEmpty()) {
                throw new BadRequestException("Missing " + metric.label + " grade for group sprint " + sprint.getId());
            }
            total = total.add(BigDecimal.valueOf(scores.stream().mapToInt(Integer::intValue).average().orElse(0.0)));
        }
        return total.divide(BigDecimal.valueOf(cumulativeSprints.size()), 4, RoundingMode.HALF_UP);
    }

    private TeamFinalGrade upsertTeamGrade(Group group, BigDecimal teamGrade) {
        TeamFinalGrade entity = teamFinalGradeRepository.findByGroup_Id(group.getId()).orElseGet(TeamFinalGrade::new);
        BigDecimal previous = entity.getTeamGrade();
        boolean changed = previous == null || previous.subtract(teamGrade).abs().compareTo(BigDecimal.valueOf(0.005)) > 0;
        entity.setGroup(group);
        entity.setTeamGrade(teamGrade);
        entity.setComputedAt(Instant.now());
        if (changed) {
            entity.setPublished(false);
            entity.setPublishedAt(null);
        }
        return teamFinalGradeRepository.save(entity);
    }

    private StudentGradeUpdate upsertStudentGrades(
            Group group,
            List<FinalGradeResponse.DeliverableBreakdown> breakdowns,
            Map<DeliverableType, List<SprintDeliverableWeight>> weightsByDeliverable,
            boolean forceUnpublished) {

        Map<String, FinalGradeResponse.DeliverableBreakdown> breakdownByType = breakdowns.stream()
                .collect(Collectors.toMap(FinalGradeResponse.DeliverableBreakdown::deliverableType, Function.identity()));

        List<User> students = resolveGroupStudents(group);
        List<StudentFinalGrade> saved = new ArrayList<>();
        boolean changed = false;
        for (User student : students) {
            BigDecimal grade = BigDecimal.ZERO;
            BigDecimal weightedRatioSum = BigDecimal.ZERO;
            BigDecimal weightSum = BigDecimal.ZERO;

            for (Map.Entry<DeliverableType, List<SprintDeliverableWeight>> entry : weightsByDeliverable.entrySet()) {
                FinalGradeResponse.DeliverableBreakdown breakdown = breakdownByType.get(entry.getKey().name());
                if (breakdown == null) continue;
                BigDecimal ratio = resolveStudentRatio(group.getId(), student, entry.getValue());
                BigDecimal contribution = breakdown.contribution().multiply(ratio).setScale(2, RoundingMode.HALF_UP);
                grade = grade.add(contribution);
                weightedRatioSum = weightedRatioSum.add(ratio.multiply(breakdown.finalWeight()));
                weightSum = weightSum.add(breakdown.finalWeight());
            }

            BigDecimal overallRatio = weightSum.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : weightedRatioSum.divide(weightSum, 4, RoundingMode.HALF_UP);

            StudentFinalGrade entity = studentFinalGradeRepository
                    .findByGroup_IdAndUser_UserId(group.getId(), student.getUserId())
                    .orElseGet(StudentFinalGrade::new);
            BigDecimal nextGrade = scale2(grade);
            boolean studentChanged = valueChanged(entity.getFinalGrade(), nextGrade, "0.005")
                    || valueChanged(entity.getSpRatio(), overallRatio, "0.00005");
            entity.setGroup(group);
            entity.setUser(student);
            entity.setFinalGrade(nextGrade);
            entity.setSpRatio(overallRatio);
            entity.setComputedAt(Instant.now());
            if (forceUnpublished || studentChanged) {
                entity.setPublished(false);
            }
            changed = changed || studentChanged;
            saved.add(studentFinalGradeRepository.save(entity));
        }
        return new StudentGradeUpdate(saved.stream().map(this::toStudentGrade).toList(), changed);
    }

    private BigDecimal resolveRawDeliverableGrade(Long groupId, DeliverableType type) {
        Optional<Submission> submission;
        if (type == DeliverableType.PROPOSAL) {
            submission = submissionRepository.findTopByGroupIdAndDeliverableTypeAndStatusOrderByCreatedAtDesc(
                    groupId, DeliverableType.REVISED_PROPOSAL, SubmissionStatus.GRADED);
            if (submission.isEmpty()) {
                submission = submissionRepository.findTopByGroupIdAndDeliverableTypeAndStatusOrderByCreatedAtDesc(
                        groupId, DeliverableType.PROPOSAL, SubmissionStatus.GRADED);
            }
        } else {
            submission = submissionRepository.findTopByGroupIdAndDeliverableTypeAndStatusOrderByCreatedAtDesc(
                    groupId, type, SubmissionStatus.GRADED);
        }

        Submission graded = submission.orElseThrow(() ->
                new BadRequestException("Missing graded deliverable for " + type + " in group " + groupId));
        if (graded.getFinalGrade() == null) {
            throw new BadRequestException("Graded deliverable has no finalGrade: " + graded.getId());
        }
        return BigDecimal.valueOf(graded.getFinalGrade());
    }

    private BigDecimal resolveWeightedSprintAverage(Long groupId, List<SprintDeliverableWeight> rows, SprintMetric metric) {
        // Find the latest sprint date among this deliverable's associations
        java.time.LocalDate latestDate = rows.stream()
                .map(w -> w.getSprint().getStartDate())
                .max(Comparator.naturalOrder())
                .orElseThrow(() -> new BadRequestException("Deliverable has no sprint associations."));

        // Cumulative: include all sprints whose start date is <= the deliverable's latest sprint
        List<Sprint> cumulativeSprints = sprintRepository.findAllByOrderByStartDateAscIdAsc().stream()
                .filter(s -> !s.getStartDate().isAfter(latestDate))
                .toList();

        if (cumulativeSprints.isEmpty()) {
            throw new BadRequestException("No sprints found up to the associated sprint date.");
        }

        BigDecimal total = BigDecimal.ZERO;
        for (Sprint sprint : cumulativeSprints) {
            total = total.add(resolveSprintMetricScore(groupId, sprint.getId(), metric));
        }
        return total.divide(BigDecimal.valueOf(cumulativeSprints.size()), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveSprintMetricScore(Long groupId, Long sprintId, SprintMetric metric) {
        List<SprintAdvisorGrade> grades = sprintAdvisorGradeRepository.findByGroup_IdAndSprint_Id(groupId, sprintId);
        if (metric == SprintMetric.CODE_REVIEW && grades.isEmpty()) {
            BigDecimal aiScore = resolveAiCodeReviewScore(groupId, sprintId);
            if (aiScore != null) return aiScore;
        }

        List<Integer> scores = grades.stream()
                .map(g -> metric == SprintMetric.SCRUM ? g.getScrumGrade() : g.getCodeReviewGrade())
                .map(this::softGradeToScore)
                .toList();

        if (scores.isEmpty()) {
            if (metric == SprintMetric.CODE_REVIEW) {
                BigDecimal aiScore = resolveAiCodeReviewScore(groupId, sprintId);
                if (aiScore != null) return aiScore;
            }
            throw new BadRequestException("Missing " + metric.label + " grade for group " + groupId
                    + " sprint " + sprintId);
        }
        return BigDecimal.valueOf(scores.stream().mapToInt(Integer::intValue).average().orElse(0.0));
    }

    private BigDecimal resolveAiCodeReviewScore(Long groupId, Long sprintId) {
        List<IssueValidationResult> raw = issueValidationResultRepository
                .findBySprintIdAndTeamIdAndValidationStatus(sprintId, groupId, VALIDATED);
        if (raw.isEmpty()) return null;

        Map<String, IssueValidationResult> latestByIssue = raw.stream()
                .filter(r -> r.getCompositeScore() != null)
                .collect(Collectors.toMap(
                        IssueValidationResult::getIssueKey,
                        Function.identity(),
                        (a, b) -> a.getEvaluatedAt().isAfter(b.getEvaluatedAt()) ? a : b));
        if (latestByIssue.isEmpty()) return null;

        Map<String, Integer> storyPoints = sprintIssueTrackingRepository
                .findByGroup_IdAndSprint_Id(groupId, sprintId)
                .stream()
                .collect(Collectors.toMap(
                        SprintIssueTracking::getIssueKey,
                        s -> s.getStoryPoints() != null && s.getStoryPoints() > 0 ? s.getStoryPoints() : 1,
                        Math::max));

        BigDecimal weighted = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (IssueValidationResult result : latestByIssue.values()) {
            BigDecimal sp = BigDecimal.valueOf(storyPoints.getOrDefault(result.getIssueKey(), 1));
            weighted = weighted.add(result.getCompositeScore().multiply(sp));
            total = total.add(sp);
        }
        return total.compareTo(BigDecimal.ZERO) == 0
                ? null
                : weighted.divide(total, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveStudentRatio(Long groupId, User student, List<SprintDeliverableWeight> rows) {
        String githubUsername = student.getGithubUsername();
        if (githubUsername == null || githubUsername.isBlank()) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }

        // Global ratio: completed SP / required SP across ALL sprints (not per-deliverable)
        List<Sprint> allSprints = sprintRepository.findAllByOrderByStartDateAscIdAsc();
        Set<Long> allSprintIds = allSprints.stream().map(Sprint::getId).collect(Collectors.toCollection(LinkedHashSet::new));

        List<SprintIssueTracking> logs = sprintIssueTrackingRepository.findByGroup_IdAndSprint_IdIn(groupId, allSprintIds);
        List<SprintIssueTracking> ownLogs = logs.stream()
                .filter(log -> log.getAssigneeGithubUsername() != null)
                .filter(log -> log.getAssigneeGithubUsername().equalsIgnoreCase(githubUsername))
                .toList();

        int completed = ownLogs.stream()
                .filter(log -> Boolean.TRUE.equals(log.getPrMerged()))
                .mapToInt(log -> log.getStoryPoints() != null ? log.getStoryPoints() : 0)
                .sum();

        int target = allSprints.stream()
                .mapToInt(s -> s.getRequiredStoryPoints() != null ? s.getRequiredStoryPoints() : 0)
                .sum();

        if (target <= 0) {
            target = ownLogs.stream()
                    .mapToInt(log -> log.getStoryPoints() != null ? log.getStoryPoints() : 0)
                    .sum();
        }
        if (target <= 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        BigDecimal ratio = BigDecimal.valueOf(completed).divide(BigDecimal.valueOf(target), 4, RoundingMode.HALF_UP);
        if (ratio.compareTo(BigDecimal.ONE) > 0) ratio = BigDecimal.ONE;
        return ratio.setScale(4, RoundingMode.HALF_UP);
    }

    private List<User> resolveGroupStudents(Group group) {
        Map<Long, User> users = new LinkedHashMap<>();
        if (group.getLeader() != null) {
            users.put(group.getLeader().getUserId(), group.getLeader());
        }
        groupMemberRepository.findByGroup_Id(group.getId()).forEach(member -> {
            if (member.getUser() != null) {
                users.put(member.getUser().getUserId(), member.getUser());
            }
        });
        return new ArrayList<>(users.values());
    }

    private FinalGradeResponse.StudentGrade toStudentGrade(StudentFinalGrade grade) {
        User user = grade.getUser();
        return new FinalGradeResponse.StudentGrade(
                user.getUserId(),
                user.getFullName(),
                user.getGithubUsername(),
                grade.getFinalGrade(),
                grade.getSpRatio(),
                grade.isPublished());
    }

    private SprintAdvisorGradeResponse toSprintAdvisorGradeResponse(SprintAdvisorGrade grade) {
        return new SprintAdvisorGradeResponse(
                grade.getId(),
                grade.getGroup().getId(),
                grade.getSprint().getId(),
                grade.getAdvisor().getUserId(),
                grade.getScrumGrade(),
                grade.getCodeReviewGrade(),
                grade.getUpdatedAt());
    }

    private SprintConfigResponse toSprintConfigResponse(Sprint sprint) {
        return new SprintConfigResponse(
                sprint.getId(),
                sprint.getSprintName(),
                sprint.getStartDate(),
                sprint.getEndDate(),
                sprint.getStatus(),
                sprint.getRequiredStoryPoints(),
                sprint.getCreatedAt(),
                sprint.getUpdatedAt());
    }

    private void validateSprintRequest(SprintConfigRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new BadRequestException("Sprint endDate must be on or after startDate.");
        }
    }

    private void applySprintRequest(Sprint sprint, SprintConfigRequest request) {
        sprint.setSprintName(request.sprintName().trim());
        sprint.setStartDate(request.startDate());
        sprint.setEndDate(request.endDate());
        sprint.setStatus(normalizeSprintStatus(request.status()));
        sprint.setRequiredStoryPoints(request.requiredStoryPoints());
    }

    private String normalizeSprintStatus(String status) {
        String normalized = status == null ? "" : status.trim();
        if (normalized.isEmpty()) {
            throw new BadRequestException("Sprint status is required.");
        }
        if ("active".equalsIgnoreCase(normalized)) return "Active";
        if ("completed".equalsIgnoreCase(normalized)) return "Completed";
        if ("planned".equalsIgnoreCase(normalized)) return "Planned";
        return normalized;
    }

    private DeliverableType parseDeliverableType(String value) {
        try {
            return DeliverableType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new BadRequestException("Invalid deliverableType: " + value);
        }
    }

    private String normalizeSoftGrade(String grade) {
        String normalized = grade == null ? "" : grade.trim().toUpperCase(Locale.ROOT);
        softGradeToScore(normalized);
        return normalized;
    }

    private int softGradeToScore(String grade) {
        return switch (grade) {
            case "A" -> 100;
            case "B" -> 80;
            case "C" -> 60;
            case "D" -> 50;
            case "F" -> 0;
            default -> throw new BadRequestException("Invalid soft grade: " + grade + ". Must be A, B, C, D, or F.");
        };
    }

    private String scoreToSoftGrade(BigDecimal score) {
        if (score.compareTo(BigDecimal.valueOf(90)) >= 0) return "A";
        if (score.compareTo(BigDecimal.valueOf(75)) >= 0) return "B";
        if (score.compareTo(BigDecimal.valueOf(60)) >= 0) return "C";
        if (score.compareTo(BigDecimal.valueOf(50)) >= 0) return "D";
        return "F";
    }

    private void requireTotalWeight(BigDecimal total, String label) {
        BigDecimal delta = total.subtract(HUNDRED).abs();
        if (delta.compareTo(BigDecimal.valueOf(0.01)) > 0) {
            throw new BadRequestException(label + " must sum to 100. Current total: " + total);
        }
    }

    private BigDecimal sumWeights(List<SprintDeliverableWeight> rows) {
        return rows.stream().map(SprintDeliverableWeight::getWeight).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal scale2(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean valueChanged(BigDecimal previous, BigDecimal current, String tolerance) {
        return previous == null || previous.subtract(current).abs().compareTo(new BigDecimal(tolerance)) > 0;
    }

    private record StudentGradeUpdate(List<FinalGradeResponse.StudentGrade> grades, boolean changed) {}

    private enum SprintMetric {
        SCRUM("Scrum"),
        CODE_REVIEW("Code Review");

        private final String label;

        SprintMetric(String label) {
            this.label = label;
        }
    }

    @Transactional
    public void saveDemonstrationGrade(Long groupId, Double grade) {
        if (grade < 0 || grade > 100) {
            throw new BadRequestException("Demonstration grade must be between 0 and 100.");
        }
        groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found: " + groupId));

        Optional<Submission> existing = submissionRepository
                .findTopByGroupIdAndDeliverableTypeOrderByCreatedAtDesc(groupId, DeliverableType.DEMONSTRATION);

        Submission s = existing.orElseGet(Submission::new);
        if (s.getId() == null) {
            s.setGroupId(groupId);
            s.setDeliverableType(DeliverableType.DEMONSTRATION);
            s.setContent("");
            s.setVersion(1);
        }
        s.setFinalGrade(grade);
        s.setStatus(SubmissionStatus.GRADED);
        submissionRepository.save(s);
        recalculateGroupIfReady(groupId);
    }

    @Transactional
    public Long initializeDemonstration(Long groupId) {
        groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found: " + groupId));

        Optional<Submission> existing = submissionRepository
                .findTopByGroupIdAndDeliverableTypeOrderByCreatedAtDesc(groupId, DeliverableType.DEMONSTRATION);

        if (existing.isPresent()) {
            return existing.get().getId();
        }

        Submission s = new Submission();
        s.setGroupId(groupId);
        s.setDeliverableType(DeliverableType.DEMONSTRATION);
        s.setContent("");
        s.setVersion(1);
        s.setStatus(SubmissionStatus.PENDING_REVIEW);
        s.setCreatedAt(java.time.LocalDateTime.now());
        s = submissionRepository.save(s);
        return s.getId();
    }

    @Transactional(readOnly = true)
    public Double getDemonstrationGrade(Long groupId) {
        return submissionRepository
                .findTopByGroupIdAndDeliverableTypeOrderByCreatedAtDesc(groupId, DeliverableType.DEMONSTRATION)
                .map(Submission::getFinalGrade)
                .orElse(null);
    }
}
