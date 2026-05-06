package com.spms.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spms.backend.dto.response.IssueValidationDetailResponse;
import com.spms.backend.dto.response.SprintValidationResultsResponse;
import com.spms.backend.exception.P7ApiException;
import com.spms.backend.model.Group;
import com.spms.backend.model.IssueValidationResult;
import com.spms.backend.model.User;
import com.spms.backend.model.ValidationConfig;
import com.spms.backend.model.Sprint;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.IssueValidationResultRepository;
import com.spms.backend.repository.SprintRepository;
import com.spms.backend.repository.ValidationConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ValidationResultReadService} — Issue #296.
 *
 * <p>Covers all acceptance criteria: RBAC filtering, composite score
 * recalculation at query time, 404 for missing data, and response shape.</p>
 */
@ExtendWith(MockitoExtension.class)
class ValidationResultReadServiceTest {

    @Mock
    private IssueValidationResultRepository resultRepository;

    @Mock
    private ValidationConfigRepository configRepository;

    @Mock
    private SprintRepository sprintRepository;

    @Mock
    private GroupRepository groupRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ValidationResultReadService service;

    private ValidationConfig config;
    private Sprint sprint;

    @BeforeEach
    void setUp() {
        config = new ValidationConfig();
        config.setId(1L);
        config.setReviewWeight(40);
        config.setImplementationWeight(60);
        config.setOpenaiModel("gpt-4o");
        config.setMaxDiffLines(500);
        config.setExcludedFilePatterns(Collections.emptyList());

        sprint = new Sprint();
        sprint.setId(1L);
        sprint.setSprintName("Sprint 1");
    }

    // ── Test data factories ─────────────────────────────────────────────

    private Group createTeam(Long id, String name, Long advisorUserId) {
        Group g = new Group();
        g.setId(id);
        g.setGroupName(name);
        if (advisorUserId != null) {
            User advisor = new User();
            advisor.setUserId(advisorUserId);
            g.setAdvisor(advisor);
        }
        return g;
    }

    private IssueValidationResult createResult(Long sprintId, Long teamId, String issueKey,
                                                BigDecimal reviewScore, BigDecimal implScore,
                                                String status) {
        IssueValidationResult r = new IssueValidationResult();
        r.setId(1L);
        r.setSprintId(sprintId);
        r.setTeamId(teamId);
        r.setIssueKey(issueKey);
        r.setIssueTitle("Test issue: " + issueKey);
        r.setAssignee("student@example.com");
        r.setPrNumber(42L);
        r.setPrMerged(true);
        r.setReviewScore(reviewScore);
        r.setReviewQuality("THOROUGH");
        r.setReviewerCount(2);
        r.setHasChangeRequests(true);
        r.setHasSubstantiveComments(true);
        r.setReviewAiFeedback("Good review process.");
        r.setImplScore(implScore);
        r.setImplIsValid(true);
        r.setImplAiFeedback("Implementation matches requirements.");
        r.setFilesAnalyzed(4);
        r.setDiffTruncated(false);
        r.setValidationStatus(status);
        r.setEvaluatedAt(Instant.now());
        return r;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Sprint Results
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /sprints/{sprintId}/results")
    class SprintResults {

        @Test
        @DisplayName("AC7: Composite score arithmetic — review=90, impl=80, weights 40/60 → 84.0")
        void compositeScoreArithmetic() {
            when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));
            when(configRepository.findById(1L)).thenReturn(Optional.of(config));

            IssueValidationResult result = createResult(1L, 10L, "PROJ-123",
                    new BigDecimal("90.00"), new BigDecimal("80.00"), "VALIDATED");

            when(resultRepository.findBySprintId(1L)).thenReturn(List.of(result));
            when(groupRepository.findAllById(any())).thenReturn(
                    List.of(createTeam(10L, "Alpha Team", null)));

            SprintValidationResultsResponse response = service.getSprintResults(
                    1L, null, "coordinator", 100L);

            assertThat(response.status()).isEqualTo("success");
            assertThat(response.data().teams()).hasSize(1);

            var team = response.data().teams().get(0);
            assertThat(team.overallSprintScore()).isEqualTo(84.0);

            var issue = team.issues().get(0);
            assertThat(issue.compositeScore()).isEqualTo(84.0);
            assertThat(issue.reviewVerificationScore()).isEqualTo(90.0);
            assertThat(issue.implementationMatchScore()).isEqualTo(80.0);
        }

