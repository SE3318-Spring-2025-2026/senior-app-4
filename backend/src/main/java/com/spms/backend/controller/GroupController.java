package com.spms.backend.controller;

import com.spms.backend.dto.request.GithubBindingRequest;
import com.spms.backend.dto.request.GroupCreateRequestDto;
import com.spms.backend.dto.request.GroupUpdateRequestDto;
import com.spms.backend.dto.request.InviteMemberRequestDto;
import com.spms.backend.dto.request.JiraBindingRequest;
import com.spms.backend.dto.response.GroupDetailDto;
import com.spms.backend.dto.response.GroupResponseDto;
import com.spms.backend.dto.response.IntegrationsTestResponse;
import com.spms.backend.dto.response.JiraIntegrationResponse;
import com.spms.backend.dto.response.GithubIntegrationResponse;
import com.spms.backend.dto.response.AuditLogResponseDto;
import com.spms.backend.repository.AuditLogRepository;
import com.spms.backend.service.GroupService;
import com.spms.backend.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springdoc.core.annotations.ParameterObject;
import com.spms.backend.dto.response.MemberResponseDto;
import java.util.List;


@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {

    private final GroupService groupService;
    private final MemberService memberService;
    private final AuditLogRepository auditLogRepository;

    public GroupController(GroupService groupService, MemberService memberService, AuditLogRepository auditLogRepository) {
        this.groupService = groupService;
        this.memberService = memberService;
        this.auditLogRepository = auditLogRepository;
    }

    @PostMapping
    public ResponseEntity<GroupResponseDto> createGroup(
            @Valid @RequestBody GroupCreateRequestDto request,
            @RequestAttribute("jwt_userId") Object userId) {

        Long creatorId = Long.valueOf(userId.toString());
        GroupResponseDto response = groupService.createGroup(request, creatorId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{groupId}/leave")
    public ResponseEntity<Void> leaveGroup(
            @PathVariable Long groupId,
            @RequestAttribute("jwt_userId") Object userId) {
        
        Long requesterId = Long.valueOf(userId.toString());
        groupService.leaveGroup(groupId, requesterId);
        
        return ResponseEntity.ok().build();
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
            @ParameterObject Pageable pageable,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String advisorAssigned,
            @RequestParam(required = false) String groupName,
            @RequestAttribute("jwt_userId") Object userId,
            @RequestAttribute("jwt_role") Object role) {

        Long requesterId = Long.valueOf(userId.toString());
        String requesterRole = role.toString();

        return ResponseEntity.ok(groupService.getGroups(pageable, requesterId, requesterRole, status, advisorAssigned, groupName));
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
            @PathVariable Long groupId) {
        return ResponseEntity.ok(groupService.getJiraIntegration(groupId));
    }

    @GetMapping("/{groupId}/integrations/github")
    public ResponseEntity<GithubIntegrationResponse> getGithubIntegration(
            @PathVariable Long groupId) {
        return ResponseEntity.ok(groupService.getGithubIntegration(groupId));
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

    @GetMapping("/{groupId}/members")
    public ResponseEntity<List<MemberResponseDto>> getGroupMembers(@PathVariable Long groupId) {
    List<MemberResponseDto> members = groupService.getGroupMembers(groupId);
    return ResponseEntity.ok(members);
    }

    @DeleteMapping("/{groupId}/members/{studentId}")
    public ResponseEntity<Void> removeMember(
        @PathVariable Long groupId,
        @PathVariable String studentId) {
        groupService.removeMember(groupId, studentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{groupId}/members/{studentId}")
    public ResponseEntity<Void> addMember(
        @PathVariable Long groupId,
        @PathVariable String studentId) {
        groupService.addMember(groupId, studentId);
        return ResponseEntity.ok().build();
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
    // ==============================================================================
    // GITHUB ENTEGRASYON UÇ NOKTALARI (ISSUE #...)
    // ==============================================================================

    @PostMapping("/{groupId}/integrations/github")
    public ResponseEntity<Void> bindGithubIntegration(
            @PathVariable Long groupId,
            @RequestAttribute("jwt_userId") Long requesterId, 
            @Valid @RequestBody GithubBindingRequest request) {
        groupService.bindGithubIntegration(groupId, requesterId, request);
        return ResponseEntity.ok().build();
    }



    @DeleteMapping("/{groupId}/integrations/github")
    public ResponseEntity<Void> unbindGithubIntegration(
            @PathVariable Long groupId,
            @RequestAttribute("jwt_userId") Long requesterId) {
        groupService.unbindGithubIntegration(groupId, requesterId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{groupId}/integrations/test")
    public ResponseEntity<IntegrationsTestResponse> testIntegrations(
            @PathVariable Long groupId,
            @RequestAttribute("jwt_userId") Long requesterId) {
        return ResponseEntity.ok(groupService.testIntegrations(groupId, requesterId));}

    @PostMapping("/{groupId}/advisor/transfer")
    public ResponseEntity<?> transferAdvisor(
            @PathVariable Long groupId,
            @Valid @RequestBody com.spms.backend.dto.request.AdvisorTransferRequestDto request,
            @RequestAttribute("jwt_role") Object role) {
        
        String requesterRole = role.toString();
        groupService.transferAdvisor(groupId, request.professorId(), requesterRole);

        return ResponseEntity.ok().body(java.util.Map.of(
                "success", true,
                "message", "Advisor assigned successfully"
        ));
    }

    @GetMapping("/{groupId}/logs")
    public ResponseEntity<Page<AuditLogResponseDto>> getGroupLogs(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        Page<AuditLogResponseDto> responsePage = auditLogRepository.findByGroupId(groupId, pageable)
            .map(log -> new AuditLogResponseDto(
                log.getId(),
                log.getUserId(),
                log.getActionType(),
                log.getEventDetails(),
                log.getGroupId(),
                log.getCommitteeId(),
                log.getIpAddress(),
                log.getCreatedAt()
            ));

        return ResponseEntity.ok(responsePage);
    }
}
