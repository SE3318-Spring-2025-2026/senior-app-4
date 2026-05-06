package com.spms.backend.controller;

import com.spms.backend.dto.response.GroupSprintSummaryResponse;
import com.spms.backend.dto.response.GroupTrackingDetailResponse;
import com.spms.backend.model.Group;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.service.AdvisorSprintService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/advisor")
public class AdvisorSprintController {

    private final GroupRepository groupRepository;
    private final AdvisorSprintService advisorSprintService;

    public AdvisorSprintController(GroupRepository groupRepository, AdvisorSprintService advisorSprintService) {
        this.groupRepository = groupRepository;
        this.advisorSprintService = advisorSprintService;
    }

    /**
     * Get active sprint summary with all groups for advisor
     */
    @GetMapping("/sprint-summary")
    public ResponseEntity<GroupSprintSummaryResponse> getSprintSummary(HttpServletRequest request) {
        Long advisorId = ((Number) request.getAttribute("jwt_userId")).longValue();

        List<Group> advisorGroups = groupRepository.findByAdvisorId(advisorId);
        GroupSprintSummaryResponse response = advisorSprintService.buildSprintSummary(advisorGroups);

        return ResponseEntity.ok(response);
    }

    /**
     * Get detailed tracking for a specific group
     */
    @GetMapping("/groups/{groupId}/sprint-tracking")
    public ResponseEntity<GroupTrackingDetailResponse> getGroupTrackingDetails(
            @PathVariable Long groupId,
            HttpServletRequest request) {

        Long advisorId = ((Number) request.getAttribute("jwt_userId")).longValue();

        Optional<Group> groupOpt = groupRepository.findById(groupId);
        if (groupOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Group group = groupOpt.get();
        if (!advisorId.equals(group.getAdvisor() != null ? group.getAdvisor().getUserId() : null)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        GroupTrackingDetailResponse response = advisorSprintService.buildGroupTrackingDetail(group);

        return ResponseEntity.ok(response);
    }
}
