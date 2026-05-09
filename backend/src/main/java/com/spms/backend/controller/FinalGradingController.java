package com.spms.backend.controller;

import com.spms.backend.dto.request.SprintAdvisorGradeRequest;
import com.spms.backend.dto.request.SprintConfigRequest;
import com.spms.backend.dto.request.SprintDeliverableWeightsRequest;
import com.spms.backend.dto.response.FinalGradeResponse;
import com.spms.backend.dto.response.AiCodeReviewSuggestionResponse;
import com.spms.backend.dto.response.SprintConfigResponse;
import com.spms.backend.dto.response.SprintAdvisorGradeResponse;
import com.spms.backend.dto.response.SprintDeliverableWeightResponse;
import com.spms.backend.exception.ForbiddenException;
import com.spms.backend.exception.NotFoundException;
import com.spms.backend.model.Group;
import com.spms.backend.repository.GroupMemberRepository;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.service.FinalGradeCalculationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/v1/final-grading")
public class FinalGradingController {

    private final FinalGradeCalculationService finalGradeCalculationService;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;

    public FinalGradingController(FinalGradeCalculationService finalGradeCalculationService,
                                  GroupRepository groupRepository,
                                  GroupMemberRepository groupMemberRepository) {
        this.finalGradeCalculationService = finalGradeCalculationService;
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
    }

    @GetMapping("/sprints")
    public ResponseEntity<Map<String, Object>> listSprints() {
        List<SprintConfigResponse> sprints = finalGradeCalculationService.listSprints();
        return ResponseEntity.ok(Map.of("status", "success", "data", sprints));
    }

    @PostMapping("/sprints")
    public ResponseEntity<Map<String, Object>> createSprint(
            @Valid @RequestBody SprintConfigRequest request,
            @RequestAttribute("jwt_role") Object role) {
        requireCoordinator(role);
        SprintConfigResponse sprint = finalGradeCalculationService.createSprint(request);
        return ResponseEntity.status(201).body(Map.of("status", "success", "data", sprint));
    }

    @PutMapping("/sprints/{sprintId}")
    public ResponseEntity<Map<String, Object>> updateSprint(
            @PathVariable Long sprintId,
            @Valid @RequestBody SprintConfigRequest request,
            @RequestAttribute("jwt_role") Object role) {
        requireCoordinator(role);
        SprintConfigResponse sprint = finalGradeCalculationService.updateSprint(sprintId, request);
        return ResponseEntity.ok(Map.of("status", "success", "data", sprint));
    }

    @PutMapping("/sprint-deliverable-weights")
    public ResponseEntity<SprintDeliverableWeightResponse> replaceSprintDeliverableWeights(
            @Valid @RequestBody SprintDeliverableWeightsRequest request,
            @RequestAttribute("jwt_role") Object role) {
        requireCoordinator(role);
        return ResponseEntity.ok(finalGradeCalculationService.replaceSprintDeliverableWeights(request));
    }

    @GetMapping("/sprint-deliverable-weights")
    public ResponseEntity<SprintDeliverableWeightResponse> getSprintDeliverableWeights(
            @RequestAttribute("jwt_role") Object role) {
        requireCoordinatorOrProfessor(role);
        return ResponseEntity.ok(finalGradeCalculationService.getSprintDeliverableWeights());
    }

    @PostMapping("/sprint-advisor-grades")
    public ResponseEntity<Map<String, Object>> saveSprintAdvisorGrade(
            @Valid @RequestBody SprintAdvisorGradeRequest request,
            @RequestAttribute("jwt_userId") Object userId,
            @RequestAttribute("jwt_role") Object role) {
        Long callerId = Long.valueOf(userId.toString());
        requireAdvisorAccess(role, callerId, request.groupId());
        SprintAdvisorGradeResponse saved = finalGradeCalculationService.saveSprintAdvisorGrade(request, callerId);
        return ResponseEntity.ok(Map.of("status", "success", "data", saved));
    }

    @GetMapping("/groups/{groupId}/sprint-advisor-grades")
    public ResponseEntity<Map<String, Object>> getSprintAdvisorGrades(
            @PathVariable Long groupId,
            @RequestAttribute("jwt_userId") Object userId,
            @RequestAttribute("jwt_role") Object role) {
        requireAdvisorAccess(role, Long.valueOf(userId.toString()), groupId);
        List<SprintAdvisorGradeResponse> grades = finalGradeCalculationService.getSprintAdvisorGrades(groupId);
        return ResponseEntity.ok(Map.of("status", "success", "data", grades));
    }

