package com.spms.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spms.backend.dto.response.IssueValidationDetailResponse;
import com.spms.backend.dto.response.IssueValidationDetailResponse.*;
import com.spms.backend.dto.response.SprintValidationResultsResponse;
import com.spms.backend.dto.response.SprintValidationResultsResponse.*;
import com.spms.backend.exception.P7ApiException;
import com.spms.backend.model.Group;
import com.spms.backend.model.IssueValidationResult;
import com.spms.backend.model.ValidationConfig;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.IssueValidationResultRepository;
import com.spms.backend.repository.SprintRepository;
import com.spms.backend.repository.ValidationConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Read service for AI validation results — powers the Advisor Dashboard.
 *
 * <p>DFD: 7.6 Store Validation Results (read side) — Issue #296.</p>
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Sprint-level aggregation with per-team grouping</li>
 *   <li>Per-issue detail drill-down</li>
 *   <li>RBAC: Advisor sees only advisee teams; Coordinator sees all</li>
 *   <li>Composite score recalculation at query time from validation_config weights</li>
 * </ul></p>
 */
@Service
public class ValidationResultReadService {

    private static final Logger logger = LoggerFactory.getLogger(ValidationResultReadService.class);

    private static final String STATUS_VALIDATED = "VALIDATED";

    private final IssueValidationResultRepository resultRepository;
    private final ValidationConfigRepository configRepository;
    private final SprintRepository sprintRepository;
    private final GroupRepository groupRepository;
    private final ObjectMapper objectMapper;

