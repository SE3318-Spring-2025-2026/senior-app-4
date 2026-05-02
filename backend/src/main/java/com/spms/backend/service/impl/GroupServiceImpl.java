package com.spms.backend.service.impl;

import com.spms.backend.dto.request.GithubBindingRequest;
import com.spms.backend.dto.request.GroupCreateRequestDto;
import com.spms.backend.dto.request.GroupUpdateRequestDto;
import com.spms.backend.dto.response.GroupDetailDto;
import com.spms.backend.dto.response.GroupMemberDto;
import com.spms.backend.dto.response.GroupResponseDto;
import com.spms.backend.dto.request.JiraBindingRequest;
import com.spms.backend.dto.response.JiraIntegrationResponse;
import com.spms.backend.dto.response.GithubIntegrationResponse;
import com.spms.backend.dto.response.IntegrationsTestResponse;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.exception.ForbiddenException;
import com.spms.backend.exception.NotFoundException;
import com.spms.backend.exception.UnauthorizedException;
import com.spms.backend.annotation.AuditableOperation;
import com.spms.backend.model.ActionType;
import com.spms.backend.model.User;
import com.spms.backend.model.Group;
import com.spms.backend.model.GroupMember;
import com.spms.backend.model.GroupRole;
import com.spms.backend.model.GroupStatus;
import com.spms.backend.model.JiraIntegration;
import com.spms.backend.model.JiraIntegrationStatus;
import com.spms.backend.model.GithubIntegration;
import com.spms.backend.model.GithubIntegrationStatus;
import com.spms.backend.repository.AuditLogRepository;
import com.spms.backend.repository.GroupMemberRepository;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.JiraIntegrationRepository;
import com.spms.backend.repository.GithubIntegrationRepository;
import com.spms.backend.service.GroupService;
import com.spms.backend.client.GithubApiClient;
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
import com.spms.backend.model.notification.Notification;
import com.spms.backend.model.notification.NotificationStatus;
import com.spms.backend.model.notification.NotificationType;
import com.spms.backend.repository.NotificationRepository;
import com.spms.backend.model.AuditLog;
import com.spms.backend.dto.response.GroupFormationReportDto;
import com.spms.backend.dto.response.AdvisorRequestResponseDto;
import com.spms.backend.dto.request.OverrideAssignmentRequest;
import com.spms.backend.dto.response.OverrideAssignmentResponse;
import com.spms.backend.dto.response.AdvisorDecisionResponseDto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.jpa.domain.Specification;
import com.spms.backend.repository.specification.GroupSpecification;


@Service
public class GroupServiceImpl implements GroupService {
    private static final Logger log = LoggerFactory.getLogger(GroupServiceImpl.class);
    private static final String STUDENT_ROLE = "student";

    private final GithubApiClient githubApiClient;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final StudentAuthorizationService authService;
    private final JiraIntegrationRepository jiraIntegrationRepository;
    private final GithubIntegrationRepository githubIntegrationRepository;
    private final JiraApiClient jiraApiClient;

    private final NotificationRepository notificationRepository;
    private final AuditLogRepository auditLogRepository;

    public GroupServiceImpl(GroupRepository groupRepository,
                            GroupMemberRepository groupMemberRepository,
                            UserRepository userRepository,
                            NotificationService notificationService,
                            AuditLogRepository auditLogRepository,
                            StudentAuthorizationService authService,
                            JiraIntegrationRepository jiraIntegrationRepository,
                            GithubIntegrationRepository githubIntegrationRepository,
                            JiraApiClient jiraApiClient,
                            NotificationRepository notificationRepository,
                            GithubApiClient githubApiClient) 
                            {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.authService = authService;
        this.jiraIntegrationRepository = jiraIntegrationRepository;
        this.githubIntegrationRepository = githubIntegrationRepository;
        this.jiraApiClient = jiraApiClient;
        this.githubApiClient = githubApiClient;
        this.notificationRepository = notificationRepository;
        this.auditLogRepository = auditLogRepository;}

