package com.spms.backend.controller;

import com.spms.backend.client.JiraApiClient;
import com.spms.backend.dto.JiraIssueData;
import com.spms.backend.dto.response.ScrumSyncResponse;
import com.spms.backend.exception.SyncAlreadyRunningException;
import com.spms.backend.model.JiraIntegration;
import com.spms.backend.model.SprintIssueTracking;
import com.spms.backend.repository.JiraIntegrationRepository;
import com.spms.backend.repository.SprintIssueTrackingRepository;
import com.spms.backend.service.ScrumSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ISSUE #395: Scrum Sync REST Controller
 * POST /api/v1/scrum-sync/trigger → Trigger synchronization pipeline
 */
@Tag(name = "Scrum Synchronization", description = "JIRA & GitHub synchronization operations")
@RestController
@RequestMapping("/api/v1/scrum-sync")
public class ScrumSyncController {

    private final ScrumSyncService scrumSyncService;
    private final JiraIntegrationRepository jiraIntegrationRepository;
    private final JiraApiClient jiraApiClient;
    private final SprintIssueTrackingRepository sprintIssueTrackingRepository;

    public ScrumSyncController(ScrumSyncService scrumSyncService,
                                JiraIntegrationRepository jiraIntegrationRepository,
                                JiraApiClient jiraApiClient,
                                SprintIssueTrackingRepository sprintIssueTrackingRepository) {
        this.scrumSyncService = scrumSyncService;
        this.jiraIntegrationRepository = jiraIntegrationRepository;
        this.jiraApiClient = jiraApiClient;
        this.sprintIssueTrackingRepository = sprintIssueTrackingRepository;
    }

    /**
     * POST /api/v1/scrum-sync/trigger
     * Triggers the synchronization pipeline asynchronously.
     * Returns 202 Accepted immediately without waiting for completion.
     *
     * @return 202 Accepted with sync status
     * @throws SyncAlreadyRunningException (409 Conflict) if sync is already in progress
     */
    @Operation(
        summary = "Trigger Scrum Synchronization",
        description = "Initiates asynchronous synchronization of JIRA and GitHub data for the active sprint. Returns immediately without waiting for completion."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "202",
            description = "Synchronization pipeline started",
            content = @Content(schema = @Schema(implementation = ScrumSyncResponse.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Sync already in progress - Another synchronization process is currently running"
        )
    })
    @GetMapping("/debug/jira/{groupId}")
    public ResponseEntity<Map<String, Object>> debugJira(@PathVariable Long groupId) {
        Map<String, Object> result = new HashMap<>();
        Optional<JiraIntegration> opt = jiraIntegrationRepository.findByGroup_Id(groupId);
        if (opt.isEmpty()) {
            result.put("error", "No JIRA integration found for group " + groupId);
            return ResponseEntity.ok(result);
        }
        JiraIntegration jira = opt.get();
        result.put("groupId", groupId);
        result.put("jiraSpaceUrl", jira.getJiraSpaceUrl());
        result.put("projectKey", jira.getProjectKey());
        result.put("jiraEmail", jira.getJiraEmail());
        result.put("apiKeyNull", jira.getApiKey() == null);
        result.put("status", jira.getStatus());

        if (jira.getJiraEmail() == null || jira.getApiKey() == null) {
            result.put("fetchResult", "SKIPPED: email or apiKey is null");
            return ResponseEntity.ok(result);
        }
        try {
            String token = jira.getApiKey(); // EncryptionConverter already decrypts on entity load
            List<JiraIssueData> issues = jiraApiClient.fetchActiveSprintIssues(
                jira.getJiraSpaceUrl(), jira.getJiraEmail(), token, jira.getProjectKey());
            result.put("fetchResult", "SUCCESS");
            result.put("issueCount", issues.size());
            result.put("issues", issues);
        } catch (Exception e) {
            result.put("fetchResult", "ERROR: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/debug/tracking/{groupId}")
    public ResponseEntity<Map<String, Object>> debugTracking(@PathVariable Long groupId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<SprintIssueTracking> records = sprintIssueTrackingRepository.findByGroup_Id(groupId);
            result.put("count", records.size());
            List<Map<String, Object>> rows = records.stream().map(r -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", r.getId());
                m.put("issueKey", r.getIssueKey());
                m.put("issueTitle", r.getIssueTitle());
                m.put("issueDescription", r.getIssueDescription());
                m.put("storyPoints", r.getStoryPoints());
                m.put("prNumber", r.getPrNumber());
                m.put("prMerged", r.getPrMerged());
                m.put("assigneeGithubUsername", r.getAssigneeGithubUsername());
                m.put("syncedAt", r.getSyncedAt() != null ? r.getSyncedAt().toString() : null);
                return m;
            }).toList();
            result.put("records", rows);
        } catch (Exception e) {
            result.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
            if (e.getCause() != null) result.put("cause", e.getCause().getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/trigger")
    public ResponseEntity<ScrumSyncResponse> triggerSync() {
        ScrumSyncResponse response = scrumSyncService.triggerSync();
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping("/debug/run-sync-now")
    public ResponseEntity<Map<String, Object>> runSyncNow() {
        Map<String, Object> result = new HashMap<>();
        try {
            scrumSyncService.executeSyncPipeline();
            result.put("status", "COMPLETED");
        } catch (Exception e) {
            result.put("status", "FAILED");
            result.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
            if (e.getCause() != null) result.put("cause", e.getCause().getMessage());
        }
        return ResponseEntity.ok(result);
    }
}
