package com.spms.backend.controller;

import com.spms.backend.dto.response.ActiveSprintDto;
import com.spms.backend.service.ActiveSprintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ISSUE #396: Schedule Controller
 * GET /api/v1/schedules/active-sprint → Get active sprint context
 */
@Tag(name = "Schedule Management", description = "Sprint and schedule operations")
@RestController
@RequestMapping("/api/v1/schedules")
public class ScheduleController {

    private final ActiveSprintService activeSprintService;

    public ScheduleController(ActiveSprintService activeSprintService) {
        this.activeSprintService = activeSprintService;
    }

    /**
     * GET /api/v1/schedules/active-sprint
     * Retrieves the currently active sprint based on today's date.
     * Used by Cron Service to determine which sprint to synchronize.
     *
     * @return 200 OK with active sprint details
     * @throws NotFoundException (404) if no active sprint found
     */
    @Operation(
        summary = "Get Active Sprint",
        description = "Retrieves the currently active sprint based on today's date. " +
                      "Returns sprint details if found, or 404 if no active sprint exists."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Active sprint found",
            content = @Content(schema = @Schema(implementation = ActiveSprintDto.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "No active sprint found for the current date"
        )
    })
    @GetMapping("/active-sprint")
    public ResponseEntity<ActiveSprintDto> getActiveSprint() {
        ActiveSprintDto activeSprint = activeSprintService.getActiveSprint();
        return ResponseEntity.ok(activeSprint);
    }
}
