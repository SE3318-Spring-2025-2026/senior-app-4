package com.spms.backend.controller;

import com.spms.backend.dto.request.ProfessorRegisterRequest;
import com.spms.backend.dto.response.ProfessorRegisterResponse;
import com.spms.backend.service.ProfessorRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spms.backend.service.GroupService;
import com.spms.backend.dto.response.AdvisorRequestResponseDto;
import com.spms.backend.dto.request.AdvisorRequestDecisionDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import java.util.List;

@Tag(name = "Professor Management")
@RestController
@RequestMapping("/api/v1/professors")
public class ProfessorController {

    private final ProfessorRegistrationService professorRegistrationService;
    private final GroupService groupService;

    public ProfessorController(ProfessorRegistrationService professorRegistrationService, GroupService groupService) {
        this.professorRegistrationService = professorRegistrationService;
        this.groupService = groupService;
    }

    @Operation(summary = "Register a new professor or coordinator")
    @PostMapping("/register")
    public ResponseEntity<ProfessorRegisterResponse> registerProfessor(
            @Valid @RequestBody ProfessorRegisterRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(professorRegistrationService.registerProfessor(request));
    }

    @Operation(summary = "Get pending advisor requests for the authenticated professor")
    @GetMapping("/advisor-requests")
    public ResponseEntity<List<AdvisorRequestResponseDto>> getAdvisorRequests(
            @RequestAttribute("jwt_userId") Object userId) {
        Long professorId = Long.valueOf(userId.toString());
        return ResponseEntity.ok(groupService.getPendingAdvisorRequests(professorId));
    }

    @Operation(summary = "Approve or reject an advisor request")
    @PatchMapping("/advisor-requests")
    public ResponseEntity<Void> handleAdvisorRequestDecision(
            @Valid @RequestBody AdvisorRequestDecisionDto request,
            @RequestAttribute("jwt_userId") Object userId) {
        Long professorId = Long.valueOf(userId.toString());
        groupService.handleAdvisorRequestDecision(professorId, request.groupId(), request.status());
        return ResponseEntity.ok().build();
    }
}