    public ValidationResultReadService(
            IssueValidationResultRepository resultRepository,
            ValidationConfigRepository configRepository,
            SprintRepository sprintRepository,
            GroupRepository groupRepository,
            ObjectMapper objectMapper) {
        this.resultRepository = resultRepository;
        this.configRepository = configRepository;
        this.sprintRepository = sprintRepository;
        this.groupRepository = groupRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * GET /ai-validation/sprints/{sprintId}/results
     *
     * <p>Returns aggregated validation results grouped by team, with RBAC filtering.</p>
     */
    @Transactional(readOnly = true)
    public SprintValidationResultsResponse getSprintResults(Long sprintId, Long teamId,
                                                             String role, Long userId) {
        enforceP7Access(role);

        sprintRepository.findById(sprintId)
                .orElseThrow(() -> new P7ApiException(HttpStatus.NOT_FOUND,
                        "SPRINT_NOT_FOUND", "Sprint not found."));

        Set<Long> allowedTeamIds = resolveAllowedTeamIds(role, userId, teamId);

        List<IssueValidationResult> results;
        if (teamId != null) {
            results = resultRepository.findBySprintIdAndTeamId(sprintId, teamId);
        } else {
            results = resultRepository.findBySprintId(sprintId);
        }

        if (results.isEmpty()) {
            throw new P7ApiException(HttpStatus.NOT_FOUND,
                    "SPRINT_HAS_NO_RESULTS", "No validation results exist for this sprint.");
        }

        // Filter by RBAC-allowed teams (for advisors)
        if (allowedTeamIds != null) {
            results = results.stream()
                    .filter(r -> r.getTeamId() != null && allowedTeamIds.contains(r.getTeamId()))
                    .collect(Collectors.toList());

            if (results.isEmpty()) {
                throw new P7ApiException(HttpStatus.NOT_FOUND,
                        "SPRINT_HAS_NO_RESULTS", "No validation results exist for your teams in this sprint.");
            }
        }

        ValidationConfig config = loadConfig();

        Instant latestEvaluatedAt = results.stream()
                .map(IssueValidationResult::getEvaluatedAt)
                .filter(Objects::nonNull)
                .max(Instant::compareTo)
                .orElse(null);

        // Group by teamId
        Map<Long, List<IssueValidationResult>> byTeam = results.stream()
                .filter(r -> r.getTeamId() != null)
                .collect(Collectors.groupingBy(IssueValidationResult::getTeamId));

        // Build team name lookup
        Set<Long> teamIds = byTeam.keySet();
        Map<Long, String> teamNames = groupRepository.findAllById(teamIds).stream()
                .collect(Collectors.toMap(Group::getId, Group::getGroupName));

        List<TeamResult> teams = new ArrayList<>();
        for (Map.Entry<Long, List<IssueValidationResult>> entry : byTeam.entrySet()) {
            Long tid = entry.getKey();
            List<IssueValidationResult> teamResults = entry.getValue();

            List<IssueSummary> issueSummaries = teamResults.stream()
                    .map(r -> toIssueSummary(r, config))
                    .collect(Collectors.toList());

            double overallScore = teamResults.stream()
                    .filter(r -> STATUS_VALIDATED.equals(r.getValidationStatus()))
                    .mapToDouble(r -> computeComposite(r, config))
                    .average()
                    .orElse(0.0);

            // Round to 1 decimal for clean display
            overallScore = BigDecimal.valueOf(overallScore)
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue();

            teams.add(new TeamResult(
                    tid,
                    teamNames.getOrDefault(tid, "Unknown Team"),
                    overallScore,
                    teamResults.size(),
                    issueSummaries
            ));
        }

        logger.info("Sprint results retrieved. sprintId={}, teamCount={}, totalIssues={}",
                sprintId, teams.size(), results.size());

        return new SprintValidationResultsResponse(
                "success",
                new SprintResultsData(sprintId, latestEvaluatedAt, teams)
        );
    }

    /**
     * GET /ai-validation/issues/{issueKey}/details
     *
     * <p>Returns the full AI analysis detail for a specific issue, with RBAC.</p>
     */
    @Transactional(readOnly = true)
    public IssueValidationDetailResponse getIssueDetails(String issueKey, String role, Long userId) {
        enforceP7Access(role);

        IssueValidationResult result = resultRepository.findTopByIssueKeyOrderByEvaluatedAtDesc(issueKey)
                .orElseThrow(() -> new P7ApiException(HttpStatus.NOT_FOUND,
                        "ISSUE_NOT_VALIDATED", "Issue not yet validated."));

        // RBAC check on the issue's team
        if ("advisor".equalsIgnoreCase(role)) {
            Set<Long> advisorTeamIds = getAdvisorTeamIds(userId);
            if (result.getTeamId() == null || !advisorTeamIds.contains(result.getTeamId())) {
                throw new P7ApiException(HttpStatus.FORBIDDEN,
                        "FORBIDDEN_TEAM_ACCESS", "Advisor is not authorized for this team.");
            }
        }

        logger.info("Issue details retrieved. issueKey={}, resultId={}", issueKey, result.getId());

        return new IssueValidationDetailResponse(
                "success",
                toIssueDetailData(result)
        );
    }

    // ── RBAC helpers ─────────────────────────────────────────────────────

    private void enforceP7Access(String role) {
        if (role == null || "student".equalsIgnoreCase(role)) {
            throw new P7ApiException(HttpStatus.FORBIDDEN,
                    "FORBIDDEN", "Caller role is not allowed.");
        }
        if (!"coordinator".equalsIgnoreCase(role) && !"advisor".equalsIgnoreCase(role)) {
            throw new P7ApiException(HttpStatus.FORBIDDEN,
                    "FORBIDDEN", "Caller role is not allowed.");
        }
    }

    /**
     * Resolves the set of team IDs the caller is allowed to see.
     *
     * @return null if coordinator (no filter), or a Set of allowed IDs for advisors
     */
    private Set<Long> resolveAllowedTeamIds(String role, Long userId, Long requestedTeamId) {
        if ("coordinator".equalsIgnoreCase(role)) {
            return null; // coordinator sees all
        }

        // Advisor
        Set<Long> advisorTeamIds = getAdvisorTeamIds(userId);

        if (requestedTeamId != null && !advisorTeamIds.contains(requestedTeamId)) {
            throw new P7ApiException(HttpStatus.FORBIDDEN,
                    "FORBIDDEN_TEAM_ACCESS", "Advisor is not authorized for this team.");
        }

        return advisorTeamIds;
    }

    private Set<Long> getAdvisorTeamIds(Long userId) {
        return groupRepository.findByAdvisorId(userId).stream()
                .map(Group::getId)
                .collect(Collectors.toSet());
    }

    // ── Composite score ─────────────────────────────────────────────────

    private double computeComposite(IssueValidationResult r, ValidationConfig config) {
        BigDecimal rScore = r.getReviewScore() != null ? r.getReviewScore() : BigDecimal.ZERO;
        BigDecimal iScore = r.getImplScore() != null ? r.getImplScore() : BigDecimal.ZERO;
        return rScore.multiply(BigDecimal.valueOf(config.getReviewWeight()))
                .add(iScore.multiply(BigDecimal.valueOf(config.getImplementationWeight())))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    // ── DTO mapping ─────────────────────────────────────────────────────

    private IssueSummary toIssueSummary(IssueValidationResult r, ValidationConfig config) {
        Double compositeScore = null;
        Double reviewScore = null;
        Double implScore = null;

        if (STATUS_VALIDATED.equals(r.getValidationStatus())) {
            compositeScore = computeComposite(r, config);
            reviewScore = r.getReviewScore() != null ? r.getReviewScore().doubleValue() : null;
            implScore = r.getImplScore() != null ? r.getImplScore().doubleValue() : null;
        }

        return new IssueSummary(
                r.getIssueKey(),
                r.getIssueTitle(),
                r.getAssignee(),
                r.getPrNumber() != null ? r.getPrNumber().intValue() : null,
                r.getPrMerged(),
                reviewScore,
                r.getReviewQuality(),
                implScore,
                compositeScore,
                r.getValidationStatus()
        );
    }

    private IssueDetailData toIssueDetailData(IssueValidationResult r) {
        return new IssueDetailData(
                r.getIssueKey(),
                r.getIssueDescription(),
                r.getPrNumber() != null ? r.getPrNumber().intValue() : null,
                r.getPrUrl(),
                r.getEvaluatedAt(),
                toReviewVerification(r),
                toImplementationValidation(r)
        );
    }

    private ReviewVerification toReviewVerification(IssueValidationResult r) {
        return new ReviewVerification(
                r.getReviewScore() != null ? r.getReviewScore().doubleValue() : null,
                r.getHasReview(),
                r.getReviewQuality(),
                r.getReviewerCount(),
                r.getHasChangeRequests(),
                r.getHasSubstantiveComments(),
                r.getReviewAiFeedback()
        );
    }

    private ImplementationValidation toImplementationValidation(IssueValidationResult r) {
        return new ImplementationValidation(
                r.getImplScore() != null ? r.getImplScore().doubleValue() : null,
                r.getImplIsValid(),
                parseCoverageAreas(r.getImplCoverageAreas()),
                parseMissingRequirements(r.getImplMissingRequirements()),
                r.getImplAiFeedback(),
                r.getFilesAnalyzed(),
                r.getDiffTruncated()
        );
    }

    private List<CoverageArea> parseCoverageAreas(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            List<Map<String, Object>> raw = objectMapper.readValue(json,
                    new TypeReference<List<Map<String, Object>>>() {});
            return raw.stream()
                    .map(m -> new CoverageArea(
                            (String) m.get("requirement"),
                            m.get("covered") instanceof Boolean b ? b : null
                    ))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.warn("Failed to parse coverageAreas JSON (length={})", json.length());
            return Collections.emptyList();
        }
    }

    private List<String> parseMissingRequirements(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            logger.warn("Failed to parse missingRequirements JSON (length={})", json.length());
            return Collections.emptyList();
        }
    }

    private ValidationConfig loadConfig() {
        return configRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException(
                        "validation_config singleton row (id=1) is missing — check V12 migration."));
    }
}
