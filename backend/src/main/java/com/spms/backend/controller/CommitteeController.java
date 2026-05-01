package com.spms.backend.controller;

import com.spms.backend.dto.request.AdvisorAssignmentRequest;
import com.spms.backend.dto.request.CommitteeCreateRequest;
import com.spms.backend.dto.request.JuryAssignmentRequest;
import com.spms.backend.dto.response.AdvisorListResponse;
import com.spms.backend.dto.response.JuryListResponse;
import com.spms.backend.exception.ForbiddenException;
import com.spms.backend.model.Committee;
import com.spms.backend.service.AdvisorAssignmentService;
import com.spms.backend.service.CommitteeService;
import com.spms.backend.service.JuryAssignmentService;
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
    private final JuryAssignmentService juryAssignmentService;

    public CommitteeController(CommitteeService committeeService,
                              AdvisorAssignmentService advisorAssignmentService,
                              JuryAssignmentService juryAssignmentService) {
        this.committeeService = committeeService;
        this.advisorAssignmentService = advisorAssignmentService;
        this.juryAssignmentService = juryAssignmentService;
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
        Object role = httpReq.getAttribute("jwt_role");
        if (role == null || !"coordinator".equalsIgnoreCase(role.toString())) {
            throw new ForbiddenException("Only coordinators can assign advisors.");
        }
        Long userId = ((Number) httpReq.getAttribute("jwt_userId")).longValue();
        advisorAssignmentService.assignAdvisor(id, request.getAdvisorId(), request.getRole(), userId);
        return ResponseEntity.ok(Map.of("message", "Advisor assigned successfully"));
    }

    @GetMapping("/{id}/advisors")
    public ResponseEntity<AdvisorListResponse> getAdvisorsByCommittee(@PathVariable Long id) {
        Committee committee = committeeService.getCommitteeById(id);
        List<com.spms.backend.dto.response.AdvisorDto> advisors = committee.getAdvisors().stream()
                .map(ca -> new com.spms.backend.dto.response.AdvisorDto(
                        ca.getCommitteeAdvisorId(),
                        ca.getAdvisor().getUserId(),
                        ca.getAdvisor().getFullName(),
                        ca.getRole(),
                        ca.getAssignedAt()))
                .toList();
        return ResponseEntity.ok(new AdvisorListResponse("success", advisors));
    }

    @DeleteMapping("/{id}/advisors/{committeeAdvisorId}")
    public ResponseEntity<Void> removeAdvisor(@PathVariable Long id, @PathVariable Long committeeAdvisorId,
                                              HttpServletRequest httpReq) {
        Object role = httpReq.getAttribute("jwt_role");
        if (role == null || !"coordinator".equalsIgnoreCase(role.toString())) {
            throw new ForbiddenException("Only coordinators can remove advisors.");
        }
        advisorAssignmentService.removeAdvisor(id, committeeAdvisorId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/jury")
    public ResponseEntity<Map<String, String>> assignJury(
            @PathVariable Long id,
            @Valid @RequestBody JuryAssignmentRequest request,
            HttpServletRequest httpReq) {
        Object role = httpReq.getAttribute("jwt_role");
        if (role == null || !"coordinator".equalsIgnoreCase(role.toString())) {
            throw new ForbiddenException("Only coordinators can assign jury members.");
        }
        Long userId = ((Number) httpReq.getAttribute("jwt_userId")).longValue();
        juryAssignmentService.assignJury(id, request.getJuryMemberId(), request.getJuryType(), userId);
        return ResponseEntity.ok(Map.of("message", "Jury member assigned successfully"));
    }

    @GetMapping("/{id}/jury")
    public ResponseEntity<JuryListResponse> getJuryByCommittee(@PathVariable Long id) {
        return ResponseEntity.ok(juryAssignmentService.getJuryByCommittee(id));
    }

    @DeleteMapping("/{id}/jury/{juryMemberId}")
    public ResponseEntity<Void> removeJury(@PathVariable Long id, @PathVariable Long juryMemberId,
                                          HttpServletRequest httpReq) {
        Object role = httpReq.getAttribute("jwt_role");
        if (role == null || !"coordinator".equalsIgnoreCase(role.toString())) {
            throw new ForbiddenException("Only coordinators can remove jury members.");
        }
        juryAssignmentService.removeJury(id, juryMemberId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/jury/{juryMemberId}/committees")
    public ResponseEntity<JuryListResponse> getCommitteesByJury(@PathVariable Long juryMemberId) {
        return ResponseEntity.ok(juryAssignmentService.getCommitteesByJury(juryMemberId));
    }
}
