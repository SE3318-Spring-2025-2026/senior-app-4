package com.spms.backend.controller;

import com.spms.backend.dto.response.AdvisorRequestDetailDto;
import com.spms.backend.service.AdvisorRequestDetailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/advisor-requests")
public class AdvisorRequestController {

    private final AdvisorRequestDetailService advisorRequestDetailService;

    public AdvisorRequestController(AdvisorRequestDetailService advisorRequestDetailService) {
        this.advisorRequestDetailService = advisorRequestDetailService;
    }

    // [P4-DETAIL-1] GET /api/v1/advisor-requests/{requestId}
    @GetMapping("/{requestId}")
    public ResponseEntity<Map<String, Object>> getAdvisorRequestDetail(
            @PathVariable Long requestId,
            @RequestAttribute("jwt_userId") Object userId,
            @RequestAttribute("jwt_role") Object role) {

        Long callerId = Long.valueOf(userId.toString());
        String callerRole = role != null ? role.toString() : "";

        // TODO(P2-dep:#14-22): delegates to MockAdvisorRequestDetailService until P2 advisor-request workflow merges.
        //   After merge, delete the mock and this TODO. See DEPENDENCIES.md.
        AdvisorRequestDetailDto detail = advisorRequestDetailService.getDetail(requestId, callerId, callerRole);

        return ResponseEntity.ok(Map.of("status", "success", "data", detail));
    }
}
