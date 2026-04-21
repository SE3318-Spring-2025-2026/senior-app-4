package com.spms.backend.service.impl;

import com.spms.backend.dto.request.GroupCreateRequestDto;
import com.spms.backend.dto.request.GroupUpdateRequestDto;
import com.spms.backend.dto.response.GroupDetailDto;
import com.spms.backend.dto.response.GroupMemberDto;
import com.spms.backend.dto.response.GroupResponseDto;
import com.spms.backend.dto.request.JiraBindingRequest;
import com.spms.backend.dto.response.JiraIntegrationResponse;
import com.spms.backend.dto.response.GithubIntegrationResponse;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.exception.ForbiddenException;
import com.spms.backend.exception.NotFoundException;
import com.spms.backend.exception.UnauthorizedException;
import com.spms.backend.model.AuditLog;
import com.spms.backend.model.User;
import com.spms.backend.model.Group;
import com.spms.backend.model.GroupMember;
import com.spms.backend.model.GroupRole;
import com.spms.backend.model.GroupStatus;
import com.spms.backend.model.JiraIntegration;
import com.spms.backend.model.JiraIntegrationStatus;
import com.spms.backend.model.GithubIntegration;
import com.spms.backend.repository.AuditLogRepository;
import com.spms.backend.repository.GroupMemberRepository;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.JiraIntegrationRepository;
import com.spms.backend.repository.GithubIntegrationRepository;
import com.spms.backend.service.GroupService;
import com.spms.backend.client.JiraApiClient;
import com.spms.backend.service.NotificationService;
import com.spms.backend.service.StudentAuthorizationService;
import com.spms.backend.service.ValidationResult;
import com.spms.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.spms.backend.dto.response.MemberResponseDto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GroupServiceImpl implements GroupService {
    private static final Logger log = LoggerFactory.getLogger(GroupServiceImpl.class);
    private static final String STUDENT_ROLE = "student";

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditLogRepository auditLogRepository;
    private final StudentAuthorizationService authService;
    private final JiraIntegrationRepository jiraIntegrationRepository;
    private final GithubIntegrationRepository githubIntegrationRepository;
    private final JiraApiClient jiraApiClient;

    public GroupServiceImpl(GroupRepository groupRepository,
                            GroupMemberRepository groupMemberRepository,
                            UserRepository userRepository,
                            NotificationService notificationService,
                            AuditLogRepository auditLogRepository,
                            StudentAuthorizationService authService,
                            JiraIntegrationRepository jiraIntegrationRepository,
                            GithubIntegrationRepository githubIntegrationRepository,
                            JiraApiClient jiraApiClient) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditLogRepository = auditLogRepository;
        this.authService = authService;
        this.jiraIntegrationRepository = jiraIntegrationRepository;
        this.githubIntegrationRepository = githubIntegrationRepository;
        this.jiraApiClient = jiraApiClient;
    }

    @Override
    @Transactional
    public GroupResponseDto createGroup(GroupCreateRequestDto request, Long creatorId) {
        ValidationResult studentExists = authService.validateStudentExists(creatorId);
        if (!studentExists.valid()) {
            throw new BadRequestException(studentExists.reason());
        }

        ValidationResult notInGroup = authService.validateNotInGroup(creatorId);
        if (!notInGroup.valid()) {
            throw new BadRequestException(notInGroup.reason());
        }

        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new BadRequestException("Student not found."));

        Group group = new Group();
        group.setGroupName(request.groupName());
        group.setLeader(creator);
        group.setStatus(GroupStatus.FORMING);

        GroupMember leaderMember = new GroupMember();
        leaderMember.setGroup(group);
        leaderMember.setUser(creator);
        leaderMember.setRole(GroupRole.LEADER);

        group.getMembers().add(leaderMember);

        Group savedGroup = groupRepository.save(group);

        return mapToSimpleDto(savedGroup);
    }

    @Override
    @Transactional
    public void updateGroupName(Long groupId, GroupUpdateRequestDto request, Long requesterId) {
        ValidationResult isLeader = authService.validateIsGroupLeader(requesterId, groupId);
        if (!isLeader.valid()) {
            if ("Group not found.".equals(isLeader.reason())) {
                throw new BadRequestException(isLeader.reason());
            }
            throw new UnauthorizedException(isLeader.reason());
        }

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BadRequestException("Group not found."));

        group.setGroupName(request.groupName());
        group.setUpdatedAt(Instant.now());

        groupRepository.save(group);
        
    }

    // rol bazlı grup getirmek için
    @Override
    @Transactional(readOnly = true)
