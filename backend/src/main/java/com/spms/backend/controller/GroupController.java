package com.spms.backend.controller;

import com.spms.backend.dto.request.GroupCreateRequestDto;
import com.spms.backend.dto.request.GroupUpdateRequestDto;
import com.spms.backend.dto.response.GroupDetailDto;
import com.spms.backend.dto.response.GroupResponseDto;
import com.spms.backend.service.GroupService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springdoc.core.annotations.ParameterObject;

@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    public ResponseEntity<GroupResponseDto> createGroup(
            @Valid @RequestBody GroupCreateRequestDto request,
            @RequestAttribute("jwt_userId") Object userId) {

        Long creatorId = Long.valueOf(userId.toString());
        GroupResponseDto response = groupService.createGroup(request, creatorId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{groupId}")
    public ResponseEntity<Void> updateGroup(
            @PathVariable Long groupId,
            @Valid @RequestBody GroupUpdateRequestDto request,
            @RequestAttribute("jwt_userId") Object userId) {

        Long requesterId = Long.valueOf(userId.toString());
        groupService.updateGroupName(groupId, request, requesterId);

        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<Page<GroupResponseDto>> getGroups(
            @ParameterObject Pageable pageable,  // <-- SADECE BURAYA @ParameterObject EKLENDİ
            @RequestAttribute("jwt_userId") Object userId,
            @RequestAttribute("jwt_role") Object role) {

        Long requesterId = Long.valueOf(userId.toString());
        String requesterRole = role.toString();

        return ResponseEntity.ok(groupService.getGroups(pageable, requesterId, requesterRole));
    }

    
    @GetMapping("/{groupId}")
    public ResponseEntity<GroupDetailDto> getGroupDetails(
            @PathVariable Long groupId,
            @RequestAttribute("jwt_userId") Object userId,
            @RequestAttribute("jwt_role") Object role) {

        Long requesterId = Long.valueOf(userId.toString());
        String requesterRole = role.toString();

        return ResponseEntity.ok(groupService.getGroupDetails(groupId, requesterId, requesterRole));
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<?> disbandGroup(
            @PathVariable Long groupId,
            @RequestAttribute("jwt_userId") Object userId,
            @RequestAttribute("jwt_role") Object role) {

        Long requesterId = Long.valueOf(userId.toString());
        String requesterRole = role.toString();
        groupService.disbandGroup(groupId, requesterId, requesterRole);

        return ResponseEntity.ok().body(java.util.Map.of(
                "success", true,
                "message", "Group disbanded successfully"
        ));
    }
}