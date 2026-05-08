package com.spms.backend.api;

import com.spms.api.BaseApiTest;
import com.spms.api.TestDataFactory;
import com.spms.backend.model.*;
import com.spms.backend.repository.*;
import com.spms.backend.service.TokenService;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AiValidationJobsApiTest extends BaseApiTest {

        @Autowired
        private UserRepository userRepository;
        @Autowired
        private GroupRepository groupRepository;
        @Autowired
        private SprintRepository sprintRepository;
        @Autowired
        private SprintIssueTrackingRepository sprintIssueTrackingRepository;
        @Autowired
        private ValidationJobRepository validationJobRepository;
        @Autowired
        private SystemLogRepository systemLogRepository;
        @Autowired
        private IssueValidationResultRepository issueValidationResultRepository;
        @Autowired
        private ValidationConfigRepository validationConfigRepository;
        @Autowired
        private TokenService tokenService;

        private String coordinatorToken;
        private String advisorToken;
        private String studentToken;
        private User coordinator;
        private User advisor;
        private User student;
        private Group group;
        private Sprint sprint;
        private final List<Long> extraUserIds = new ArrayList<>();

        @AfterEach
        void cleanup() {
                if (sprint == null)
                        return;
                Long sprintId = sprint.getId();

                // 1. Find all jobs for this sprint
                List<ValidationJob> jobs = validationJobRepository.findAll().stream()
                                .filter(j -> j.getSprint() != null && sprintId.equals(j.getSprint().getId()))
                                .collect(Collectors.toList());
                List<Long> jobIds = jobs.stream().map(ValidationJob::getJobId).collect(Collectors.toList());

                // 2. Delete issue_validation_results first (FK child of validation_jobs)
                jobIds.forEach(id -> issueValidationResultRepository.deleteAll(
                                issueValidationResultRepository.findByJob_JobId(id)));

                // 3. Nullify self-referencing parent_job_id before bulk delete to avoid FK
                // violations
                jobs.forEach(j -> j.setParentJob(null));
                validationJobRepository.saveAll(jobs);
                validationJobRepository.deleteAll(jobs);

                // 2. Sprint issue tracking
                sprintIssueTrackingRepository.deleteAll(
                                sprintIssueTrackingRepository.findBySprint_Id(sprintId));

                // 3. Group
                if (group != null) {
                        groupRepository.deleteById(group.getId());
                }

                // 4. Sprint
                sprintRepository.deleteById(sprintId);

                // 5. Core users created in @BeforeEach
                if (student != null)
                        userRepository.deleteById(student.getUserId());
                if (advisor != null)
                        userRepository.deleteById(advisor.getUserId());
                if (coordinator != null)
                        userRepository.deleteById(coordinator.getUserId());

                // 6. Extra users created inside individual test methods
                extraUserIds.forEach(id -> userRepository.deleteById(id));
        }

        @BeforeEach
        void setupData() {
                extraUserIds.clear();

                coordinator = new User();
                coordinator.setFullName("Coord");
                coordinator.setEmail("coord-" + System.nanoTime() + "@spms.com");
                coordinator.setRole("coordinator");
                coordinator.setCreatedAt(Instant.now());
                coordinator = userRepository.save(coordinator);
                coordinatorToken = tokenService.generateToken(coordinator);

                advisor = new User();
                advisor.setFullName("Advisor");
                advisor.setEmail("advisor-" + System.nanoTime() + "@spms.com");
                advisor.setRole("advisor");
                advisor.setCreatedAt(Instant.now());
                advisor = userRepository.save(advisor);
                advisorToken = tokenService.generateToken(advisor);

                student = new User();
                student.setFullName("Student");
                student.setEmail("student-" + System.nanoTime() + "@spms.com");
                student.setRole("student");
                student.setStudentId(TestDataFactory.uniqueStudentId());
                student.setGithubUsername(TestDataFactory.uniqueGithubUsername());
                student.setCreatedAt(Instant.now());
                student = userRepository.save(student);
                studentToken = tokenService.generateToken(student);

                sprint = new Sprint("Sprint-" + System.nanoTime(), LocalDate.now().minusDays(2),
                                LocalDate.now().plusDays(2), "Active");
                sprint = sprintRepository.save(sprint);

                group = new Group();
                group.setGroupName("Team-" + System.nanoTime());
                group.setLeader(student);
                group.setAdvisor(advisor);
                group.setStatus(GroupStatus.FORMED);
                group = groupRepository.save(group);

                // Ensure the ValidationConfig singleton (id=1) exists for tests that need it
                if (!validationConfigRepository.existsById(1L)) {
                        ValidationConfig cfg = new ValidationConfig();
                        cfg.setId(1L);
                        cfg.setReviewWeight(50);
                        cfg.setImplementationWeight(50);
                        cfg.setOpenaiModel("gpt-4o");
                        cfg.setMaxDiffLines(1000);
                        validationConfigRepository.save(cfg);
                }
        }

        @Test
        void triggerReturns404WhenSprintNotFound() {
                given()
                                .contentType(ContentType.JSON)
                                .header("Authorization", "Bearer " + coordinatorToken)
                                .when()
                                .post("/api/v1/ai-validation/sprints/999999/trigger")
                                .then()
                                .statusCode(404)
                                .body("error", equalTo("SPRINT_NOT_FOUND"))
                                .body("message", equalTo("Sprint not found."));
        }

        @Test
        void triggerReturns400WhenNoIssuesInSprint() {
                given()
                                .contentType(ContentType.JSON)
                                .header("Authorization", "Bearer " + coordinatorToken)
                                .when()
                                .post("/api/v1/ai-validation/sprints/" + sprint.getId() + "/trigger")
                                .then()
                                .statusCode(400)
                                .body("error", equalTo("NO_ISSUES_IN_SPRINT"));
        }

        @Test
        void triggerReturns202AndQueuedStatus() {
                SprintIssueTracking sit = new SprintIssueTracking(group, sprint, "P7-101");
                sprintIssueTrackingRepository.save(sit);

                given()
                                .contentType(ContentType.JSON)
                                .header("Authorization", "Bearer " + coordinatorToken)
                                .body(Map.of("teamId", group.getId()))
                                .when()
                                .post("/api/v1/ai-validation/sprints/" + sprint.getId() + "/trigger")
                                .then()
                                .statusCode(202)
                                .body("data.status", equalTo("QUEUED"))
                                .body("data.teamId", equalTo(group.getId().intValue()));

                long enqueueLogs = systemLogRepository.findAll().stream()
                                .filter(l -> "P7_VALIDATION_ENQUEUED".equals(l.getEventType()))
                                .count();
                org.junit.jupiter.api.Assertions.assertTrue(enqueueLogs >= 1);
        }

        @Test
        void triggerReturns409WhenValidationAlreadyRunning() {
                SprintIssueTracking sit = new SprintIssueTracking(group, sprint, "P7-102");
                sprintIssueTrackingRepository.save(sit);

                ValidationJob existing = new ValidationJob();
                existing.setSprint(sprint);
                existing.setTeam(group);
                existing.setJobStatus(ValidationJobStatus.QUEUED);
                existing.setCurrentStep(ValidationJobStep.LOADING_CONTEXT);
                existing.setIssuesTotal(1);
                existing.setStartedAt(Instant.now());
                validationJobRepository.save(existing);

                given()
                                .contentType(ContentType.JSON)
                                .header("Authorization", "Bearer " + coordinatorToken)
                                .body(Map.of("teamId", group.getId()))
                                .when()
                                .post("/api/v1/ai-validation/sprints/" + sprint.getId() + "/trigger")
                                .then()
                                .statusCode(409)
                                .body("error", equalTo("VALIDATION_ALREADY_RUNNING"));
        }

        @Test
        void getStatusReturns403ForNonAdviseeAdvisor() {
                User otherAdvisor = new User();
                otherAdvisor.setFullName("Other Advisor");
                otherAdvisor.setEmail("other-advisor-" + System.nanoTime() + "@spms.com");
                otherAdvisor.setRole("advisor");
                otherAdvisor.setCreatedAt(Instant.now());
                otherAdvisor = userRepository.save(otherAdvisor);
                extraUserIds.add(otherAdvisor.getUserId());
                String otherAdvisorToken = tokenService.generateToken(otherAdvisor);

                ValidationJob job = new ValidationJob();
                job.setSprint(sprint);
                job.setTeam(group);
                job.setJobStatus(ValidationJobStatus.IN_PROGRESS);
                job.setCurrentStep(ValidationJobStep.FETCHING_DIFFS);
                job.setProgressPercentage(45);
                job.setIssuesTotal(10);
                job.setIssuesCompleted(4);
                job.setIssuesFailed(1);
                job.setStartedAt(Instant.now());
                job = validationJobRepository.save(job);

                given()
                                .contentType(ContentType.JSON)
                                .header("Authorization", "Bearer " + otherAdvisorToken)
                                .when()
                                .get("/api/v1/ai-validation/jobs/" + job.getJobId())
                                .then()
                                .statusCode(403)
                                .body("error", equalTo("FORBIDDEN_TEAM_ACCESS"));
        }

        @Test
        void getStatusReturns200WithStructuredFields() {
                ValidationJob job = new ValidationJob();
                job.setSprint(sprint);
                job.setTeam(group);
                job.setJobStatus(ValidationJobStatus.IN_PROGRESS);
                job.setCurrentStep(ValidationJobStep.FETCHING_DIFFS);
                job.setProgressPercentage(45);
                job.setIssuesTotal(10);
                job.setIssuesCompleted(4);
                job.setIssuesFailed(1);
                job.setFailureReason(null);
                job.setStartedAt(Instant.now());
                job = validationJobRepository.save(job);

                given()
                                .contentType(ContentType.JSON)
                                .header("Authorization", "Bearer " + advisorToken)
                                .when()
                                .get("/api/v1/ai-validation/jobs/" + job.getJobId())
                                .then()
                                .statusCode(200)
                                .body("status", equalTo("success"))
                                .body("data.jobStatus", equalTo("IN_PROGRESS"))
                                .body("data.currentStep", equalTo("FETCHING_DIFFS"))
                                .body("data.progressPercentage", equalTo(45))
                                .body("data.issuesTotal", equalTo(10))
                                .body("data.issuesCompleted", equalTo(4))
                                .body("data.issuesFailed", equalTo(1));
        }

        @Test
        void getStatusReturns404WhenJobMissing() {
                given()
                                .contentType(ContentType.JSON)
                                .header("Authorization", "Bearer " + coordinatorToken)
                                .when()
                                .get("/api/v1/ai-validation/jobs/999999")
                                .then()
                                .statusCode(404)
                                .body("error", equalTo("JOB_NOT_FOUND"));
        }

        @Test
        void retryFlowCoversNotRetryableAcceptedAndDedup() {
                ValidationJob completed = new ValidationJob();
                completed.setSprint(sprint);
                completed.setTeam(group);
                completed.setJobStatus(ValidationJobStatus.COMPLETED);
                completed.setCurrentStep(ValidationJobStep.STORING_RESULTS);
                completed.setIssuesTotal(4);
                completed.setStartedAt(Instant.now());
                completed.setCompletedAt(Instant.now());
                completed = validationJobRepository.save(completed);

                given()
                                .header("Authorization", "Bearer " + coordinatorToken)
                                .when()
                                .post("/api/v1/ai-validation/jobs/" + completed.getJobId() + "/retry")
                                .then()
                                .statusCode(400)
                                .body("error", equalTo("JOB_NOT_RETRYABLE"));

                ValidationJob failed = new ValidationJob();
                failed.setSprint(sprint);
                failed.setTeam(group);
                failed.setJobStatus(ValidationJobStatus.FAILED);
                failed.setCurrentStep(ValidationJobStep.AI_IMPLEMENTATION_VALIDATION);
                failed.setIssuesTotal(5);
                failed.setIssuesFailed(2);
                failed.setStartedAt(Instant.now());
                failed.setCompletedAt(Instant.now());
                failed = validationJobRepository.save(failed);

                given()
                                .header("Authorization", "Bearer " + coordinatorToken)
                                .when()
                                .post("/api/v1/ai-validation/jobs/" + failed.getJobId() + "/retry")
                                .then()
                                .statusCode(202)
                                .body("data.status", equalTo("QUEUED"))
                                .body("data.issueCount", equalTo(2));

                // The async pipeline may change the retry job status quickly. Force it back to
                // QUEUED so the dedup check is deterministic before we attempt the second retry.
                Long failedJobId = failed.getJobId();
                List<ValidationJob> retryJobs = validationJobRepository.findAll().stream()
                                .filter(j -> j.getParentJob() != null
                                                && failedJobId.equals(j.getParentJob().getJobId()))
                                .toList();
                assertEquals(1, retryJobs.size());
                ValidationJob retryJob = retryJobs.get(0);
                retryJob.setJobStatus(ValidationJobStatus.QUEUED);
                validationJobRepository.save(retryJob);

                given()
                                .header("Authorization", "Bearer " + coordinatorToken)
                                .when()
                                .post("/api/v1/ai-validation/jobs/" + failed.getJobId() + "/retry")
                                .then()
                                .statusCode(409)
                                .body("error", equalTo("JOB_RETRY_ALREADY_RUNNING"));
        }

        @Test
        void triggerReturns403ForAdvisorAndStudent() {
                SprintIssueTracking sit = new SprintIssueTracking(group, sprint, "P7-999");
                sprintIssueTrackingRepository.save(sit);

                given()
                                .contentType(ContentType.JSON)
                                .header("Authorization", "Bearer " + advisorToken)
                                .body(Map.of("teamId", group.getId()))
                                .when()
                                .post("/api/v1/ai-validation/sprints/" + sprint.getId() + "/trigger")
                                .then()
                                .statusCode(403)
                                .body("error", equalTo("FORBIDDEN"));

                given()
                                .contentType(ContentType.JSON)
                                .header("Authorization", "Bearer " + studentToken)
                                .body(Map.of("teamId", group.getId()))
                                .when()
                                .post("/api/v1/ai-validation/sprints/" + sprint.getId() + "/trigger")
                                .then()
                                .statusCode(403)
                                .body("error", equalTo("FORBIDDEN"));
        }

        @Test
        void retryReturns403ForAdvisor() {
                ValidationJob failed = new ValidationJob();
                failed.setSprint(sprint);
                failed.setTeam(group);
                failed.setJobStatus(ValidationJobStatus.FAILED);
                failed.setCurrentStep(ValidationJobStep.AI_IMPLEMENTATION_VALIDATION);
                failed.setIssuesTotal(3);
                failed.setIssuesFailed(1);
                failed.setStartedAt(Instant.now());
                failed.setCompletedAt(Instant.now());
                failed = validationJobRepository.save(failed);

                given()
                                .contentType(ContentType.JSON)
                                .header("Authorization", "Bearer " + advisorToken)
                                .when()
                                .post("/api/v1/ai-validation/jobs/" + failed.getJobId() + "/retry")
                                .then()
                                .statusCode(403)
                                .body("error", equalTo("FORBIDDEN"));
        }

        @Test
        void activeJobReturns204WhenNoActiveJob() {
                given()
                                .header("Authorization", "Bearer " + coordinatorToken)
                                .when()
                                .get("/api/v1/ai-validation/sprints/" + sprint.getId() + "/active-job")
                                .then()
                                .statusCode(204);
        }

        @Test
        void activeJobReturns200WithJobStatusWhenActive() {
                ValidationJob active = new ValidationJob();
                active.setSprint(sprint);
                active.setTeam(group);
                active.setJobStatus(ValidationJobStatus.IN_PROGRESS);
                active.setCurrentStep(ValidationJobStep.AI_REVIEW_VERIFICATION);
                active.setProgressPercentage(60);
                active.setIssuesTotal(5);
                active.setIssuesCompleted(3);
                active.setIssuesFailed(0);
                active.setStartedAt(Instant.now());
                validationJobRepository.save(active);

                given()
                                .header("Authorization", "Bearer " + coordinatorToken)
                                .when()
                                .get("/api/v1/ai-validation/sprints/" + sprint.getId() + "/active-job")
                                .then()
                                .statusCode(200)
                                .body("status", equalTo("success"))
                                .body("data.jobStatus", equalTo("IN_PROGRESS"))
                                .body("data.currentStep", equalTo("AI_REVIEW_VERIFICATION"))
                                .body("data.progressPercentage", equalTo(60));
        }

        @Test
        void activeJobReturns404WhenSprintNotFound() {
                given()
                                .header("Authorization", "Bearer " + coordinatorToken)
                                .when()
                                .get("/api/v1/ai-validation/sprints/999999/active-job")
                                .then()
                                .statusCode(404)
                                .body("error", equalTo("SPRINT_NOT_FOUND"));
        }

        @Test
        void activeJobReturns403ForStudent() {
                given()
                                .header("Authorization", "Bearer " + studentToken)
                                .when()
                                .get("/api/v1/ai-validation/sprints/" + sprint.getId() + "/active-job")
                                .then()
                                .statusCode(403)
                                .body("error", equalTo("FORBIDDEN"));
        }

        @Test
        void activeJobReturns403ForAdvisorOnNonAdviseeTeam() {
                User otherAdvisor = new User();
                otherAdvisor.setFullName("Other Advisor");
                otherAdvisor.setEmail("other-advisor2-" + System.nanoTime() + "@spms.com");
                otherAdvisor.setRole("advisor");
                otherAdvisor.setCreatedAt(Instant.now());
                otherAdvisor = userRepository.save(otherAdvisor);
                extraUserIds.add(otherAdvisor.getUserId());
                String otherToken = tokenService.generateToken(otherAdvisor);

                ValidationJob active = new ValidationJob();
                active.setSprint(sprint);
                active.setTeam(group);
                active.setJobStatus(ValidationJobStatus.QUEUED);
                active.setCurrentStep(ValidationJobStep.LOADING_CONTEXT);
                active.setProgressPercentage(0);
                active.setIssuesTotal(3);
                active.setIssuesCompleted(0);
                active.setIssuesFailed(0);
                active.setStartedAt(Instant.now());
                validationJobRepository.save(active);

                given()
                                .header("Authorization", "Bearer " + otherToken)
                                .when()
                                .get("/api/v1/ai-validation/sprints/" + sprint.getId() + "/active-job")
                                .then()
                                .statusCode(403)
                                .body("error", equalTo("FORBIDDEN_TEAM_ACCESS"));
        }

        @Test
        void getResultsReturns200ForCoordinator() {
                ValidationJob job = new ValidationJob();
                job.setSprint(sprint);
                job.setTeam(group);
                job.setJobStatus(ValidationJobStatus.COMPLETED);
                job.setStartedAt(Instant.now());
                job = validationJobRepository.save(job);

                IssueValidationResult res = new IssueValidationResult();
                res.setJob(job);
                res.setSprintId(sprint.getId());
                res.setTeamId(group.getId());
                res.setIssueKey("P7-COORD-1");
                res.setValidationStatus("PASSED");
                res.setEvaluatedAt(Instant.now());
                issueValidationResultRepository.save(res);

                given()
                                .header("Authorization", "Bearer " + coordinatorToken)
                                .when()
                                .get("/api/v1/ai-validation/sprints/" + sprint.getId() + "/results")
                                .then()
                                .statusCode(200)
                                .body("status", equalTo("success"))
                                .body("data.teams", notNullValue());
        }

        @Test
        void getResultsReturns200ForAdvisorOwnTeam() {
                ValidationJob job = new ValidationJob();
                job.setSprint(sprint);
                job.setTeam(group);
                job.setJobStatus(ValidationJobStatus.COMPLETED);
                job.setStartedAt(Instant.now());
                job = validationJobRepository.save(job);

                IssueValidationResult res = new IssueValidationResult();
                res.setJob(job);
                res.setSprintId(sprint.getId());
                res.setTeamId(group.getId());
                res.setIssueKey("P7-ADV-1");
                res.setValidationStatus("PASSED");
                res.setEvaluatedAt(Instant.now());
                issueValidationResultRepository.save(res);

                given()
                                .header("Authorization", "Bearer " + advisorToken)
                                .when()
                                .get("/api/v1/ai-validation/sprints/" + sprint.getId() + "/results")
                                .then()
                                .statusCode(200)
                                .body("status", equalTo("success"));
        }

        @Test
        void getResultsReturns403ForAdvisorOtherTeam() {
                User otherAdvisor = new User();
                otherAdvisor.setFullName("Other Advisor");
                otherAdvisor.setEmail("other-adv-" + System.nanoTime() + "@spms.com");
                otherAdvisor.setRole("advisor");
                otherAdvisor.setCreatedAt(Instant.now());
                otherAdvisor = userRepository.save(otherAdvisor);
                extraUserIds.add(otherAdvisor.getUserId());
                String otherToken = tokenService.generateToken(otherAdvisor);

                given()
                                .header("Authorization", "Bearer " + otherToken)
                                .queryParam("teamId", group.getId())
                                .when()
                                .get("/api/v1/ai-validation/sprints/" + sprint.getId() + "/results")
                                .then()
                                .statusCode(403)
                                .body("error", equalTo("FORBIDDEN_TEAM_ACCESS"));
        }

        @Test
        void getResultsReturns403ForStudent() {
                given()
                                .header("Authorization", "Bearer " + studentToken)
                                .when()
                                .get("/api/v1/ai-validation/sprints/" + sprint.getId() + "/results")
                                .then()
                                .statusCode(403)
                                .body("error", equalTo("FORBIDDEN"));
        }

        @Test
        void getResultsReturns404WhenSprintNotFound() {
                given()
                                .header("Authorization", "Bearer " + coordinatorToken)
                                .when()
                                .get("/api/v1/ai-validation/sprints/999999/results")
                                .then()
                                .statusCode(404)
                                .body("error", equalTo("SPRINT_NOT_FOUND"));
        }

        @Test
        void getIssueDetailsReturns200ForCoordinator() {
                ValidationJob job = new ValidationJob();
                job.setSprint(sprint);
                job.setTeam(group);
                job.setJobStatus(ValidationJobStatus.COMPLETED);
                job.setStartedAt(Instant.now());
                job = validationJobRepository.save(job);

                IssueValidationResult res = new IssueValidationResult();
                res.setJob(job);
                res.setIssueKey("P7-TEST-1");
                res.setValidationStatus("PASSED");
                res.setEvaluatedAt(Instant.now());
                issueValidationResultRepository.save(res);

                given()
                                .header("Authorization", "Bearer " + coordinatorToken)
                                .when()
                                .get("/api/v1/ai-validation/issues/P7-TEST-1/details")
                                .then()
                                .statusCode(200)
                                .body("status", equalTo("success"))
                                .body("data.issueKey", equalTo("P7-TEST-1"));
        }

        @Test
        void getIssueDetailsReturns403ForAdvisorOtherTeam() {
                User otherAdvisor = new User();
                otherAdvisor.setFullName("Other Advisor");
                otherAdvisor.setEmail("other-adv-issue-" + System.nanoTime() + "@spms.com");
                otherAdvisor.setRole("advisor");
                otherAdvisor.setCreatedAt(Instant.now());
                otherAdvisor = userRepository.save(otherAdvisor);
                extraUserIds.add(otherAdvisor.getUserId());
                String otherToken = tokenService.generateToken(otherAdvisor);

                ValidationJob job = new ValidationJob();
                job.setSprint(sprint);
                job.setTeam(group);
                job.setJobStatus(ValidationJobStatus.COMPLETED);
                job.setStartedAt(Instant.now());
                job = validationJobRepository.save(job);

                IssueValidationResult res = new IssueValidationResult();
                res.setJob(job);
                res.setIssueKey("P7-TEST-2");
                res.setValidationStatus("PASSED");
                res.setEvaluatedAt(Instant.now());
                issueValidationResultRepository.save(res);

                given()
                                .header("Authorization", "Bearer " + otherToken)
                                .when()
                                .get("/api/v1/ai-validation/issues/P7-TEST-2/details")
                                .then()
                                .statusCode(403)
                                .body("error", equalTo("FORBIDDEN_TEAM_ACCESS"));
        }

        @Test
        void getIssueDetailsReturns403ForStudent() {
                given()
                                .header("Authorization", "Bearer " + studentToken)
                                .when()
                                .get("/api/v1/ai-validation/issues/P7-ANY/details")
                                .then()
                                .statusCode(403)
                                .body("error", equalTo("FORBIDDEN"));
        }

        @Test
        void getIssueDetailsReturns404ForNonexistentIssue() {
                given()
                                .header("Authorization", "Bearer " + coordinatorToken)
                                .when()
                                .get("/api/v1/ai-validation/issues/NONEXISTENT-999/details")
                                .then()
                                .statusCode(404)
                                .body("error", equalTo("ISSUE_NOT_VALIDATED"));
        }

        @Test
        void getConfigReturns200ForCoordinator() {
                if (!validationConfigRepository.existsById(1L)) {
                        ValidationConfig config = new ValidationConfig();
                        config.setId(1L);
                        config.setReviewWeight(50);
                        config.setImplementationWeight(50);
                        config.setOpenaiModel("gpt-4");
                        config.setMaxDiffLines(1000);
                        validationConfigRepository.save(config);
                }

                given()
                                .header("Authorization", "Bearer " + coordinatorToken)
                                .when()
                                .get("/api/v1/ai-validation/config")
                                .then()
                                .statusCode(200)
                                .body("status", equalTo("success"))
                                .body("data.reviewWeight", notNullValue())
                                .body("data.implementationWeight", notNullValue());
        }

        @Test
        void getConfigReturns403ForNonCoordinator() {
                // ForbiddenException is handled by GlobalExceptionHandler which uses
                // HttpStatus.getReasonPhrase() → "Forbidden" (not "FORBIDDEN")
                given()
                                .header("Authorization", "Bearer " + advisorToken)
                                .when()
                                .get("/api/v1/ai-validation/config")
                                .then()
                                .statusCode(403)
                                .body("error", equalTo("Forbidden"));
        }

        @Test
        void putConfigReturns400WhenWeightsDontSumTo100() {
                // BadRequestException → GlobalExceptionHandler uses HttpStatus.getReasonPhrase()
                // for the "error" field: "Bad Request". The specific code is in "message".
                given()
                                .contentType(ContentType.JSON)
                                .header("Authorization", "Bearer " + coordinatorToken)
                                .body(Map.of("reviewWeight", 60, "implementationWeight", 60))
                                .when()
                                .put("/api/v1/ai-validation/config")
                                .then()
                                .statusCode(400)
                                .body("message", containsString("INVALID_WEIGHTS"));
        }

        @Test
        void putConfigReturns400WhenModelInvalid() {
                given()
                                .contentType(ContentType.JSON)
                                .header("Authorization", "Bearer " + coordinatorToken)
                                .body(Map.of(
                                                "reviewWeight", 40,
                                                "implementationWeight", 60,
                                                "openaiModel", "banana-model"))
                                .when()
                                .put("/api/v1/ai-validation/config")
                                .then()
                                .statusCode(400)
                                .body("message", containsString("INVALID_OPENAI_MODEL"));
        }

        @Test
        void putConfigReturns403ForNonCoordinator() {
                // ForbiddenException → GlobalExceptionHandler → error = "Forbidden"
                given()
                                .contentType(ContentType.JSON)
                                .header("Authorization", "Bearer " + advisorToken)
                                .body(Map.of("reviewWeight", 50, "implementationWeight", 50))
                                .when()
                                .put("/api/v1/ai-validation/config")
                                .then()
                                .statusCode(403)
                                .body("error", equalTo("Forbidden"));
        }

        @Test
        void putConfigReturns200AndPersistsUpdate() {
                given()
                                .contentType(ContentType.JSON)
                                .header("Authorization", "Bearer " + coordinatorToken)
                                .body(Map.of(
                                                "reviewWeight", 40,
                                                "implementationWeight", 60,
                                                "openaiModel", "gpt-4o",
                                                "maxDiffLines", 500))
                                .when()
                                .put("/api/v1/ai-validation/config")
                                .then()
                                .statusCode(200);

                given()
                                .header("Authorization", "Bearer " + coordinatorToken)
                                .when()
                                .get("/api/v1/ai-validation/config")
                                .then()
                                .statusCode(200)
                                .body("data.reviewWeight", equalTo(40))
                                .body("data.implementationWeight", equalTo(60));
        }
}