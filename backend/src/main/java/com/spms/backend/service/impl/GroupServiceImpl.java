package com.spms.backend.service.impl;

import com.spms.backend.dto.request.GroupCreateRequestDto;
import com.spms.backend.dto.request.GroupUpdateRequestDto;
import com.spms.backend.dto.response.GroupDetailDto;
import com.spms.backend.dto.response.GroupMemberDto;
import com.spms.backend.dto.response.GroupResponseDto;
import com.spms.backend.exception.ForbiddenException;
import com.spms.backend.exception.NotFoundException;
import com.spms.backend.model.AuditLog;
import com.spms.backend.model.User;
import com.spms.backend.model.Group;
import com.spms.backend.model.GroupMember;
import com.spms.backend.model.GroupRole;
import com.spms.backend.model.GroupStatus;
import com.spms.backend.repository.AuditLogRepository;
import com.spms.backend.repository.GroupMemberRepository;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.service.GroupService;
import com.spms.backend.service.NotificationService;
import com.spms.backend.service.StudentAuthorizationService;
import com.spms.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public GroupServiceImpl(GroupRepository groupRepository,
                            GroupMemberRepository groupMemberRepository,
                            UserRepository userRepository,
                            NotificationService notificationService,
                            AuditLogRepository auditLogRepository,
                            StudentAuthorizationService authService) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditLogRepository = auditLogRepository;
        this.authService = authService;
    }

    @Override
    @Transactional
    public GroupResponseDto createGroup(GroupCreateRequestDto request, Long creatorId) {
        User creator = authService.validateStudentExists(creatorId);
        authService.validateNotInGroup(creatorId);

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

        Group group = authService.validateIsGroupLeader(requesterId, groupId);

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
}