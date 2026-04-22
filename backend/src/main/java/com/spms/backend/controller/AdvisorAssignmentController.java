package com.spms.backend.controller;

import com.spms.backend.dto.request.OverrideAssignmentRequest;
import com.spms.backend.dto.response.OverrideAssignmentResponse;
import com.spms.backend.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "4.5/4.7 Advisor Assignments")
@RestController
@RequestMapping("/api/v1/advisor-assignments")
public class AdvisorAssignmentController {

    private final GroupService groupService;

    public AdvisorAssignmentController(GroupService groupService) {
        this.groupService = groupService;
    }

    @Operation(summary = "Coordinator override assignment")
    @PostMapping("/override")
    public ResponseEntity<OverrideAssignmentResponse> overrideAssignment(
            @Valid @RequestBody OverrideAssignmentRequest request,
            HttpServletRequest httpReq) {

        String role = (String) httpReq.getAttribute("jwt_role");
        Long userId = ((Number) httpReq.getAttribute("jwt_userId")).longValue();

        OverrideAssignmentResponse response = groupService.overrideAdvisorAssignment(request, userId, role);

        return ResponseEntity.ok(response);
    }
}