    @Override
    @Transactional
    @AuditableOperation(actionType = ActionType.GROUP_CREATED)
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
    @AuditableOperation(actionType = ActionType.GROUP_UPDATED)
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

    @Override
    @Transactional(readOnly = true)
    public Page<GroupResponseDto> getGroups(Pageable pageable, Long requesterId, String requesterRole, String status, String advisorAssigned, String groupName) {
        String role = (requesterRole != null) ? requesterRole.toLowerCase() : "guest";
        boolean hasFilters = (status != null && !status.isEmpty() && !"all".equalsIgnoreCase(status)) ||
                             (advisorAssigned != null && !advisorAssigned.isEmpty() && !"all".equalsIgnoreCase(advisorAssigned)) ||
                             (groupName != null && !groupName.trim().isEmpty());

        Page<Group> groupsPage;

        if ("student".equals(role) && !hasFilters) {
            groupsPage = groupRepository.findAllWithStudentGroupFirst(requesterId, pageable);
        } else {
            Specification<Group> spec = GroupSpecification.filterGroups(status, advisorAssigned, groupName, role, requesterId);
            groupsPage = groupRepository.findAll(spec, pageable);
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
                memberDtos);
    }

    @Override
    @Transactional
    @AuditableOperation(actionType = ActionType.GROUP_DISBANDED)
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

        // Üyelerin rolünü sıfırla
        for (GroupMember member : currentMembers) {
            User user = member.getUser();
            user.setRole(STUDENT_ROLE);
            userRepository.save(user);
        }

        // JPA cascade/orphanRemoval çakışmasını önlemek için önce in-memory koleksiyonu temizle
        group.getMembers().clear();

        // Üyeleri doğrudan veritabanından sil (cascade conflict olmadan)
        groupMemberRepository.deleteAllInBatch(currentMembers);

        group.setStatus(GroupStatus.DISBANDED);
        group.setUpdatedAt(Instant.now());
        groupRepository.save(group);

