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
import com.spms.backend.repository.CommitteeRepository;
import com.spms.backend.repository.CommitteeAdvisorRepository;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.UserRepository;
import com.spms.backend.service.AdvisorAssignmentService;
import com.spms.backend.service.CommitteeNotificationService;
import com.spms.backend.service.NotificationService;
import com.spms.backend.model.Committee;
import com.spms.backend.model.CommitteeAdvisor;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** P4-ASSIGN-1 list logic + professor release of advisee group. */
@Service
public class AdvisorAssignmentServiceImpl implements AdvisorAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(AdvisorAssignmentServiceImpl.class);
    private static final String PROFESSOR_ROLE = "professor";

    private final GroupRepository groupRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationService notificationService;
    private final CommitteeRepository committeeRepository;
    private final UserRepository userRepository;
    private final CommitteeAdvisorRepository committeeAdvisorRepository;
    private final CommitteeNotificationService committeeNotificationService;

    public AdvisorAssignmentServiceImpl(
            GroupRepository groupRepository,
            AuditLogRepository auditLogRepository,
            NotificationService notificationService,
            CommitteeRepository committeeRepository,
            UserRepository userRepository,
            CommitteeAdvisorRepository committeeAdvisorRepository,
            CommitteeNotificationService committeeNotificationService) {
        this.groupRepository = groupRepository;
        this.auditLogRepository = auditLogRepository;
        this.notificationService = notificationService;
        this.committeeRepository = committeeRepository;
        this.userRepository = userRepository;
        this.committeeAdvisorRepository = committeeAdvisorRepository;
        this.committeeNotificationService = committeeNotificationService;
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
                .map(this::toDto)
                .sorted(Comparator.comparing(GroupAdvisorAssignmentDto::teamId))
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
                .map(this::toDto)
                .sorted(Comparator.comparing(GroupAdvisorAssignmentDto::teamId))
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

    private GroupAdvisorAssignmentDto toDto(Group g) {
        Instant assignedAt = null;
        String assignmentType = null;
        if (g.getAdvisor() != null) {
            java.util.Optional<AuditLog> log = auditLogRepository
                    .findTopByGroupIdAndActionTypeOrderByCreatedAtDesc(g.getId(), ActionType.ADVISOR_ASSIGNED);
            if (log.isPresent()) {
                assignedAt = log.get().getCreatedAt();
                String details = log.get().getEventDetails();
                // "approved by professor" → came via advisor request; otherwise coordinator override
                assignmentType = (details != null && details.toLowerCase().contains("approved"))
                        ? "REQUESTED" : "OVERRIDDEN";
            }
        }
        return new GroupAdvisorAssignmentDto(
                g.getId(),
                g.getGroupName(),
                g.getLeader() != null ? g.getLeader().getFullName() : "N/A",
                g.getAdvisor() != null ? g.getAdvisor().getUserId() : null,
                g.getAdvisor() != null ? g.getAdvisor().getFullName() : null,
                g.getStatus().name(),
                assignedAt,
                assignmentType);
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

    @Override
    @Transactional
    public void assignAdvisor(Long committeeId, Long advisorId, String role, Long assignedBy) {
        Committee committee = committeeRepository.findById(committeeId)
                .orElseThrow(() -> new NotFoundException("Committee not found"));

        User advisor = userRepository.findById(advisorId)
                .orElseThrow(() -> new NotFoundException("Advisor not found"));

        if (!"PROFESSOR".equalsIgnoreCase(advisor.getRole())) {
            throw new BadRequestException("Only professors can be assigned as advisors");
        }

        boolean alreadyAssigned = committee.getAdvisors().stream()
                .anyMatch(ca -> ca.getAdvisor().getUserId().equals(advisorId));
        if (alreadyAssigned) {
            throw new BadRequestException("Advisor already assigned to this committee");
        }

        CommitteeAdvisor committeeAdvisor = new CommitteeAdvisor(committee, advisor, role, assignedBy);
        committee.getAdvisors().add(committeeAdvisor);
        committeeRepository.save(committee);

        AuditLog log = new AuditLog();
        log.setActionType(ActionType.ADVISOR_ASSIGNED);
        log.setUserId(assignedBy);
        log.setEventDetails("Assigned advisor " + advisorId + " to committee " + committeeId + " with role " + role);
        auditLogRepository.save(log);

        try {
            committeeNotificationService.notifyAdvisorAssignment(committeeId, advisorId, assignedBy);
        } catch (Exception e) {
            // Ignore notification failure
        }
    }

    @Override
    @Transactional
    public void removeAdvisor(Long committeeId, Long advisorId) {
        Committee committee = committeeRepository.findById(committeeId)
                .orElseThrow(() -> new NotFoundException("Committee not found"));

        CommitteeAdvisor assignment = committee.getAdvisors().stream()
                .filter(ca -> ca.getAdvisor().getUserId().equals(advisorId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Advisor is not assigned to this committee"));

        committee.getAdvisors().remove(assignment);
        committeeAdvisorRepository.delete(assignment);
        committeeRepository.save(committee);

        // Assume assignedBy is the current context user, but since we don't pass it, we can just log a system event or skip.
        // The issue specifies AuditLogging, but removeAdvisor interface doesn't have `removedBy`. 
        // We'll log it without a specific `userId` or default to 1L (system) or the advisorId itself.
        AuditLog auditLog = new AuditLog();
        auditLog.setActionType(ActionType.MEMBER_REMOVED);
        auditLog.setUserId(1L); // Defaulting to system
        auditLog.setEventDetails("Removed advisor " + advisorId + " from committee " + committeeId);
        auditLogRepository.save(auditLog);
    }
}
