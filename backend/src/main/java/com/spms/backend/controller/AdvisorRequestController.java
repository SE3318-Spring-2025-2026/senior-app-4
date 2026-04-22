package com.spms.backend.controller;

import com.spms.backend.dto.request.AdvisorDecisionRequestDto;
import com.spms.backend.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Advisor Requests")
@RestController
@RequestMapping("/api/v1/advisor-requests")
public class AdvisorRequestController {

    private final GroupService groupService;

    public AdvisorRequestController(GroupService groupService) {
        this.groupService = groupService;
    }

    @Operation(summary = "Process an advisor request decision (Approve/Reject)")
    @PostMapping("/{requestId}/decision")
    public ResponseEntity<Void> processAdvisorRequestDecision(
            @PathVariable Long requestId,
            @Valid @RequestBody AdvisorDecisionRequestDto request,
            @RequestAttribute("jwt_userId") Object userId) {
        Long professorId = Long.valueOf(userId.toString());
        groupService.processAdvisorRequestDecision(professorId, requestId, request.status(), request.reason());
        return ResponseEntity.ok().build();
    }
}