        try {
            notificationService.sendGroupDisbandedNotification(groupId, requesterId, group.getGroupName(), memberIds);
        } catch (Exception exception) {
            log.warn("Failed to send disband notifications for group {}: {}", groupId, exception.getMessage());
        }
    }

    @Override
    @Transactional
    @AuditableOperation(actionType = ActionType.INTEGRATION_BOUND)
    public void bindJiraIntegration(Long groupId, Long requesterId, JiraBindingRequest request) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found."));

        ensureRequesterIsGroupLeader(group, requesterId);

        boolean valid = jiraApiClient.validateSpaceConnection(request.jiraSpaceUrl(), request.projectKey(),
                request.apiKey());
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
                        jira.getStatus().name().toLowerCase(java.util.Locale.ROOT),
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

        GithubIntegration github = githubIntegrationRepository.findByGroup_Id(groupId)
                .orElseThrow(() -> new NotFoundException("No GitHub integration exists for this group."));

        return new GithubIntegrationResponse(
                true,
                new GithubIntegrationResponse.GithubIntegrationData(
                        github.getStatus().name().toLowerCase(java.util.Locale.ROOT),
                        github.getOrganizationName(),
                        github.getCreatedAt().toString(),
                        github.getLastError() != null ? github.getLastError() : "Connected successfully"
                )
        );
    }

    @Override
    @Transactional
    @AuditableOperation(actionType = ActionType.INTEGRATION_REMOVED)
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
                group.getMemberCount(),
                group.getCreatedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberResponseDto> getGroupMembers(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found with id: " + groupId));

        return group.getMembers().stream().map(member -> {
            // Lider mi üye mi kontrolü
            String role = group.getLeader().getUserId().equals(member.getUser().getUserId()) ? "LEADER" : "MEMBER";

            return new MemberResponseDto(
                    member.getUser().getUserId(),
                    member.getUser().getStudentId(),
                    member.getUser().getFullName(),
                    role);
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    @AuditableOperation(actionType = ActionType.MEMBER_REMOVED)
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
    }

    @Override
    @Transactional
    @AuditableOperation(actionType = ActionType.MEMBER_REMOVED)
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
    @AuditableOperation(actionType = ActionType.MEMBER_ADDED)
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

    private void ensureRequesterIsGithubLeader(Group group, Long requesterId) {
        Long leaderUserId = group.getLeader().getUserId();
        String leaderStudentId = group.getLeader().getStudentId(); // Modelinde bu alan varsa
        
        // Hem normal User ID'yi hem de Öğrenci Numarasını kontrol et
        boolean isUserIdMatch = leaderUserId != null && leaderUserId.equals(requesterId);
        boolean isStudentIdMatch = leaderStudentId != null && leaderStudentId.equals(String.valueOf(requesterId));

        if (!isUserIdMatch && !isStudentIdMatch) {
            throw new ForbiddenException("Only the group leader can manage GitHub integration.");
        }
    }

    @Override
    @Transactional
    public void bindGithubIntegration(Long groupId, Long requesterId, GithubBindingRequest request) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found."));

        ensureRequesterIsGithubLeader(group, requesterId);

        boolean valid = githubApiClient.validateOrganizationAccess(request.organizationName(), request.githubPat());
        if (!valid) {
            throw new BadRequestException("GitHub connection validation failed. Check PAT or Organization Name.");
        }

        GithubIntegration integration = githubIntegrationRepository.findByGroup_Id(groupId)
                .orElseGet(GithubIntegration::new);
        
        integration.setGroup(group);
        integration.setOrganizationName(request.organizationName().trim());
        integration.setGithubPatEncrypted(request.githubPat().trim()); 
        integration.setStatus(GithubIntegrationStatus.ACTIVE);
        integration.setUpdatedAt(Instant.now());

        githubIntegrationRepository.save(integration);
    }

    @Override
    @Transactional
    public void unbindGithubIntegration(Long groupId, Long requesterId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found."));
                
        ensureRequesterIsGithubLeader(group, requesterId);

        GithubIntegration integration = githubIntegrationRepository.findByGroup_Id(groupId)
                .orElseThrow(() -> new NotFoundException("GitHub integration not found."));

        githubIntegrationRepository.delete(integration);
    }

    @AuditableOperation(actionType = ActionType.ADVISOR_ASSIGNED)
    public void handleAdvisorRequestDecision(Long professorId, Long groupId, String status) {
        Notification request = notificationRepository.findByGroupIdAndToUser_UserIdAndTypeAndStatus(
                groupId, professorId, NotificationType.ADVISOR_REQUEST, NotificationStatus.PENDING)
                .orElseThrow(() -> new BadRequestException("Pending advisor request not found for this group."));

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found."));

        User professor = userRepository.findById(professorId)
                .orElseThrow(() -> new NotFoundException("Professor not found."));

        if ("approved".equalsIgnoreCase(status)) {
            group.setAdvisor(professor);
            group.setStatus(GroupStatus.ADVISED);
            group.setUpdatedAt(Instant.now());
            groupRepository.save(group);

            request.setStatus(NotificationStatus.ACCEPTED);
            notificationRepository.save(request);

            List<Notification> otherRequests = notificationRepository.findByGroupIdAndTypeAndStatusAndToUser_UserIdNot(
                    groupId, NotificationType.ADVISOR_REQUEST, NotificationStatus.PENDING, professorId);
            for (Notification otherReq : otherRequests) {
                otherReq.setStatus(NotificationStatus.REJECTED);
                notificationRepository.save(otherReq);

                Notification rejectNotif = new Notification();
                rejectNotif.setType(NotificationType.SYSTEM_ALERT);
                rejectNotif.setStatus(NotificationStatus.PENDING);
                rejectNotif.setMessage("Your request to be advisor for group " + group.getGroupName()
                        + " was automatically cancelled because they were assigned another advisor.");
                rejectNotif.setGroupId(groupId);
                rejectNotif.setToUser(otherReq.getToUser());
                notificationRepository.save(rejectNotif);

                AuditLog log = new AuditLog();
                log.setActionType(ActionType.ADVISOR_REJECTED);
                log.setUserId(professorId);
                log.setGroupId(groupId);
                log.setEventDetails(
                        "Auto-rejected pending advisor request for professor " + otherReq.getToUser().getUserId() + " ("
                                + otherReq.getToUser().getFullName() + ") due to approval of a different advisor.");
                auditLogRepository.save(log);
            }

            Notification notif = new Notification();
            notif.setType(NotificationType.ADVISOR_DECISION);
            notif.setStatus(NotificationStatus.PENDING);
            notif.setMessage("Professor " + professor.getFullName() + " has approved your advisor request.");
            notif.setGroupId(groupId);
            notif.setFromUser(professor);
            notif.setToUser(group.getLeader());
            notificationRepository.save(notif);

        } else if ("rejected".equalsIgnoreCase(status)) {
            request.setStatus(NotificationStatus.REJECTED);
            notificationRepository.save(request);

            Notification notif = new Notification();
            notif.setType(NotificationType.ADVISOR_DECISION);
            notif.setStatus(NotificationStatus.PENDING);
            notif.setMessage("Professor " + professor.getFullName() + " has rejected your advisor request.");
            notif.setGroupId(groupId);
            notif.setFromUser(professor);
            notif.setToUser(group.getLeader());
            notificationRepository.save(notif);
        } else {
            throw new BadRequestException("Status must be 'approved' or 'rejected'.");
        }
    }

    @Override
    @Transactional
    public AdvisorDecisionResponseDto processAdvisorRequestDecision(Long professorId, Long requestId, String status, String reason) {
        Notification request = notificationRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Advisor request not found."));

        if (!request.getToUser().getUserId().equals(professorId)) {
            throw new ForbiddenException("You are not authorized to process this request.");
        }
        
        if (request.getType() != NotificationType.ADVISOR_REQUEST || request.getStatus() != NotificationStatus.PENDING) {
            throw new BadRequestException("Request is not a pending advisor request.");
        }

        Long groupId = request.getGroupId();
        if (groupId == null) {
            throw new BadRequestException("Request is missing group information.");
        }

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found."));

        User professor = userRepository.findById(professorId)
                .orElseThrow(() -> new NotFoundException("Professor not found."));

        if ("APPROVE".equals(status)) {
            if (group.getAdvisor() != null) {
                throw new com.spms.backend.exception.ConflictException("Group already has an assigned advisor. The advisor must release the group first.");
            }
            group.setAdvisor(professor);
            group.setStatus(GroupStatus.ADVISED);
            group.setUpdatedAt(Instant.now());
            groupRepository.save(group);

            // Increment workload (FIX-1)
            professor.setCurrentAdviseeCount(professor.getCurrentAdviseeCount() + 1);
            userRepository.save(professor);

            request.setStatus(NotificationStatus.ACCEPTED);
            // B-3: persist decidedAt + reason in metadata for AdvisorRequestDetailServiceImpl
            request.setMetadata("{\"decidedAt\":\"" + java.time.Instant.now() + "\",\"reason\":\"\"}");
            notificationRepository.save(request);

            // Manual Audit Log for APPROVE (FIX-3)
            AuditLog approveLog = new AuditLog();
            approveLog.setActionType(ActionType.ADVISOR_ASSIGNED);
            approveLog.setUserId(professorId);
            approveLog.setGroupId(groupId);
            approveLog.setEventDetails("Advisor request approved by professor " + professor.getFullName());
            auditLogRepository.save(approveLog);

            List<Notification> otherRequests = notificationRepository.findByGroupIdAndTypeAndStatusAndToUser_UserIdNot(
                    groupId, NotificationType.ADVISOR_REQUEST, NotificationStatus.PENDING, professorId);
            for (Notification otherReq : otherRequests) {
                otherReq.setStatus(NotificationStatus.REJECTED);
                notificationRepository.save(otherReq);

                Notification rejectNotif = new Notification();
                rejectNotif.setType(NotificationType.SYSTEM_ALERT);
                rejectNotif.setStatus(NotificationStatus.PENDING);
                rejectNotif.setMessage("Your request to be advisor for group " + group.getGroupName()
                        + " was automatically cancelled because they were assigned another advisor.");
                rejectNotif.setGroupId(groupId);
                rejectNotif.setToUser(otherReq.getToUser());
                notificationRepository.save(rejectNotif);

                AuditLog log = new AuditLog();
                log.setActionType(ActionType.ADVISOR_REJECTED);
                log.setUserId(professorId);
                log.setGroupId(groupId);
                log.setEventDetails(
                        "Auto-rejected pending advisor request for professor " + otherReq.getToUser().getUserId() + " ("
                                + otherReq.getToUser().getFullName() + ") due to approval of a different advisor.");
                auditLogRepository.save(log);
            }

            Notification notif = new Notification();
            notif.setType(NotificationType.ADVISOR_DECISION);
            notif.setStatus(NotificationStatus.PENDING);
            notif.setMessage("Professor " + professor.getFullName() + " has approved your advisor request.");
            notif.setGroupId(groupId);
            notif.setFromUser(professor);
            notif.setToUser(group.getLeader());
            notificationRepository.save(notif);

            return new AdvisorDecisionResponseDto(
                    "success",
                    "Request approved. Advisor assigned to group.",
                    new AdvisorDecisionResponseDto.AdvisorDecisionData(requestId, "APPROVE", groupId, professorId)
            );

        } else if ("REJECT".equals(status)) {
            request.setStatus(NotificationStatus.REJECTED);
            // B-3: persist decidedAt + reason in metadata
            String escapedReason = reason != null ? reason.replace("\"", "'") : "";
            request.setMetadata("{\"decidedAt\":\"" + java.time.Instant.now() + "\",\"reason\":\"" + escapedReason + "\"}");
            notificationRepository.save(request);

            AuditLog log = new AuditLog();
            log.setActionType(ActionType.ADVISOR_REJECTED);
            log.setUserId(professorId);
            log.setGroupId(groupId);
            log.setEventDetails("Advisor request rejected. Reason: " + reason);
            auditLogRepository.save(log);

            Notification notif = new Notification();
            notif.setType(NotificationType.ADVISOR_DECISION);
            notif.setStatus(NotificationStatus.PENDING);
            String rejectMsg = "Professor " + professor.getFullName() + " has rejected your advisor request. Reason: " + reason;
            notif.setMessage(rejectMsg);
            notif.setGroupId(groupId);
            notif.setFromUser(professor);
            notif.setToUser(group.getLeader());
            notificationRepository.save(notif);

            return new AdvisorDecisionResponseDto(
                    "success",
                    "Request rejected.",
                    new AdvisorDecisionResponseDto.AdvisorDecisionData(requestId, "REJECT", groupId, professorId)
            );
        } else {
            throw new BadRequestException("Status must be 'APPROVE' or 'REJECT'.");
        }
    }

    @Override
    @Transactional
    @AuditableOperation(actionType = ActionType.ADVISOR_ASSIGNED)
    public void transferAdvisor(Long groupId, Long professorId, String requesterRole) {
        if (!"coordinator".equalsIgnoreCase(requesterRole)) {
            throw new ForbiddenException("Only coordinators can transfer advisors.");
        }

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found."));

        User professor = userRepository.findById(professorId)
                .orElseThrow(() -> new NotFoundException("Professor not found."));

        group.setAdvisor(professor);
        group.setStatus(GroupStatus.ADVISED);
        group.setUpdatedAt(Instant.now());
        groupRepository.save(group);

        Notification notif = new Notification();
        notif.setType(NotificationType.SYSTEM_ALERT);
        notif.setStatus(NotificationStatus.PENDING);
        notif.setMessage("You have been directly assigned as advisor to group: " + group.getGroupName()
                + " by the coordinator.");
        notif.setGroupId(groupId);
        notif.setToUser(professor);
        notificationRepository.save(notif);

        List<Notification> pendingRequests = notificationRepository.findByGroupIdAndTypeAndStatusAndToUser_UserIdNot(
                groupId, NotificationType.ADVISOR_REQUEST, NotificationStatus.PENDING, -1L);
        for (Notification req : pendingRequests) {
            req.setStatus(NotificationStatus.REJECTED);
            notificationRepository.save(req);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public IntegrationsTestResponse testIntegrations(Long groupId, Long requesterId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found."));
        ensureRequesterIsGroupLeader(group, requesterId);

        // 1. GitHub Test
        boolean githubConnected = false;
        String githubMsg = "Not configured";
        var githubOpt = githubIntegrationRepository.findByGroup_Id(groupId);
        if (githubOpt.isPresent()) {
            githubConnected = githubApiClient.validateOrganizationAccess(
                    githubOpt.get().getOrganizationName(), githubOpt.get().getGithubPatEncrypted());
            githubMsg = githubConnected ? "Connected" : "Token or Organization invalid";
        }

        // 2. Jira Test
        boolean jiraConnected = false;
        String jiraMsg = "Not configured";
        var jiraOpt = jiraIntegrationRepository.findByGroup_Id(groupId);
        if (jiraOpt.isPresent()) {
            jiraConnected = jiraApiClient.validateSpaceConnection(
                    jiraOpt.get().getJiraSpaceUrl(), jiraOpt.get().getProjectKey(), jiraOpt.get().getApiKey());
            jiraMsg = jiraConnected ? "Connected" : "Jira credentials invalid";
        }

        // En az biri kurulu olmalı
        if (githubOpt.isEmpty() && jiraOpt.isEmpty()) {
            throw new BadRequestException("No integrations configured to test.");
        }

        // Kabul kriteri: Biri bile başarısızsa 400 dön

        return new IntegrationsTestResponse(
                new IntegrationsTestResponse.IntegrationStatus(githubConnected, githubMsg),
                new IntegrationsTestResponse.IntegrationStatus(jiraConnected, jiraMsg)
        );}

    public GroupFormationReportDto getGroupFormationReport(String role) {
        if (!"coordinator".equalsIgnoreCase(role)) {
            throw new ForbiddenException("Only coordinators can view group formation reports.");
        }

        List<Group> allGroups = groupRepository.findAll();
        long totalGroups = allGroups.size();

        long formedGroupsCnt = allGroups.stream()
                .filter(g -> g.getStatus() == GroupStatus.FORMED || g.getStatus() == GroupStatus.ADVISED)
                .count();

        long unadvisedGroupsCnt = allGroups.stream()
                .filter(g -> g.getAdvisor() == null && g.getStatus() != GroupStatus.DISBANDED)
                .count();

        List<GroupFormationReportDto.GroupStatusDetail> details = allGroups.stream()
                .map(g -> new GroupFormationReportDto.GroupStatusDetail(
                        g.getId(),
                        g.getGroupName(),
                        g.getStatus().name(),
                        g.getAdvisor() != null ? g.getAdvisor().getUserId() : null,
                        g.getAdvisor() != null ? g.getAdvisor().getFullName() : null))
                .collect(Collectors.toList());

        return new GroupFormationReportDto(totalGroups, formedGroupsCnt, unadvisedGroupsCnt, details);
    }
    @Override
    @Transactional(readOnly = true)
    public List<AdvisorRequestResponseDto> getPendingAdvisorRequests(Long professorId) {
        List<Notification> requests = notificationRepository.findByToUser_UserIdAndTypeAndStatus(
                professorId, NotificationType.ADVISOR_REQUEST, NotificationStatus.PENDING);

        return requests.stream()
                .filter(req -> req.getGroupId() != null)
                .map(req -> {
                    Group group = groupRepository.findById(req.getGroupId()).orElse(null);
                    String groupName = group != null ? group.getGroupName() : "Unknown Group";
                    return new AdvisorRequestResponseDto(req.getId(), req.getGroupId(), groupName, req.getCreatedAt());
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OverrideAssignmentResponse overrideAdvisorAssignment(OverrideAssignmentRequest request, Long requesterId, String requesterRole) {
        if (!"coordinator".equalsIgnoreCase(requesterRole)) {
            throw new ForbiddenException("Only coordinators can override advisor assignments.");
        }

        Group group = groupRepository.findById(request.teamId())
                .orElseThrow(() -> new NotFoundException("Group not found."));

        User professor = userRepository.findById(request.advisorId())
                .orElseThrow(() -> new NotFoundException("Professor not found."));

        if (!"professor".equalsIgnoreCase(professor.getRole())) {
            throw new BadRequestException("The assigned user is not a professor.");
        }

        Long previousAdvisorId = group.getAdvisor() != null ? group.getAdvisor().getUserId() : null;

        group.setAdvisor(professor);
        group.setStatus(GroupStatus.ADVISED);
        group.setUpdatedAt(Instant.now());
        groupRepository.save(group);

        // Send SYSTEM_ALERT to the new advisor
        Notification advisorNotif = new Notification();
        advisorNotif.setType(NotificationType.SYSTEM_ALERT);
        advisorNotif.setStatus(NotificationStatus.PENDING);
        advisorNotif.setMessage("You have been forcefully assigned as advisor to group: " + group.getGroupName() + " by the coordinator.");
        advisorNotif.setGroupId(group.getId());
        advisorNotif.setToUser(professor);
        notificationRepository.save(advisorNotif);

        // Send SYSTEM_ALERT to the group leader
        Notification leaderNotif = new Notification();
        leaderNotif.setType(NotificationType.SYSTEM_ALERT);
        leaderNotif.setStatus(NotificationStatus.PENDING);
        leaderNotif.setMessage("Your group's advisor has been forcefully changed to " + professor.getFullName() + " by the coordinator.");
        leaderNotif.setGroupId(group.getId());
        leaderNotif.setToUser(group.getLeader());
        notificationRepository.save(leaderNotif);

        // Cancel any pending advisor requests for this group
        List<Notification> pendingRequests = notificationRepository.findByGroupIdAndTypeAndStatusAndToUser_UserIdNot(
                group.getId(), NotificationType.ADVISOR_REQUEST, NotificationStatus.PENDING, -1L);
        for (Notification req : pendingRequests) {
            req.setStatus(NotificationStatus.REJECTED);
            notificationRepository.save(req);
        }

        // Send SYSTEM_ALERT to the previous advisor (displaced advisor)
        if (previousAdvisorId != null) {
            User previousAdvisor = userRepository.findById(previousAdvisorId).orElse(null);
            if (previousAdvisor != null) {
                Notification oldAdvisorNotif = new Notification();
                oldAdvisorNotif.setType(NotificationType.SYSTEM_ALERT);
                oldAdvisorNotif.setStatus(NotificationStatus.PENDING);
                oldAdvisorNotif.setMessage("You have been released as advisor from group: " + group.getGroupName() + " due to a coordinator override.");
                oldAdvisorNotif.setGroupId(group.getId());
                oldAdvisorNotif.setToUser(previousAdvisor);
                notificationRepository.save(oldAdvisorNotif);
            }
        }

        // Write to D9 (System Logs) manually to include reason and ensure correct groupId mapping
        AuditLog log = new AuditLog();
        log.setActionType(ActionType.ADVISOR_OVERRIDDEN);
        log.setUserId(requesterId);
        log.setGroupId(group.getId());
        
        String logReason = (request.reason() != null && !request.reason().isBlank()) 
                ? " Reason: " + request.reason() 
                : "";
        log.setEventDetails("Coordinator forcefully assigned professor " + professor.getUserId() + 
                " (" + professor.getFullName() + ") as advisor to group " + group.getId() + "." + logReason);
        auditLogRepository.save(log);

        return new OverrideAssignmentResponse(
                "success",
                "Advisor assignment overridden. Both parties notified.",
                new OverrideAssignmentResponse.OverrideAssignmentData(
                        group.getId(),
                        previousAdvisorId,
                        professor.getUserId()
                )
        );
    }
}
