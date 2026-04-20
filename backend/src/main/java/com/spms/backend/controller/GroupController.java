package com.spms.backend.controller;


import com.spms.backend.dto.request.GroupCreateRequestDto;
import com.spms.backend.dto.request.GroupUpdateRequestDto;
import com.spms.backend.dto.request.InviteMemberRequestDto;
import com.spms.backend.dto.request.JiraBindingRequest;
import com.spms.backend.dto.response.GroupDetailDto;
import com.spms.backend.dto.response.GroupResponseDto;
import com.spms.backend.dto.response.JiraIntegrationResponse;
import com.spms.backend.service.GroupService;
import com.spms.backend.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {

    private final GroupService groupService;
    private final MemberService memberService;

    public GroupController(GroupService groupService, MemberService memberService) {
        this.groupService = groupService;
        this.memberService = memberService;
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
    public ResponseEntity<Page<GroupResponseDto>> getGroups(Pageable pageable) {
        return ResponseEntity.ok(groupService.getAllGroups(pageable));
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupDetailDto> getGroupDetails(@PathVariable Long groupId) {
        return ResponseEntity.ok(groupService.getGroupDetails(groupId));
    }


    @PostMapping("/{groupId}/integrations/jira")
    public ResponseEntity<?> bindJiraIntegration(
            @PathVariable Long groupId,
            @Valid @RequestBody JiraBindingRequest request,
            @RequestAttribute("jwt_userId") Object userId) {

        Long requesterId = Long.valueOf(userId.toString());
        groupService.bindJiraIntegration(groupId, requesterId, request);

        return ResponseEntity.ok().body(java.util.Map.of(
                "success", true,
                "message", "JIRA space bound successfully"
        ));
    }

    @GetMapping("/{groupId}/integrations/jira")
    public ResponseEntity<JiraIntegrationResponse> getJiraIntegration(
            @PathVariable Long groupId,
            @RequestAttribute("jwt_userId") Object userId) {
        Long requesterId = Long.valueOf(userId.toString());
        return ResponseEntity.ok(groupService.getJiraIntegration(groupId, requesterId));
    }

    @DeleteMapping("/{groupId}/integrations/jira")
    public ResponseEntity<?> unbindJiraIntegration(
            @PathVariable Long groupId,
            @RequestAttribute("jwt_userId") Object userId) {
        Long requesterId = Long.valueOf(userId.toString());
        groupService.unbindJiraIntegration(groupId, requesterId);

        return ResponseEntity.ok().body(java.util.Map.of(
                "success", true,
                "message", "JIRA integration removed successfully"
        ));
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

    /**
     * P2-API-05: Leader invites a student to the group.
     * Creates a MEMBERSHIP_INVITE notification for the target student (ns_f1).
     */
    @PostMapping("/{groupId}/members")
    public ResponseEntity<?> inviteMember(
            @PathVariable Long groupId,
            @Valid @RequestBody InviteMemberRequestDto request,
            @RequestAttribute("jwt_userId") Object userId) {

        Long leaderId = Long.valueOf(userId.toString());
        memberService.inviteMember(groupId, request.studentId(), leaderId);

        return ResponseEntity.status(HttpStatus.CREATED).body(java.util.Map.of(
                "success", true,
                "message", "Membership invitation sent successfully"
        ));
    }
}
