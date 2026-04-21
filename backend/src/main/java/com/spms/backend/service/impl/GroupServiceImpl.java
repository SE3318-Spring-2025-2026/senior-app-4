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
import com.spms.backend.model.notification.Notification;
import com.spms.backend.model.notification.NotificationStatus;
import com.spms.backend.model.notification.NotificationType;
import com.spms.backend.repository.NotificationRepository;
import com.spms.backend.repository.AuditLogRepository;
import com.spms.backend.model.AuditLog;
import com.spms.backend.dto.response.GroupFormationReportDto;
import com.spms.backend.dto.response.AdvisorRequestResponseDto;

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
            StudentAuthorizationService authService,
            JiraIntegrationRepository jiraIntegrationRepository,
            GithubIntegrationRepository githubIntegrationRepository,
            JiraApiClient jiraApiClient,
            NotificationRepository notificationRepository,
            AuditLogRepository auditLogRepository) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.authService = authService;
        this.jiraIntegrationRepository = jiraIntegrationRepository;
        this.githubIntegrationRepository = githubIntegrationRepository;
        this.jiraApiClient = jiraApiClient;
        this.notificationRepository = notificationRepository;
        this.auditLogRepository = auditLogRepository;
    }

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
                    counts.forEach(
                            result -> log.info("Coordinator Summary - Status: {} Count: {}", result[0], result[1]));
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

        group.setStatus(GroupStatus.DISBANDED);
        group.setUpdatedAt(Instant.now());

        for (GroupMember member : currentMembers) {
            User user = member.getUser();
            user.setRole(STUDENT_ROLE);
            userRepository.save(user);
        }

        groupMemberRepository.deleteAll(currentMembers);
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
                group.getMembers().size(),
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
    @AuditableOperation(actionType = ActionType.ADVISOR_ASSIGNED)
    public void processAdvisorRequestDecision(Long professorId, Long requestId, String status, String reason) {
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

            AuditLog log = new AuditLog();
            log.setActionType(ActionType.ADVISOR_REJECTED);
            log.setUserId(professorId);
            log.setGroupId(groupId);
            log.setEventDetails("Advisor request rejected. Reason: " + (reason != null ? reason : "No reason provided"));
            auditLogRepository.save(log);

            Notification notif = new Notification();
            notif.setType(NotificationType.ADVISOR_DECISION);
            notif.setStatus(NotificationStatus.PENDING);
            String rejectMsg = "Professor " + professor.getFullName() + " has rejected your advisor request.";
            if (reason != null && !reason.isBlank()) {
                rejectMsg += " Reason: " + reason;
            }
            notif.setMessage(rejectMsg);
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
}
