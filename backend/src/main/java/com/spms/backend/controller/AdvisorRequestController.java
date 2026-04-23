package com.spms.backend.controller;

import com.spms.backend.dto.response.AdvisorRequestListResponseDto;
import com.spms.backend.dto.response.AdvisorRequestSummaryDto;
import com.spms.backend.service.AdvisorRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/advisor-requests")
public class AdvisorRequestController {

    private final AdvisorRequestService advisorRequestService;

    public AdvisorRequestController(AdvisorRequestService advisorRequestService) {
        this.advisorRequestService = advisorRequestService;
    }

    @GetMapping
    public ResponseEntity<AdvisorRequestListResponseDto> listAdvisorRequests(
            @RequestAttribute("jwt_userId") Object userIdObj,
            @RequestAttribute("jwt_role") Object roleObj,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) Long professorId) {

        Long userId = Long.valueOf(userIdObj.toString());
        String role = roleObj.toString();

        List<AdvisorRequestSummaryDto> requests = advisorRequestService.listAdvisorRequests(userId, role, status, teamId, professorId);

        return ResponseEntity.ok(new AdvisorRequestListResponseDto("success", requests));
    }
}
