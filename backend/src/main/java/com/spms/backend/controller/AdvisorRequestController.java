package com.spms.backend.controller;

import com.spms.backend.dto.request.AdvisorDecisionRequestDto;
import com.spms.backend.dto.response.AdvisorDecisionResponseDto;
import com.spms.backend.dto.response.AdvisorRequestDetailDto;
import com.spms.backend.service.AdvisorRequestDetailService;
import com.spms.backend.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Advisor Requests")
@RestController
@RequestMapping("/api/v1/advisor-requests")
public class AdvisorRequestController {

    private final GroupService groupService;
    private final AdvisorRequestDetailService advisorRequestDetailService;

    public AdvisorRequestController(GroupService groupService, AdvisorRequestDetailService advisorRequestDetailService) {
        this.groupService = groupService;
        this.advisorRequestDetailService = advisorRequestDetailService;
    }

    @Operation(summary = "Process an advisor request decision (Approve/Reject)")
    @PostMapping("/{requestId}/decision")
    public ResponseEntity<AdvisorDecisionResponseDto> processAdvisorRequestDecision(
            @PathVariable Long requestId,
            @Valid @RequestBody AdvisorDecisionRequestDto request,
            @RequestAttribute("jwt_userId") Object userId) {
        Long professorId = Long.valueOf(userId.toString());
        AdvisorDecisionResponseDto response = groupService.processAdvisorRequestDecision(professorId, requestId, request.decision(), request.reason());
        return ResponseEntity.ok(response);
    }

    // [P4-DETAIL-1] GET /api/v1/advisor-requests/{requestId}
    @GetMapping("/{requestId}")
    public ResponseEntity<Map<String, Object>> getAdvisorRequestDetail(
            @PathVariable Long requestId,
            @RequestAttribute("jwt_userId") Object userId,
            @RequestAttribute("jwt_role") Object role) {

        Long callerId = Long.valueOf(userId.toString());
        String callerRole = role != null ? role.toString() : "";

        AdvisorRequestDetailDto detail = advisorRequestDetailService.getDetail(requestId, callerId, callerRole);

        return ResponseEntity.ok(Map.of("status", "success", "data", detail));
    }
}
