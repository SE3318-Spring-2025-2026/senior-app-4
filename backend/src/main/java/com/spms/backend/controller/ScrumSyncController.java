package com.spms.backend.controller;

import com.spms.backend.dto.response.ScrumSyncResponse;
import com.spms.backend.exception.SyncAlreadyRunningException;
import com.spms.backend.service.ScrumSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ISSUE #395: Scrum Sync REST Controller
 * POST /api/v1/scrum-sync/trigger → Trigger synchronization pipeline
 */
@Tag(name = "Scrum Synchronization", description = "JIRA & GitHub synchronization operations")
@RestController
@RequestMapping("/api/v1/scrum-sync")
public class ScrumSyncController {

    private final ScrumSyncService scrumSyncService;

    public ScrumSyncController(ScrumSyncService scrumSyncService) {
        this.scrumSyncService = scrumSyncService;
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
    @PostMapping("/trigger")
    public ResponseEntity<ScrumSyncResponse> triggerSync() {
        ScrumSyncResponse response = scrumSyncService.triggerSync();
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
