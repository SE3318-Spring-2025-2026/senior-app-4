package com.spms.backend.service.impl;

import com.spms.backend.dto.response.AdvisorAssignmentListResponse;
import com.spms.backend.dto.response.GroupAdvisorAssignmentDto;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.exception.ForbiddenException;
import com.spms.backend.exception.NotFoundException;
import com.spms.backend.model.ActionType;
import com.spms.backend.model.AuditLog;
import com.spms.backend.model.Group;
import com.spms.backend.model.GroupStatus;
import com.spms.backend.model.User;
import com.spms.backend.repository.AuditLogRepository;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.service.AdvisorAssignmentService;
import com.spms.backend.service.NotificationService;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * P4-ASSIGN-1 list logic + professor release of advisee group.
 * TODO(P4-ASSIGN-2): expose {@code assignmentType} and accurate {@code assignedAt} once stored on group or derivable from audit.
 */
@Service
public class AdvisorAssignmentServiceImpl implements AdvisorAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(AdvisorAssignmentServiceImpl.class);
    private static final String PROFESSOR_ROLE = "professor";

    private final GroupRepository groupRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationService notificationService;

    public AdvisorAssignmentServiceImpl(
            GroupRepository groupRepository,
            AuditLogRepository auditLogRepository,
            NotificationService notificationService) {
        this.groupRepository = groupRepository;
        this.auditLogRepository = auditLogRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional(readOnly = true)
    public AdvisorAssignmentListResponse listAdvisorAssignments(
            String requesterRole, Long requesterUserId, Long filterAdvisorId, Boolean hasAdvisor) {

        if (requesterRole == null || requesterRole.isBlank()) {
            throw new ForbiddenException("Missing role.");
        }

        if ("coordinator".equalsIgnoreCase(requesterRole.trim())) {
            List<GroupAdvisorAssignmentDto> rows = buildCoordinatorRows(filterAdvisorId, hasAdvisor);
            return new AdvisorAssignmentListResponse("success", rows);
        }

        if ("professor".equalsIgnoreCase(requesterRole.trim())) {
            List<GroupAdvisorAssignmentDto> rows = buildProfessorRows(requesterUserId, hasAdvisor);
            return new AdvisorAssignmentListResponse("success", rows);
        }

        throw new ForbiddenException("Only coordinators and professors can list advisor assignments.");
    }

    private List<GroupAdvisorAssignmentDto> buildCoordinatorRows(Long filterAdvisorId, Boolean hasAdvisor) {
        List<Group> groups =
                groupRepository.findAllNonDisbandedWithAdvisorAndLeaderFetched(GroupStatus.DISBANDED);

        return groups.stream()
                .filter(g -> matchesAdvisorIdFilter(g, filterAdvisorId))
                .filter(g -> matchesHasAdvisorFilter(g, hasAdvisor))
                .map(AdvisorAssignmentServiceImpl::toDto)
                .sorted(Comparator.comparing(GroupAdvisorAssignmentDto::groupId))
                .collect(Collectors.toList());
    }

    private List<GroupAdvisorAssignmentDto> buildProfessorRows(Long professorUserId, Boolean hasAdvisor) {
        if (professorUserId == null) {
            throw new ForbiddenException("Missing user context.");
        }

        List<Group> groups =
                groupRepository.findAllNonDisbandedWithAdvisorAndLeaderFetched(GroupStatus.DISBANDED);

        return groups.stream()
                .filter(g -> g.getAdvisor() != null && Objects.equals(g.getAdvisor().getUserId(), professorUserId))
                .filter(g -> matchesHasAdvisorFilter(g, hasAdvisor))
                .map(AdvisorAssignmentServiceImpl::toDto)
                .sorted(Comparator.comparing(GroupAdvisorAssignmentDto::groupId))
                .collect(Collectors.toList());
    }

    /** Default (hasAdvisor null): only groups that have an advisor assigned (#160 acceptance). */
    private static boolean matchesHasAdvisorFilter(Group g, Boolean hasAdvisor) {
        if (Boolean.FALSE.equals(hasAdvisor)) {
            return g.getAdvisor() == null;
        }
        return g.getAdvisor() != null;
    }

    private static boolean matchesAdvisorIdFilter(Group g, Long filterAdvisorId) {
        if (filterAdvisorId == null) {
            return true;
        }
        return g.getAdvisor() != null && filterAdvisorId.equals(g.getAdvisor().getUserId());
    }

    private static GroupAdvisorAssignmentDto toDto(Group g) {
        return new GroupAdvisorAssignmentDto(
                g.getId(),
                g.getGroupName(),
                g.getLeader() != null ? g.getLeader().getFullName() : "N/A",
                g.getAdvisor() != null ? g.getAdvisor().getUserId() : null,
                g.getAdvisor() != null ? g.getAdvisor().getFullName() : null,
                g.getStatus().name());
    }

    @Override
    @Transactional
    public void releaseAdvisor(Long groupId, Long professorId, String role) {
        if (role == null || !PROFESSOR_ROLE.equalsIgnoreCase(role)) {
            throw new ForbiddenException("Only professors can release an advisee group.");
        }

        Group group = groupRepository
                .findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found."));

        User currentAdvisor = group.getAdvisor();
        if (currentAdvisor == null || group.getStatus() != GroupStatus.ADVISED) {
            throw new BadRequestException("Group does not have an assigned advisor.");
        }

        if (!currentAdvisor.getUserId().equals(professorId)) {
            throw new ForbiddenException("You are not the advisor of this group.");
        }

        group.setAdvisor(null);
        group.setStatus(GroupStatus.FORMING);
        group.setUpdatedAt(Instant.now());
        groupRepository.save(group);

        AuditLog auditLog = new AuditLog();
        auditLog.setActionType(ActionType.ADVISOR_RELEASED);
        auditLog.setUserId(professorId);
        auditLog.setGroupId(groupId);
        auditLog.setEventDetails("Professor " + professorId + " released advisee group " + groupId);
        auditLogRepository.save(auditLog);

        Long leaderId = group.getLeader() != null ? group.getLeader().getUserId() : null;
        if (leaderId != null) {
            try {
                notificationService.createSystemAlert(
                        leaderId,
                        "Your advisor has released your group. You may submit a new advisor request.",
                        "ADVISOR_RELEASED",
                        "groupId=" + groupId);
            } catch (Exception exception) {
                log.warn("Failed to send release notification for group {}: {}", groupId, exception.getMessage());
            }
        }
    }
}
