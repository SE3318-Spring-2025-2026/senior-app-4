package com.spms.backend.api;

import com.spms.api.BaseApiTest;
import com.spms.api.TestDataFactory;
import com.spms.backend.model.*;
import com.spms.backend.repository.*;
import com.spms.backend.service.TokenService;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AiValidationJobsApiTest extends BaseApiTest {

    @Autowired private UserRepository userRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired private SprintRepository sprintRepository;
    @Autowired private SprintIssueTrackingRepository sprintIssueTrackingRepository;
    @Autowired private ValidationJobRepository validationJobRepository;
    @Autowired private SystemLogRepository systemLogRepository;
    @Autowired private TokenService tokenService;

    private String coordinatorToken;
    private String advisorToken;
    private String studentToken;
    private User advisor;
    private Group group;
    private Sprint sprint;

    @BeforeEach
    void setupData() {
        User coordinator = new User();
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

        User student = new User();
        student.setFullName("Student");
        student.setEmail("student-" + System.nanoTime() + "@spms.com");
        student.setRole("student");
        student.setStudentId(TestDataFactory.uniqueStudentId());
        student.setGithubUsername(TestDataFactory.uniqueGithubUsername());
        student.setCreatedAt(Instant.now());
        student = userRepository.save(student);
        studentToken = tokenService.generateToken(student);

        sprint = new Sprint("Sprint-" + System.nanoTime(), LocalDate.now().minusDays(2), LocalDate.now().plusDays(2), "Active");
        sprint = sprintRepository.save(sprint);

        group = new Group();
        group.setGroupName("Team-" + System.nanoTime());
        group.setLeader(student);
        group.setAdvisor(advisor);
        group.setStatus(GroupStatus.FORMED);
        group = groupRepository.save(group);
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

        given()
                .header("Authorization", "Bearer " + coordinatorToken)
                .when()
                .post("/api/v1/ai-validation/jobs/" + failed.getJobId() + "/retry")
                .then()
                .statusCode(409)
                .body("error", equalTo("JOB_RETRY_ALREADY_RUNNING"));

        Long failedJobId = failed.getJobId();
        List<ValidationJob> jobs = validationJobRepository.findAll().stream()
                .filter(j -> j.getParentJob() != null && failedJobId.equals(j.getParentJob().getJobId()))
                .toList();
        assertEquals(1, jobs.size());
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
}
