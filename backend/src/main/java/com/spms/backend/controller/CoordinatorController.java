package com.spms.backend.controller;

import com.spms.backend.dto.request.ScheduleUpdateRequest;
import com.spms.backend.dto.response.ScheduleResponse;
import com.spms.backend.dto.response.SystemAlertListResponse;
import com.spms.backend.dto.response.SystemAlertResponse;
import com.spms.backend.service.AdvisorDeadlineDisbandService;
import com.spms.backend.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Coordinator")
@RestController
@RequestMapping("/api/v1/coordinator")
public class CoordinatorController {

    private final ScheduleService scheduleService;
    private final com.spms.backend.service.GroupService groupService;
    private final AdvisorDeadlineDisbandService advisorDeadlineDisbandService;

    public CoordinatorController(ScheduleService scheduleService,
                                  com.spms.backend.service.GroupService groupService,
                                  AdvisorDeadlineDisbandService advisorDeadlineDisbandService) {
        this.scheduleService = scheduleService;
        this.groupService = groupService;
        this.advisorDeadlineDisbandService = advisorDeadlineDisbandService;
    }

    // ── GET /api/v1/coordinator/schedule ────────────────────────────────

    @Operation(summary = "Get current deadline schedule (D11)")
    @GetMapping("/schedule")
    public ResponseEntity<ScheduleResponse> getSchedule(HttpServletRequest httpReq) {
        String role = (String) httpReq.getAttribute("jwt_role");
        if (!"coordinator".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        ScheduleResponse response = scheduleService.getLatestSchedule();
        return ResponseEntity.ok(response);
    }

    // ── PUT /api/v1/coordinator/schedule ────────────────────────────────

    @Operation(summary = "Set or update group formation and advisor assignment deadlines")
    @PutMapping("/schedule")
    public ResponseEntity<ScheduleResponse> updateSchedule(
            @Valid @RequestBody ScheduleUpdateRequest req,
            HttpServletRequest httpReq) {

        String role = (String) httpReq.getAttribute("jwt_role");
        if (!"coordinator".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Long userId = ((Number) httpReq.getAttribute("jwt_userId")).longValue();
        ScheduleResponse response = scheduleService.updateSchedule(req, userId);
        return ResponseEntity.ok(response);
    }

    // ── GET /api/v1/coordinator/system-alerts ───────────────────────────

    @Operation(summary = "Get system alerts for the coordinator (coord_f5 — D10)")
    @GetMapping("/system-alerts")
    public ResponseEntity<SystemAlertListResponse> getSystemAlerts(HttpServletRequest httpReq) {

        String role = (String) httpReq.getAttribute("jwt_role");
        if (!"coordinator".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Long userId = ((Number) httpReq.getAttribute("jwt_userId")).longValue();
        List<SystemAlertResponse> alerts = scheduleService.getSystemAlerts(userId);

        return ResponseEntity.ok(new SystemAlertListResponse(
                "System alerts retrieved successfully.",
                alerts.size(),
                alerts
        ));
    }

    // C-4: POST /api/v1/coordinator/groups/disband-unassigned (manual trigger for P4.9)
    @Operation(summary = "Manually disband groups with no advisor after deadline")
    @PostMapping("/groups/disband-unassigned")
    public ResponseEntity<?> disbandUnassignedGroups(HttpServletRequest httpReq) {
        String role = (String) httpReq.getAttribute("jwt_role");
        if (!"coordinator".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        advisorDeadlineDisbandService.disbandUnassignedGroups();
        return ResponseEntity.ok(java.util.Map.of("status", "success", "message", "Disband operation completed."));
    }

    @Operation(summary = "Get detailed group formation report")
    @GetMapping("/reports/group-formation")
    public ResponseEntity<com.spms.backend.dto.response.GroupFormationReportDto> getGroupFormationReport(HttpServletRequest httpReq) {
        String role = (String) httpReq.getAttribute("jwt_role");
        if (!"coordinator".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(groupService.getGroupFormationReport(role));
    }
}
