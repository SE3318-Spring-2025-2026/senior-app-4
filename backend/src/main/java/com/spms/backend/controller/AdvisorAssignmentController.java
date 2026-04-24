package com.spms.backend.controller;

import com.spms.backend.service.AdvisorAssignmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/advisor-assignments")
public class AdvisorAssignmentController {

    private final AdvisorAssignmentService advisorAssignmentService;

    public AdvisorAssignmentController(AdvisorAssignmentService advisorAssignmentService) {
        this.advisorAssignmentService = advisorAssignmentService;
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
