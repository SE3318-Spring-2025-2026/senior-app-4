package com.spms.backend.controller;

import com.spms.backend.dto.response.ScrumSyncResponse;
import com.spms.backend.service.ScrumSyncService;
import com.spms.backend.repository.SprintIssueTrackingRepository;
import com.spms.backend.model.SprintIssueTracking;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ISSUE #395: Scrum Sync REST Controller
 * POST /api/v1/scrum-sync/trigger → Trigger synchronization pipeline
 */
@Tag(name = "Scrum Synchronization", description = "JIRA & GitHub synchronization operations")
@RestController
@RequestMapping("/api/v1/scrum-sync")
public class ScrumSyncController {

    private final ScrumSyncService scrumSyncService;
    private final SprintIssueTrackingRepository sprintIssueTrackingRepository;

    public ScrumSyncController(ScrumSyncService scrumSyncService, SprintIssueTrackingRepository sprintIssueTrackingRepository) {
        this.scrumSyncService = scrumSyncService;
        this.sprintIssueTrackingRepository = sprintIssueTrackingRepository;
    }

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
    @PostMapping("/trigger")
    public ResponseEntity<ScrumSyncResponse> triggerSync() {
        ScrumSyncResponse response = scrumSyncService.triggerSync();
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @Operation(
        summary = "Trigger Scrum Synchronization for a specific group",
        description = "Initiates asynchronous synchronization of JIRA and GitHub data for the specified group in the active sprint."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "202",
            description = "Synchronization pipeline started",
            content = @Content(schema = @Schema(implementation = ScrumSyncResponse.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Sync already in progress"
        )
    })
    @PostMapping("/trigger/group/{groupId}")
    public ResponseEntity<ScrumSyncResponse> triggerSyncForGroup(@PathVariable Long groupId) {
        ScrumSyncResponse response = scrumSyncService.triggerSyncForGroup(groupId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @Operation(
        summary = "Get all synchronized sprint issue tracking records",
        description = "Returns all tracked issues synchronized from Jira and GitHub."
    )
    @GetMapping("/records")
    public ResponseEntity<List<SprintIssueTracking>> getAllSyncRecords() {
        List<SprintIssueTracking> records = sprintIssueTrackingRepository.findAll();
        System.out.println("RETURNING RECORDS COUNT: " + records.size());
        return ResponseEntity.ok(records);
    }
}
