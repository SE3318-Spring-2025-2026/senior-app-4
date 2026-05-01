package com.spms.backend.controller;

import com.spms.backend.dto.request.AssignmentStatusUpdateRequest;
import com.spms.backend.dto.request.GroupAssignmentRequest;
import com.spms.backend.dto.response.DeleteResponse;
import com.spms.backend.dto.response.GroupAssignmentListResponse;
import com.spms.backend.dto.response.GroupAssignmentResponse;
import com.spms.backend.service.GroupCommitteeAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Process 5.4 — Assign Groups to Committee.
 */
@Tag(name = "5.4 Committee Group Assignments")
@RestController
@RequestMapping("/api/v1/committees")
public class GroupCommitteeAssignmentController {

    private final GroupCommitteeAssignmentService service;

    public GroupCommitteeAssignmentController(GroupCommitteeAssignmentService service) {
        this.service = service;
    }

    @Operation(summary = "Assign a group to a committee (P5.4)")
    @PostMapping("/{committeeId}/groups")
    public ResponseEntity<GroupAssignmentResponse> assignGroup(
            @PathVariable Long committeeId,
            @Valid @RequestBody GroupAssignmentRequest request,
            HttpServletRequest httpReq) {

        Long actorId = resolveUserId(httpReq);
        GroupAssignmentResponse response = service.assignGroup(committeeId, request, actorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "List groups assigned to a committee")
    @GetMapping("/{committeeId}/groups")
    public ResponseEntity<GroupAssignmentListResponse> listGroupsForCommittee(
            @PathVariable Long committeeId) {
        List<GroupAssignmentResponse> items = service.listGroupsForCommittee(committeeId);
        return ResponseEntity.ok(new GroupAssignmentListResponse(items.size(), items));
    }

    @Operation(summary = "List committees a group is assigned to")
    @GetMapping("/groups/{groupId}/committees")
    public ResponseEntity<GroupAssignmentListResponse> listCommitteesForGroup(
            @PathVariable Long groupId) {
        List<GroupAssignmentResponse> items = service.listCommitteesForGroup(groupId);
        return ResponseEntity.ok(new GroupAssignmentListResponse(items.size(), items));
    }

    @Operation(summary = "Update assignment status (ASSIGNED → SCHEDULED → COMPLETED/CANCELLED)")
    @PatchMapping("/{committeeId}/groups/{assignmentId}/status")
    public ResponseEntity<GroupAssignmentResponse> updateStatus(
            @PathVariable Long committeeId,
            @PathVariable Long assignmentId,
            @Valid @RequestBody AssignmentStatusUpdateRequest request,
            HttpServletRequest httpReq) {

        Long actorId = resolveUserId(httpReq);
        GroupAssignmentResponse response = service.updateStatus(committeeId, assignmentId, request, actorId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Remove a group from a committee")
    @DeleteMapping("/{committeeId}/groups/{assignmentId}")
    public ResponseEntity<DeleteResponse> deleteAssignment(
            @PathVariable Long committeeId,
            @PathVariable Long assignmentId,
            HttpServletRequest httpReq) {

        Long actorId = resolveUserId(httpReq);
        DeleteResponse response = service.deleteAssignment(committeeId, assignmentId, actorId);
        return ResponseEntity.ok(response);
    }

    private Long resolveUserId(HttpServletRequest req) {
        Object value = req.getAttribute("jwt_userId");
        if (value instanceof Number n) {
            return n.longValue();
        }
        return value != null ? Long.valueOf(value.toString()) : null;
    }
}