        @Test
        @DisplayName("AC1: Advisor querying own team → 200 with that team's results only")
        void advisorOwnTeam() {
            Long advisorUserId = 50L;
            Long ownTeamId = 10L;
            Long otherTeamId = 20L;

            when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));
            when(configRepository.findById(1L)).thenReturn(Optional.of(config));
            when(groupRepository.findByAdvisorId(advisorUserId)).thenReturn(
                    List.of(createTeam(ownTeamId, "My Team", advisorUserId)));

            IssueValidationResult ownResult = createResult(1L, ownTeamId, "PROJ-1",
                    new BigDecimal("85.00"), new BigDecimal("75.00"), "VALIDATED");
            IssueValidationResult otherResult = createResult(1L, otherTeamId, "PROJ-2",
                    new BigDecimal("70.00"), new BigDecimal("60.00"), "VALIDATED");

            when(resultRepository.findBySprintId(1L)).thenReturn(List.of(ownResult, otherResult));
            when(groupRepository.findAllById(any())).thenReturn(
                    List.of(createTeam(ownTeamId, "My Team", advisorUserId)));

            SprintValidationResultsResponse response = service.getSprintResults(
                    1L, null, "advisor", advisorUserId);

            assertThat(response.data().teams()).hasSize(1);
            assertThat(response.data().teams().get(0).teamId()).isEqualTo(ownTeamId);
        }

        @Test
        @DisplayName("AC2: Advisor querying a non-advisee teamId → 403 FORBIDDEN_TEAM_ACCESS")
        void advisorNonAdviseeTeam() {
            Long advisorUserId = 50L;
            Long ownTeamId = 10L;
            Long foreignTeamId = 99L;

            when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));
            when(groupRepository.findByAdvisorId(advisorUserId)).thenReturn(
                    List.of(createTeam(ownTeamId, "My Team", advisorUserId)));

            assertThatThrownBy(() -> service.getSprintResults(1L, foreignTeamId, "advisor", advisorUserId))
                    .isInstanceOf(P7ApiException.class)
                    .satisfies(ex -> {
                        P7ApiException p7 = (P7ApiException) ex;
                        assertThat(p7.getErrorCode()).isEqualTo("FORBIDDEN_TEAM_ACCESS");
                        assertThat(p7.getStatus().value()).isEqualTo(403);
                    });
        }

        @Test
        @DisplayName("AC4: Coordinator → 200 with all teams; teamId filter narrows correctly")
        void coordinatorAllTeams() {
            when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));
            when(configRepository.findById(1L)).thenReturn(Optional.of(config));

            IssueValidationResult r1 = createResult(1L, 10L, "PROJ-1",
                    new BigDecimal("90.00"), new BigDecimal("80.00"), "VALIDATED");
            IssueValidationResult r2 = createResult(1L, 20L, "PROJ-2",
                    new BigDecimal("70.00"), new BigDecimal("60.00"), "VALIDATED");

            when(resultRepository.findBySprintId(1L)).thenReturn(List.of(r1, r2));
            when(groupRepository.findAllById(any())).thenReturn(List.of(
                    createTeam(10L, "Team A", null),
                    createTeam(20L, "Team B", null)));

            SprintValidationResultsResponse response = service.getSprintResults(
                    1L, null, "coordinator", 100L);

            assertThat(response.data().teams()).hasSize(2);
        }

        @Test
        @DisplayName("AC4: Coordinator teamId filter narrows to single team")
        void coordinatorTeamIdFilter() {
            when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));
            when(configRepository.findById(1L)).thenReturn(Optional.of(config));

            IssueValidationResult r1 = createResult(1L, 10L, "PROJ-1",
                    new BigDecimal("90.00"), new BigDecimal("80.00"), "VALIDATED");

            when(resultRepository.findBySprintIdAndTeamId(1L, 10L)).thenReturn(List.of(r1));
            when(groupRepository.findAllById(any())).thenReturn(
                    List.of(createTeam(10L, "Team A", null)));

            SprintValidationResultsResponse response = service.getSprintResults(
                    1L, 10L, "coordinator", 100L);

            assertThat(response.data().teams()).hasSize(1);
            assertThat(response.data().teams().get(0).teamId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("AC5: Sprint with no validation runs yet → 404 SPRINT_HAS_NO_RESULTS")
        void sprintNoResults() {
            when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));
            when(resultRepository.findBySprintId(1L)).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> service.getSprintResults(1L, null, "coordinator", 100L))
                    .isInstanceOf(P7ApiException.class)
                    .satisfies(ex -> {
                        P7ApiException p7 = (P7ApiException) ex;
                        assertThat(p7.getErrorCode()).isEqualTo("SPRINT_HAS_NO_RESULTS");
                        assertThat(p7.getStatus().value()).isEqualTo(404);
                    });
        }

        @Test
        @DisplayName("Sprint not found → 404 SPRINT_NOT_FOUND")
        void sprintNotFound() {
            when(sprintRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getSprintResults(999L, null, "coordinator", 100L))
                    .isInstanceOf(P7ApiException.class)
                    .satisfies(ex -> {
                        P7ApiException p7 = (P7ApiException) ex;
                        assertThat(p7.getErrorCode()).isEqualTo("SPRINT_NOT_FOUND");
                        assertThat(p7.getStatus().value()).isEqualTo(404);
                    });
        }

        @Test
        @DisplayName("Student role → 403 FORBIDDEN")
        void studentForbidden() {
            assertThatThrownBy(() -> service.getSprintResults(1L, null, "student", 100L))
                    .isInstanceOf(P7ApiException.class)
                    .satisfies(ex -> {
                        P7ApiException p7 = (P7ApiException) ex;
                        assertThat(p7.getErrorCode()).isEqualTo("FORBIDDEN");
                        assertThat(p7.getStatus().value()).isEqualTo(403);
                    });
        }

        @Test
        @DisplayName("AC8: SKIPPED and FAILED issues present with correct status")
        void skippedAndFailedPresent() {
            when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));
            when(configRepository.findById(1L)).thenReturn(Optional.of(config));

            IssueValidationResult validated = createResult(1L, 10L, "PROJ-1",
                    new BigDecimal("90.00"), new BigDecimal("80.00"), "VALIDATED");
            IssueValidationResult skipped = createResult(1L, 10L, "PROJ-2",
                    null, null, "SKIPPED");
            IssueValidationResult failed = createResult(1L, 10L, "PROJ-3",
                    null, null, "FAILED");

            when(resultRepository.findBySprintId(1L)).thenReturn(List.of(validated, skipped, failed));
            when(groupRepository.findAllById(any())).thenReturn(
                    List.of(createTeam(10L, "Team A", null)));

            SprintValidationResultsResponse response = service.getSprintResults(
                    1L, null, "coordinator", 100L);

            var issues = response.data().teams().get(0).issues();
            assertThat(issues).hasSize(3);
            assertThat(issues).extracting("status")
                    .containsExactlyInAnyOrder("VALIDATED", "SKIPPED", "FAILED");

            // SKIPPED and FAILED should have null scores
            var skippedIssue = issues.stream()
                    .filter(i -> "SKIPPED".equals(i.status())).findFirst().orElseThrow();
            assertThat(skippedIssue.compositeScore()).isNull();
            assertThat(skippedIssue.reviewVerificationScore()).isNull();

            // VALIDATED should have scores
            var validatedIssue = issues.stream()
                    .filter(i -> "VALIDATED".equals(i.status())).findFirst().orElseThrow();
            assertThat(validatedIssue.compositeScore()).isNotNull();
        }

        @Test
        @DisplayName("AC7: Overall sprint score averages only VALIDATED issues")
        void overallScoreAveragesValidatedOnly() {
            when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));
            when(configRepository.findById(1L)).thenReturn(Optional.of(config));

            // review=90, impl=80, weights 40/60 → composite=84.0
            IssueValidationResult v1 = createResult(1L, 10L, "PROJ-1",
                    new BigDecimal("90.00"), new BigDecimal("80.00"), "VALIDATED");
            // review=70, impl=60, weights 40/60 → composite=64.0
            IssueValidationResult v2 = createResult(1L, 10L, "PROJ-2",
                    new BigDecimal("70.00"), new BigDecimal("60.00"), "VALIDATED");
            // SKIPPED — should not affect average
            IssueValidationResult sk = createResult(1L, 10L, "PROJ-3",
                    null, null, "SKIPPED");

            when(resultRepository.findBySprintId(1L)).thenReturn(List.of(v1, v2, sk));
            when(groupRepository.findAllById(any())).thenReturn(
                    List.of(createTeam(10L, "Team A", null)));

            SprintValidationResultsResponse response = service.getSprintResults(
                    1L, null, "coordinator", 100L);

            // Average of 84.0 and 64.0 = 74.0
            assertThat(response.data().teams().get(0).overallSprintScore()).isEqualTo(74.0);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Issue Details
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /issues/{issueKey}/details")
    class IssueDetails {

        @Test
        @DisplayName("AC6: Issue not yet validated → 404 ISSUE_NOT_VALIDATED")
        void issueNotValidated() {
            when(resultRepository.findTopByIssueKeyOrderByEvaluatedAtDesc("PROJ-999"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getIssueDetails("PROJ-999", "coordinator", 100L))
                    .isInstanceOf(P7ApiException.class)
                    .satisfies(ex -> {
                        P7ApiException p7 = (P7ApiException) ex;
                        assertThat(p7.getErrorCode()).isEqualTo("ISSUE_NOT_VALIDATED");
                        assertThat(p7.getStatus().value()).isEqualTo(404);
                    });
        }

        @Test
        @DisplayName("AC3: Advisor opening a non-advisee issue's details → 403")
        void advisorNonAdviseeIssue() {
            Long advisorUserId = 50L;
            Long ownTeamId = 10L;
            Long foreignTeamId = 99L;

            IssueValidationResult result = createResult(1L, foreignTeamId, "PROJ-123",
                    new BigDecimal("90.00"), new BigDecimal("80.00"), "VALIDATED");

            when(resultRepository.findTopByIssueKeyOrderByEvaluatedAtDesc("PROJ-123"))
                    .thenReturn(Optional.of(result));
            when(groupRepository.findByAdvisorId(advisorUserId)).thenReturn(
                    List.of(createTeam(ownTeamId, "My Team", advisorUserId)));

            assertThatThrownBy(() -> service.getIssueDetails("PROJ-123", "advisor", advisorUserId))
                    .isInstanceOf(P7ApiException.class)
                    .satisfies(ex -> {
                        P7ApiException p7 = (P7ApiException) ex;
                        assertThat(p7.getErrorCode()).isEqualTo("FORBIDDEN_TEAM_ACCESS");
                        assertThat(p7.getStatus().value()).isEqualTo(403);
                    });
        }

        @Test
        @DisplayName("Coordinator can view any issue's details → 200")
        void coordinatorCanViewAny() {
            IssueValidationResult result = createResult(1L, 10L, "PROJ-123",
                    new BigDecimal("90.00"), new BigDecimal("80.00"), "VALIDATED");
            result.setIssueDescription("Implement user login flow.");
            result.setPrUrl("https://github.com/org/repo/pull/42");

            when(resultRepository.findTopByIssueKeyOrderByEvaluatedAtDesc("PROJ-123"))
                    .thenReturn(Optional.of(result));

            IssueValidationDetailResponse response = service.getIssueDetails(
                    "PROJ-123", "coordinator", 100L);

            assertThat(response.status()).isEqualTo("success");
            assertThat(response.data().issueKey()).isEqualTo("PROJ-123");
            assertThat(response.data().issueDescription()).isEqualTo("Implement user login flow.");
            assertThat(response.data().prNumber()).isEqualTo(42);
            assertThat(response.data().prUrl()).isEqualTo("https://github.com/org/repo/pull/42");
            assertThat(response.data().reviewVerification()).isNotNull();
            assertThat(response.data().reviewVerification().score()).isEqualTo(90.0);
            assertThat(response.data().reviewVerification().reviewQuality()).isEqualTo("THOROUGH");
            assertThat(response.data().implementationValidation()).isNotNull();
            assertThat(response.data().implementationValidation().score()).isEqualTo(80.0);
        }

        @Test
        @DisplayName("Advisor can view own team issue details → 200")
        void advisorOwnTeamIssue() {
            Long advisorUserId = 50L;
            Long ownTeamId = 10L;

            IssueValidationResult result = createResult(1L, ownTeamId, "PROJ-123",
                    new BigDecimal("85.00"), new BigDecimal("75.00"), "VALIDATED");

            when(resultRepository.findTopByIssueKeyOrderByEvaluatedAtDesc("PROJ-123"))
                    .thenReturn(Optional.of(result));
            when(groupRepository.findByAdvisorId(advisorUserId)).thenReturn(
                    List.of(createTeam(ownTeamId, "My Team", advisorUserId)));

            IssueValidationDetailResponse response = service.getIssueDetails(
                    "PROJ-123", "advisor", advisorUserId);

            assertThat(response.status()).isEqualTo("success");
            assertThat(response.data().issueKey()).isEqualTo("PROJ-123");
        }

        @Test
        @DisplayName("Student role → 403 FORBIDDEN")
        void studentForbidden() {
            assertThatThrownBy(() -> service.getIssueDetails("PROJ-123", "student", 100L))
                    .isInstanceOf(P7ApiException.class)
                    .satisfies(ex -> {
                        P7ApiException p7 = (P7ApiException) ex;
                        assertThat(p7.getErrorCode()).isEqualTo("FORBIDDEN");
                        assertThat(p7.getStatus().value()).isEqualTo(403);
                    });
        }

        @Test
        @DisplayName("AC9: Response payload matches spec shape (reviewVerification + implementationValidation)")
        void responseShapeMatchesSpec() {
            IssueValidationResult result = createResult(1L, 10L, "PROJ-123",
                    new BigDecimal("90.00"), new BigDecimal("80.00"), "VALIDATED");
            result.setHasReview(true);
            result.setImplCoverageAreas("[{\"requirement\":\"Login endpoint\",\"covered\":true}]");
            result.setImplMissingRequirements("[\"Session management not implemented\"]");

            when(resultRepository.findTopByIssueKeyOrderByEvaluatedAtDesc("PROJ-123"))
                    .thenReturn(Optional.of(result));

            IssueValidationDetailResponse response = service.getIssueDetails(
                    "PROJ-123", "coordinator", 100L);

            // Review verification
            var rv = response.data().reviewVerification();
            assertThat(rv.score()).isEqualTo(90.0);
            assertThat(rv.hasReview()).isTrue();
            assertThat(rv.reviewQuality()).isEqualTo("THOROUGH");
            assertThat(rv.reviewerCount()).isEqualTo(2);
            assertThat(rv.hasChangeRequests()).isTrue();
            assertThat(rv.hasSubstantiveComments()).isTrue();
            assertThat(rv.aiFeedback()).isNotNull();

            // Implementation validation
            var iv = response.data().implementationValidation();
            assertThat(iv.score()).isEqualTo(80.0);
            assertThat(iv.isValid()).isTrue();
            assertThat(iv.coverageAreas()).hasSize(1);
            assertThat(iv.coverageAreas().get(0).requirement()).isEqualTo("Login endpoint");
            assertThat(iv.coverageAreas().get(0).covered()).isTrue();
            assertThat(iv.missingRequirements()).containsExactly("Session management not implemented");
            assertThat(iv.filesAnalyzed()).isEqualTo(4);
            assertThat(iv.diffTruncated()).isFalse();
        }
    }
}
