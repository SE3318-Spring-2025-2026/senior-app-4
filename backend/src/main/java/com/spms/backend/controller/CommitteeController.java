package com.spms.backend.controller;

import com.spms.backend.dto.request.AdvisorAssignmentRequest;
import com.spms.backend.dto.request.CommitteeCreateRequest;
import com.spms.backend.model.Committee;
import com.spms.backend.service.AdvisorAssignmentService;
import com.spms.backend.service.CommitteeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/committees")
public class CommitteeController {

    private final CommitteeService committeeService;
    private final AdvisorAssignmentService advisorAssignmentService;

    public CommitteeController(CommitteeService committeeService, AdvisorAssignmentService advisorAssignmentService) {
        this.committeeService = committeeService;
        this.advisorAssignmentService = advisorAssignmentService;
    }

    @PostMapping
    public ResponseEntity<Committee> createCommittee(
            @Valid @RequestBody CommitteeCreateRequest request,
            HttpServletRequest httpReq) {
        Long userId = ((Number) httpReq.getAttribute("jwt_userId")).longValue();
        Committee created = committeeService.createCommittee(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<Committee>> getAllCommittees() {
        return ResponseEntity.ok(committeeService.getAllCommittees());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Committee> getCommitteeById(@PathVariable Long id) {
        return ResponseEntity.ok(committeeService.getCommitteeById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Committee> updateCommittee(
            @PathVariable Long id,
            @Valid @RequestBody CommitteeCreateRequest request,
            HttpServletRequest httpReq) {
        Long userId = ((Number) httpReq.getAttribute("jwt_userId")).longValue();
        Committee updated = committeeService.updateCommittee(id, request, userId);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCommittee(@PathVariable Long id, HttpServletRequest httpReq) {
        Long userId = ((Number) httpReq.getAttribute("jwt_userId")).longValue();
        committeeService.deleteCommittee(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/advisors")
    public ResponseEntity<Map<String, String>> assignAdvisor(
            @PathVariable Long id,
            @Valid @RequestBody AdvisorAssignmentRequest request,
            HttpServletRequest httpReq) {
        Long userId = ((Number) httpReq.getAttribute("jwt_userId")).longValue();
        advisorAssignmentService.assignAdvisor(id, request.getAdvisorId(), request.getRole(), userId);
        return ResponseEntity.ok(Map.of("message", "Advisor assigned successfully"));
    }

    @DeleteMapping("/{id}/advisors/{advisorId}")
    public ResponseEntity<Void> removeAdvisor(@PathVariable Long id, @PathVariable Long advisorId) {
        advisorAssignmentService.removeAdvisor(id, advisorId);
        return ResponseEntity.noContent().build();
    }
}
