package com.spms.backend.controller;

import com.spms.backend.dto.response.AdvisorAssignmentListResponse;
import com.spms.backend.service.AdvisorAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Process 4 — advisor assignments (P4-ASSIGN-1 list, release advisee group).
 */
@Tag(name = "Advisor assignments")
@RestController
@RequestMapping("/api/v1/advisor-assignments")
public class AdvisorAssignmentController {

    private final AdvisorAssignmentService advisorAssignmentService;

    public AdvisorAssignmentController(AdvisorAssignmentService advisorAssignmentService) {
        this.advisorAssignmentService = advisorAssignmentService;
    }

    @Operation(summary = "List group–advisor mappings (P4-ASSIGN-1 / #160)")
    @GetMapping
    public ResponseEntity<AdvisorAssignmentListResponse> listAdvisorAssignments(
            @RequestAttribute("jwt_userId") Object userId,
            @RequestAttribute(value = "jwt_role", required = false) Object role,
            @RequestParam(value = "advisorId", required = false) Long advisorId,
            @RequestParam(value = "hasAdvisor", required = false) Boolean hasAdvisor) {

        Long uid = Long.valueOf(userId.toString());
        String r = role != null ? role.toString() : "";
        AdvisorAssignmentListResponse body =
                advisorAssignmentService.listAdvisorAssignments(r, uid, advisorId, hasAdvisor);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/{groupId}/release")
    public ResponseEntity<Map<String, String>> releaseGroup(
            @PathVariable Long groupId,
            @RequestAttribute("jwt_userId") Object userId,
            @RequestAttribute("jwt_role") Object role) {

        Long professorId = Long.valueOf(userId.toString());
        String requesterRole = role != null ? role.toString() : null;

        advisorAssignmentService.releaseAdvisor(groupId, professorId, requesterRole);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Group released successfully. Group leader notified."
        ));
    }
}