public Page<GroupResponseDto> getGroups(Pageable pageable, Long requesterId, String requesterRole) {

    Page<Group> groupsPage = null;

    String role = (requesterRole != null) ? requesterRole.toLowerCase() : "guest";

    switch (role) {
        case "student":
            groupsPage = groupRepository.findAllWithStudentGroupFirst(requesterId, pageable);
            break;

        case "professor":
            groupsPage = groupRepository.findByAdvisorId(requesterId, pageable);
            break;

        case "coordinator":
            groupsPage = groupRepository.findAll(pageable);
            try {
                List<Object[]> counts = groupRepository.countGroupsByStatus();
                counts.forEach(result -> 
                    log.info("Coordinator Summary - Status: {} Count: {}", result[0], result[1])
                );
            } catch (Exception e) {
                log.error("Status summary count failed", e);
            }
            break;

        default:

            groupsPage = groupRepository.findAll(pageable);
            break;
    }
    if (groupsPage == null) {
        return Page.empty(pageable);
    }

    return groupsPage.map(this::mapToSimpleDto);
}

    // rol abzlı detayları getirir
    @Override
    @Transactional(readOnly = true)
    public GroupDetailDto getGroupDetails(Long groupId, Long requesterId, String requesterRole) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found."));

        List<GroupMemberDto> memberDtos = group.getMembers().stream()
                .map(m -> new GroupMemberDto(
                        m.getUser().getUserId(),
                        m.getUser().getFullName(),
                        m.getRole().name(),
                        m.getJoinedAt()))
                .collect(Collectors.toList());

        return new GroupDetailDto(
                group.getId(),
                group.getGroupName(),
                group.getLeader().getUserId(),
                group.getAdvisor() != null ? group.getAdvisor().getUserId() : null,
                group.getStatus().name(),
                group.getCreatedAt(),
                group.getUpdatedAt(),
                memberDtos
        );
    }

    @Override
    @Transactional
    public void disbandGroup(Long groupId, Long requesterId, String requesterRole) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found."));

        boolean isCoordinator = "coordinator".equalsIgnoreCase(requesterRole);
        boolean isLeader = group.getLeader().getUserId().equals(requesterId);

        if (!isCoordinator && !isLeader) {
            throw new ForbiddenException("Only the group leader or coordinator can disband the group.");
        }

        List<GroupMember> currentMembers = new ArrayList<>(group.getMembers());
        List<Long> memberIds = currentMembers.stream()
                .map(member -> member.getUser().getUserId())
                .toList();

        group.setStatus(GroupStatus.DISBANDED);
        group.setUpdatedAt(Instant.now());

        for (GroupMember member : currentMembers) {
            User user = member.getUser();
            user.setRole(STUDENT_ROLE);
            userRepository.save(user);
        }

        groupMemberRepository.deleteAll(currentMembers);
        groupRepository.save(group);

        AuditLog auditLog = new AuditLog();
        auditLog.setAction("GROUP_DISBANDED");
        auditLog.setDescription("Group " + groupId + " disbanded by user " + requesterId);
        auditLog.setEntityId(groupId);
        auditLog.setEntityType("GROUP");
        auditLog.setTimestamp(Instant.now());
        auditLog.setUserId(requesterId);
        auditLogRepository.save(auditLog);

        try {
            notificationService.sendGroupDisbandedNotification(groupId, requesterId, group.getGroupName(), memberIds);
        } catch (Exception exception) {
            log.warn("Failed to send disband notifications for group {}: {}", groupId, exception.getMessage());
        }
    }




    @Override
    @Transactional
    public void bindJiraIntegration(Long groupId, Long requesterId, JiraBindingRequest request) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found."));

        ensureRequesterIsGroupLeader(group, requesterId);

        boolean valid = jiraApiClient.validateSpaceConnection(request.jiraSpaceUrl(), request.projectKey(), request.apiKey());
        if (!valid) {
            throw new BadRequestException("JIRA connection validation failed.");
        }

        JiraIntegration integration = jiraIntegrationRepository.findByGroup_Id(groupId)
                .orElseGet(JiraIntegration::new);
        integration.setGroup(group);
        integration.setJiraSpaceUrl(request.jiraSpaceUrl().trim());
        integration.setApiKey(request.apiKey() != null ? request.apiKey().trim() : null);
        integration.setProjectKey(request.projectKey().trim());
        integration.setStatus(JiraIntegrationStatus.ACTIVE);
        integration.setLastError(null);
        integration.setUpdatedAt(Instant.now());

        jiraIntegrationRepository.save(integration);

        try {
            notificationService.createSystemAlert(
                    group.getLeader().getUserId(),
                    "JIRA integration is active for group " + groupId,
                    "JIRA_INTEGRATION_STATUS",
                    request.jiraSpaceUrl().trim());
        } catch (Exception exception) {
            log.warn("Failed to create JIRA integration alert for group {}: {}", groupId, exception.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public JiraIntegrationResponse getJiraIntegration(Long groupId) {
        groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found."));

        var integration = jiraIntegrationRepository.findByGroup_Id(groupId);

        if (integration.isEmpty()) {
            return new JiraIntegrationResponse(
                    true,
                    new JiraIntegrationResponse.JiraIntegrationData(
                            "inactive",
                            null,
                            null,
                            null,
                            "Not connected"));
        }

        JiraIntegration jira = integration.get();
        return new JiraIntegrationResponse(
                true,
                new JiraIntegrationResponse.JiraIntegrationData(
                        jira.getStatus().name().toLowerCase(),
                        jira.getJiraSpaceUrl(),
                        jira.getProjectKey(),
                        jira.getCreatedAt().toString(),
                        jira.getLastError()));
    }

    @Override
    @Transactional(readOnly = true)
    public GithubIntegrationResponse getGithubIntegration(Long groupId) {
        groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found."));

        var integration = githubIntegrationRepository.findByGroup_Id(groupId);

        if (integration.isEmpty()) {
            return new GithubIntegrationResponse(
                    true,
                    new GithubIntegrationResponse.GithubIntegrationData(
                            "inactive",
                            null,
                            null,
                            "Not connected"));
        }

        GithubIntegration github = integration.get();
        return new GithubIntegrationResponse(
                true,
                new GithubIntegrationResponse.GithubIntegrationData(
                        github.getStatus().name().toLowerCase(),
                        github.getOrganizationName(),
                        github.getCreatedAt().toString(),
                        github.getLastError()));
    }

    @Override
    @Transactional
    public void unbindJiraIntegration(Long groupId, Long requesterId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found."));

        ensureRequesterIsGroupLeader(group, requesterId);

        JiraIntegration integration = jiraIntegrationRepository.findByGroup_Id(groupId)
                .orElseThrow(() -> new NotFoundException("JIRA integration not found."));

        jiraIntegrationRepository.delete(integration);
    }

    private void ensureRequesterIsGroupLeader(Group group, Long requesterId) {
        if (!group.getLeader().getUserId().equals(requesterId)) {
            throw new ForbiddenException("Only the group leader can manage JIRA integration.");
        }
    }

    private GroupResponseDto mapToSimpleDto(Group group) {
        return new GroupResponseDto(
                group.getId(),
                group.getGroupName(),
                group.getLeader().getUserId(),
                group.getAdvisor() != null ? group.getAdvisor().getUserId() : null,
                group.getStatus().name(),
                group.getMembers().size(),
                group.getCreatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberResponseDto> getGroupMembers(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found with id: " + groupId));

        return group.getMembers().stream().map(member -> {
            // Lider mi üye mi kontrolü
            String role = group.getLeader().getUserId().equals(member.getUser().getUserId()) ? "LEADER" : "MEMBER";

            return MemberResponseDto.builder()
                    .userId(member.getUser().getUserId())
                    .studentId(member.getUser().getStudentId())
                    .fullName(member.getUser().getFullName())
                    .role(role)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void removeMember(Long groupId, String studentId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found with id: " + groupId));

        GroupMember memberToRemove = group.getMembers().stream()
                .filter(m -> m.getUser().getStudentId() != null && m.getUser().getStudentId().equals(studentId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Member not found in this group"));

        if (group.getLeader().getUserId().equals(memberToRemove.getUser().getUserId())) {
            throw new BadRequestException("Group leader cannot be removed.");
        }

        group.getMembers().remove(memberToRemove);
        groupRepository.save(group);

        // AUDIT LOG EKLEMESİ (Kriter gereği)
        AuditLog auditLog = new AuditLog();
        auditLog.setAction("MEMBER_REMOVED");
        auditLog.setDescription("Member " + studentId + " removed from group " + groupId);
        auditLog.setEntityId(groupId);
        auditLog.setEntityType("GROUP");
        auditLog.setTimestamp(Instant.now());
        auditLogRepository.save(auditLog);
    }

    @Override
    @Transactional
    public void leaveGroup(Long groupId, Long studentUserId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found with id: " + groupId));

        GroupMember memberToLeave = group.getMembers().stream()
                .filter(m -> m.getUser().getUserId().equals(studentUserId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("You are not a member of this group."));

        if (group.getLeader().getUserId().equals(studentUserId)) {
            throw new BadRequestException("Group leader cannot leave.");
        }

        group.getMembers().remove(memberToLeave);
        groupRepository.save(group);
    }

    @Override
    @Transactional
    public void addMember(Long groupId, String studentId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found with id: " + groupId));

        // grup limiti 5i aşmasına izin vermeme şeysi
        if (group.getMembers().size() >= 5) {
            throw new BadRequestException("Group member limit reached.");
        }

        User studentToAdd = userRepository.findByStudentId(studentId)
                .orElseThrow(() -> new NotFoundException("Student not found."));

        authService.validateNotInGroup(studentToAdd.getUserId());

        GroupMember newMember = new GroupMember();
        newMember.setGroup(group);
        newMember.setUser(studentToAdd);
        newMember.setRole(GroupRole.MEMBER);

        group.getMembers().add(newMember);
        groupRepository.save(group);

        // BİLDİRİM TETİKLEME (Criteria ns_f1)
        try {
            notificationService.sendMembershipInvite(studentToAdd.getUserId(), groupId, group.getGroupName());
        } catch (Exception e) {
            log.warn("Notification failed: {}", e.getMessage());
        }
    }
}
