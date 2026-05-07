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
import com.spms.backend.repository.GroupCommitteeAssignmentRepository;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.UserRepository;
import com.spms.backend.model.GroupCommitteeAssignment;
import com.spms.backend.service.AdvisorAssignmentService;
import com.spms.backend.service.CommitteeNotificationService;
import com.spms.backend.service.NotificationService;
import com.spms.backend.model.Committee;
import com.spms.backend.model.CommitteeAdvisor;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
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
    private final GroupCommitteeAssignmentRepository groupCommitteeAssignmentRepository;

    public AdvisorAssignmentServiceImpl(
            GroupRepository groupRepository,
            AuditLogRepository auditLogRepository,
            NotificationService notificationService,
            CommitteeRepository committeeRepository,
            UserRepository userRepository,
            CommitteeAdvisorRepository committeeAdvisorRepository,
            CommitteeNotificationService committeeNotificationService,
            GroupCommitteeAssignmentRepository groupCommitteeAssignmentRepository) {
        this.groupRepository = groupRepository;
        this.auditLogRepository = auditLogRepository;
        this.notificationService = notificationService;
        this.committeeRepository = committeeRepository;
        this.userRepository = userRepository;
        this.committeeAdvisorRepository = committeeAdvisorRepository;
        this.committeeNotificationService = committeeNotificationService;
        this.groupCommitteeAssignmentRepository = groupCommitteeAssignmentRepository;
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

        if (PROFESSOR_ROLE.equalsIgnoreCase(requesterRole.trim())) {
            List<GroupAdvisorAssignmentDto> rows = buildProfessorRows(requesterUserId, hasAdvisor);
            return new AdvisorAssignmentListResponse("success", rows);
        }

        throw new ForbiddenException("Only coordinators and professors can list advisor assignments.");
    }

    private List<GroupAdvisorAssignmentDto> buildCoordinatorRows(Long filterAdvisorId, Boolean hasAdvisor) {
        List<Group> groups =
                groupRepository.findByStatusNot(GroupStatus.DISBANDED);

        return groups.stream()
                .filter(g -> matchesAdvisorIdFilter(g, filterAdvisorId))
                .filter(g -> matchesHasAdvisorFilter(g, hasAdvisor))
                .map(this::toDto)
                .sorted(Comparator.comparing(GroupAdvisorAssignmentDto::teamId))
                .toList();
    }

    private List<GroupAdvisorAssignmentDto> buildProfessorRows(Long professorUserId, Boolean hasAdvisor) {
        if (professorUserId == null) {
            throw new ForbiddenException("Missing user context.");
        }

        List<Group> groups =
                groupRepository.findByStatusNot(GroupStatus.DISBANDED);

        return groups.stream()
                .filter(g -> g.getAdvisor() != null && Objects.equals(g.getAdvisor().getUserId(), professorUserId))
                .filter(g -> matchesHasAdvisorFilter(g, hasAdvisor))
                .map(this::toDto)
                .sorted(Comparator.comparing(GroupAdvisorAssignmentDto::teamId))
                .toList();
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
            java.util.Optional<AuditLog> auditEntry = auditLogRepository
                    .findTopByGroupIdAndActionTypeInOrderByCreatedAtDesc(
                            g.getId(),
                            java.util.List.of(ActionType.ADVISOR_ASSIGNED, ActionType.ADVISOR_OVERRIDDEN));
            if (auditEntry.isPresent()) {
                assignedAt = auditEntry.get().getCreatedAt();
                assignmentType = auditEntry.get().getActionType() == ActionType.ADVISOR_OVERRIDDEN
                        ? "OVERRIDDEN"
                        : "REQUESTED";
            } else {
                assignmentType = "ASSIGNED";
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

        if (committee.getAdvisors().size() >= 5) {
            throw new BadRequestException("Committee cannot have more than 5 advisors");
        }

        CommitteeAdvisor committeeAdvisor = new CommitteeAdvisor(committee, advisor, role, assignedBy);
        committee.getAdvisors().add(committeeAdvisor);
        committeeRepository.save(committee);

        // Advisor'ın sahip olduğu grupları otomatik olarak commiteye ekle
        List<Group> advisorGroups = groupRepository.findByAdvisorId(advisorId);
        for (Group group : advisorGroups) {
            if (!groupCommitteeAssignmentRepository
                    .existsByCommittee_CommitteeIdAndGroupId(committeeId, group.getId())) {
                GroupCommitteeAssignment groupAssignment = new GroupCommitteeAssignment(
                        committee, group.getId(), GroupCommitteeAssignment.STATUS_ASSIGNED, assignedBy);
                groupCommitteeAssignmentRepository.save(groupAssignment);
            }
        }

        AuditLog auditLog = new AuditLog();
        auditLog.setActionType(ActionType.ADVISOR_ASSIGNED);
        auditLog.setUserId(assignedBy);
        auditLog.setEventDetails("Assigned advisor " + advisorId + " to committee " + committeeId + " with role " + role);
        auditLogRepository.save(auditLog);

        try {
            committeeNotificationService.notifyAdvisorAssignment(committeeId, advisorId, assignedBy);
        } catch (Exception e) {
            // Ignore notification failure
        }
    }

    @Override
    @Transactional
    public void removeAdvisor(Long committeeId, Long committeeAdvisorId) {
        Committee committee = committeeRepository.findById(committeeId)
                .orElseThrow(() -> new NotFoundException("Committee not found"));

        CommitteeAdvisor assignment = committee.getAdvisors().stream()
                .filter(ca -> ca.getCommitteeAdvisorId().equals(committeeAdvisorId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Advisor assignment not found"));

        Long removedAdvisorId = assignment.getAdvisor().getUserId();
        committee.getAdvisors().remove(assignment);
        committeeAdvisorRepository.delete(assignment);
        committeeRepository.save(committee);

        // Advisor'a ait grupları commiteden kaldır
        // (sadece bu advisor'ın danıştığı ve başka bir advisor tarafından da commiteye eklenmemiş olanlar)
        List<Group> advisorGroups = groupRepository.findByAdvisorId(removedAdvisorId);
        for (Group group : advisorGroups) {
            // Bu grubun advisorı hala bu kişi mi kontrol et (group'taki advisor field'ı)
            boolean groupStillAdvisedBySameAdvisor = group.getAdvisor() != null
                    && group.getAdvisor().getUserId().equals(removedAdvisorId);
            // Commitede başka bir advisor var mı bu gruba karşılık gelen?
            boolean otherAdvisorCoversGroup = committee.getAdvisors().stream()
                    .anyMatch(ca -> ca.getAdvisor() != null
                            && groupRepository.findByAdvisorId(ca.getAdvisor().getUserId())
                                    .stream().anyMatch(g -> g.getId().equals(group.getId())));
            if (!otherAdvisorCoversGroup) {
                groupCommitteeAssignmentRepository
                        .findByCommittee_CommitteeIdAndGroupId(committeeId, group.getId())
                        .ifPresent(groupCommitteeAssignmentRepository::delete);
            }
        }

        AuditLog auditLog = new AuditLog();
        auditLog.setActionType(ActionType.MEMBER_REMOVED);
        auditLog.setUserId(1L);
        auditLog.setEventDetails("Removed advisor " + assignment.getAdvisor().getUserId() + " from committee " + committeeId);
        auditLogRepository.save(auditLog);
    }
}