    @PostMapping("/groups/{groupId}/demonstration-grade")
    public ResponseEntity<Map<String, Object>> saveDemonstrationGrade(
            @PathVariable Long groupId,
            @RequestBody Map<String, Double> body,
            @RequestAttribute("jwt_userId") Object userId,
            @RequestAttribute("jwt_role") Object role) {
        requireAdvisorAccess(role, Long.valueOf(userId.toString()), groupId);
        Double grade = body.get("grade");
        if (grade == null) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "grade is required"));
        }
        finalGradeCalculationService.saveDemonstrationGrade(groupId, grade);
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    @PostMapping("/groups/{groupId}/initialize-demonstration")
    public ResponseEntity<Map<String, Object>> initializeDemonstration(
            @PathVariable Long groupId,
            @RequestAttribute("jwt_userId") Object userId,
            @RequestAttribute("jwt_role") Object role) {
        requireAdvisorAccess(role, Long.valueOf(userId.toString()), groupId);
        Long submissionId = finalGradeCalculationService.initializeDemonstration(groupId);
        return ResponseEntity.ok(Map.of("status", "success", "data", Map.of("id", submissionId)));
    }

    @GetMapping("/groups/{groupId}/demonstration-grade")
    public ResponseEntity<Map<String, Object>> getDemonstrationGrade(
            @PathVariable Long groupId,
            @RequestAttribute("jwt_userId") Object userId,
            @RequestAttribute("jwt_role") Object role) {
        requireAdvisorAccess(role, Long.valueOf(userId.toString()), groupId);
        Double grade = finalGradeCalculationService.getDemonstrationGrade(groupId);
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("grade", grade);
        return ResponseEntity.ok(Map.of("status", "success", "data", data));
    }

    @GetMapping("/groups/{groupId}/sprints/{sprintId}/ai-code-review-suggestion")
    public ResponseEntity<AiCodeReviewSuggestionResponse> getAiCodeReviewSuggestion(
            @PathVariable Long groupId,
            @PathVariable Long sprintId,
            @RequestAttribute("jwt_userId") Object userId,
            @RequestAttribute("jwt_role") Object role) {
        requireAdvisorAccess(role, Long.valueOf(userId.toString()), groupId);
        return ResponseEntity.ok(finalGradeCalculationService.getAiCodeReviewSuggestion(groupId, sprintId));
    }

    @PostMapping("/groups/{groupId}/recalculate")
    public ResponseEntity<FinalGradeResponse> recalculateGroup(
            @PathVariable Long groupId,
            @RequestAttribute("jwt_userId") Object userId,
            @RequestAttribute("jwt_role") Object role) {
        requireAdvisorAccess(role, Long.valueOf(userId.toString()), groupId);
        return ResponseEntity.ok(finalGradeCalculationService.recalculateGroup(groupId));
    }

    @PostMapping("/groups/{groupId}/publish")
    public ResponseEntity<FinalGradeResponse> publishGroup(
            @PathVariable Long groupId,
            @RequestAttribute("jwt_role") Object role) {
        requireCoordinator(role);
        return ResponseEntity.ok(finalGradeCalculationService.publishGroup(groupId));
    }

    @GetMapping("/groups")
    public ResponseEntity<Map<String, Object>> listAllGroupGrades(
            @RequestAttribute("jwt_role") Object role) {
        requireCoordinator(role);
        return ResponseEntity.ok(Map.of("status", "success",
                "data", finalGradeCalculationService.listAllGroupGrades()));
    }

    @GetMapping("/groups/{groupId}")
    public ResponseEntity<FinalGradeResponse> getGroupFinalGrades(
            @PathVariable Long groupId,
            @RequestAttribute("jwt_userId") Object userId,
            @RequestAttribute("jwt_role") Object role) {
        Long callerId = Long.valueOf(userId.toString());
        String normalizedRole = normalizeRole(role);
        boolean includeUnpublished;

        if ("coordinator".equals(normalizedRole) || "admin".equals(normalizedRole)) {
            includeUnpublished = true;
        } else if (isProfessorRole(normalizedRole)) {
            requireAdvisorAccess(role, callerId, groupId);
            includeUnpublished = true;
        } else if ("student".equals(normalizedRole)) {
            requireStudentGroupAccess(callerId, groupId);
            includeUnpublished = false;
        } else {
            throw new ForbiddenException("Role is not allowed to view final grades.");
        }

        return ResponseEntity.ok(finalGradeCalculationService.getGroupFinalGrades(groupId, includeUnpublished));
    }

    private void requireCoordinator(Object role) {
        String normalized = normalizeRole(role);
        if (!"coordinator".equals(normalized)) {
            throw new ForbiddenException("Only coordinators can perform this grading action.");
        }
    }

    private void requireCoordinatorOrProfessor(Object role) {
        String normalized = normalizeRole(role);
        if (!"coordinator".equals(normalized) && !isProfessorRole(normalized)) {
            throw new ForbiddenException("Only coordinators and professors can access grading configuration.");
        }
    }

    private void requireAdvisorAccess(Object role, Long userId, Long groupId) {
        String normalized = normalizeRole(role);
        if ("coordinator".equals(normalized) || "admin".equals(normalized)) {
            return;
        }
        if (!isProfessorRole(normalized)) {
            throw new ForbiddenException("Only the group's advisor or a coordinator can perform this action.");
        }
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found: " + groupId));
        if (group.getAdvisor() == null || !userId.equals(group.getAdvisor().getUserId())) {
            String advisorName = group.getAdvisor() != null ? group.getAdvisor().getFullName() : "none";
            throw new ForbiddenException("Access denied. You are not the advisor of this group (Advisor: " + advisorName + ").");
        }
    }

    private void requireStudentGroupAccess(Long userId, Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found: " + groupId));
        if (group.getLeader() != null && userId.equals(group.getLeader().getUserId())) {
            return;
        }
        if (!groupMemberRepository.existsByGroup_IdAndUser_UserId(groupId, userId)) {
            throw new ForbiddenException("Student is not a member of this group.");
        }
    }

    private boolean isProfessorRole(String role) {
        return "professor".equals(role) || "advisor".equals(role);
    }

    private String normalizeRole(Object role) {
        return role == null ? "" : role.toString().trim().toLowerCase();
    }
}
