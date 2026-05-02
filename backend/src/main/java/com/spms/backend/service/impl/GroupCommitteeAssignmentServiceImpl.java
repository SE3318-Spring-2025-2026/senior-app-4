package com.spms.backend.service.impl;

import com.spms.backend.dto.request.AssignmentStatusUpdateRequest;
import com.spms.backend.dto.request.GroupAssignmentRequest;
import com.spms.backend.dto.response.DeleteResponse;
import com.spms.backend.dto.response.GroupAssignmentResponse;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.exception.ConflictException;
import com.spms.backend.exception.NotFoundException;
import com.spms.backend.model.ActionType;
import com.spms.backend.model.AuditLog;
import com.spms.backend.model.Committee;
import com.spms.backend.model.Group;
import com.spms.backend.model.GroupCommitteeAssignment;
import com.spms.backend.model.GroupMember;
import com.spms.backend.model.User;
import com.spms.backend.model.notification.Notification;
import com.spms.backend.model.notification.NotificationStatus;
import com.spms.backend.model.notification.NotificationType;
import com.spms.backend.repository.AuditLogRepository;
import com.spms.backend.repository.CommitteeRepository;
import com.spms.backend.repository.GroupCommitteeAssignmentRepository;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.NotificationRepository;
import com.spms.backend.repository.UserRepository;
import com.spms.backend.service.GroupCommitteeAssignmentService;
import com.spms.backend.service.ScheduleValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GroupCommitteeAssignmentServiceImpl implements GroupCommitteeAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(GroupCommitteeAssignmentServiceImpl.class);

    /** State machine — destinations allowed from each status (no backtracking). */
    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            GroupCommitteeAssignment.STATUS_ASSIGNED,
                    Set.of(GroupCommitteeAssignment.STATUS_SCHEDULED,
                            GroupCommitteeAssignment.STATUS_CANCELLED),
            GroupCommitteeAssignment.STATUS_SCHEDULED,
                    Set.of(GroupCommitteeAssignment.STATUS_COMPLETED,
                            GroupCommitteeAssignment.STATUS_CANCELLED),
            GroupCommitteeAssignment.STATUS_COMPLETED, Set.of(),
            GroupCommitteeAssignment.STATUS_CANCELLED, Set.of()
    );

    private final GroupCommitteeAssignmentRepository assignmentRepository;
    private final CommitteeRepository committeeRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogRepository auditLogRepository;
    private final ScheduleValidator scheduleValidator;

    public GroupCommitteeAssignmentServiceImpl(GroupCommitteeAssignmentRepository assignmentRepository,
                                               CommitteeRepository committeeRepository,
                                               GroupRepository groupRepository,
                                               UserRepository userRepository,
                                               NotificationRepository notificationRepository,
                                               AuditLogRepository auditLogRepository,
                                               ScheduleValidator scheduleValidator) {
        this.assignmentRepository = assignmentRepository;
        this.committeeRepository = committeeRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.auditLogRepository = auditLogRepository;
        this.scheduleValidator = scheduleValidator;
    }

    // ── POST /api/v1/committees/{committeeId}/groups ────────────────────────
    @Override
    @Transactional
    public GroupAssignmentResponse assignGroup(Long committeeId,
                                               GroupAssignmentRequest request,
                                               Long actorUserId) {
        Committee committee = committeeRepository.findById(committeeId)
                .orElseThrow(() -> new NotFoundException("Committee not found: " + committeeId));

        Long groupId = request.groupId();
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found: " + groupId));

        if (assignmentRepository.existsByCommittee_CommitteeIdAndGroupId(committeeId, groupId)) {
            throw new ConflictException(
                    "Group " + groupId + " is already assigned to committee " + committeeId + ".");
        }

        Instant examDate = request.examDate();
        if (examDate != null) {
            scheduleValidator.validateExamDate(examDate);
            List<GroupCommitteeAssignment> conflicts =
                    scheduleValidator.findConflictsForCommittee(committeeId, examDate, null);
            if (!conflicts.isEmpty()) {
                throw new BadRequestException(buildConflictMessage(conflicts));
            }
        }

        GroupCommitteeAssignment assignment = new GroupCommitteeAssignment(
                committee,
                groupId,
                examDate != null
                        ? GroupCommitteeAssignment.STATUS_SCHEDULED
                        : GroupCommitteeAssignment.STATUS_ASSIGNED,
                actorUserId);
        assignment.setExamDate(examDate);

        GroupCommitteeAssignment saved = assignmentRepository.save(assignment);

        writeAuditLog(actorUserId, ActionType.COMMITTEE_GROUP_ASSIGNED, groupId,
                "Group " + groupId + " assigned to committee " + committeeId
                        + (examDate != null ? " for exam at " + examDate : "") + ".");

        notifyGroupMembers(group, actorUserId,
                "Your group has been assigned to a committee."
                        + (examDate != null ? " Exam date: " + examDate : ""));

        return GroupAssignmentResponse.from(saved);
    }

    // ── GET /api/v1/committees/{committeeId}/groups ─────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<GroupAssignmentResponse> listGroupsForCommittee(Long committeeId) {
        if (!committeeRepository.existsById(committeeId)) {
            throw new NotFoundException("Committee not found: " + committeeId);
        }
        return assignmentRepository.findByCommittee_CommitteeId(committeeId).stream()
                .map(GroupAssignmentResponse::from)
                .collect(Collectors.toList());
    }

    // ── GET /api/v1/committees/groups/{groupId}/committees ──────────────────
    @Override
    @Transactional(readOnly = true)
    public List<GroupAssignmentResponse> listCommitteesForGroup(Long groupId) {
        if (!groupRepository.existsById(groupId)) {
            throw new NotFoundException("Group not found: " + groupId);
        }
        return assignmentRepository.findByGroupId(groupId).stream()
                .map(GroupAssignmentResponse::from)
                .collect(Collectors.toList());
    }

    // ── PATCH /api/v1/committees/{committeeId}/groups/{assignmentId}/status ─
    @Override
    @Transactional
    public GroupAssignmentResponse updateStatus(Long committeeId,
                                                Long assignmentId,
                                                AssignmentStatusUpdateRequest request,
                                                Long actorUserId) {
        GroupCommitteeAssignment assignment = loadAssignment(committeeId, assignmentId);

        String requested = request.status() != null ? request.status().toUpperCase() : "";
        Set<String> allowed = ALLOWED_TRANSITIONS.get(assignment.getStatus());
        if (allowed == null || !allowed.contains(requested)) {
            throw new BadRequestException(
                    "Invalid status transition: " + assignment.getStatus() + " → " + requested + ".");
        }

        if (GroupCommitteeAssignment.STATUS_SCHEDULED.equals(requested)) {
            Instant examDate = request.examDate() != null ? request.examDate() : assignment.getExamDate();
            if (examDate == null) {
                throw new BadRequestException("examDate is required to move to SCHEDULED.");
            }
            scheduleValidator.validateExamDate(examDate);

            List<GroupCommitteeAssignment> conflicts = scheduleValidator
                    .findConflictsForCommittee(committeeId, examDate, assignment.getAssignmentId());
            if (!conflicts.isEmpty()) {
                throw new BadRequestException(buildConflictMessage(conflicts));
            }
            assignment.setExamDate(examDate);
        }

        assignment.setStatus(requested);
        GroupCommitteeAssignment saved = assignmentRepository.save(assignment);

        writeAuditLog(actorUserId, ActionType.COMMITTEE_GROUP_STATUS_UPDATED, saved.getGroupId(),
                "Assignment " + assignmentId + " status set to " + requested + ".");

        groupRepository.findById(saved.getGroupId()).ifPresent(group ->
                notifyGroupMembers(group, actorUserId,
                        "Your committee assignment status changed to " + requested + "."));

        return GroupAssignmentResponse.from(saved);
    }

    // ── DELETE /api/v1/committees/{committeeId}/groups/{assignmentId} ──────
    @Override
    @Transactional
    public DeleteResponse deleteAssignment(Long committeeId, Long assignmentId, Long actorUserId) {
        GroupCommitteeAssignment assignment = loadAssignment(committeeId, assignmentId);
        Long groupId = assignment.getGroupId();

        assignmentRepository.delete(assignment);

        writeAuditLog(actorUserId, ActionType.COMMITTEE_GROUP_REMOVED, groupId,
                "Assignment " + assignmentId + " removed from committee " + committeeId + ".");

        groupRepository.findById(groupId).ifPresent(group ->
                notifyGroupMembers(group, actorUserId,
                        "Your group's committee assignment has been removed."));

        return new DeleteResponse(
                "Assignment " + assignmentId + " removed from committee " + committeeId + ".");
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private GroupCommitteeAssignment loadAssignment(Long committeeId, Long assignmentId) {
        GroupCommitteeAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException("Assignment not found: " + assignmentId));
        if (assignment.getCommittee() == null
                || !assignment.getCommittee().getCommitteeId().equals(committeeId)) {
            throw new NotFoundException(
                    "Assignment " + assignmentId + " does not belong to committee " + committeeId + ".");
        }
        return assignment;
    }

    private String buildConflictMessage(List<GroupCommitteeAssignment> conflicts) {
        StringBuilder sb = new StringBuilder("Schedule conflict detected within ±2 hours:");
        for (GroupCommitteeAssignment c : conflicts) {
            sb.append(" [assignmentId=").append(c.getAssignmentId())
                    .append(", groupId=").append(c.getGroupId())
                    .append(", examDate=").append(c.getExamDate()).append("]");
        }
        return sb.toString();
    }

    private void writeAuditLog(Long userId, ActionType actionType, Long groupId, String details) {
        try {
            AuditLog entry = new AuditLog();
            entry.setUserId(userId != null ? userId : 0L);
            entry.setActionType(actionType);
            entry.setGroupId(groupId);
            entry.setEventDetails(details);
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.warn("[AUDIT] failed to persist audit log: {}", e.getMessage());
        }
    }

    private void notifyGroupMembers(Group group, Long actorUserId, String message) {
        if (group == null || group.getMembers() == null || group.getMembers().isEmpty()) {
            return;
        }
        User actor = actorUserId != null
                ? userRepository.findById(actorUserId).orElse(null)
                : null;

        List<Notification> batch = new ArrayList<>(group.getMembers().size());
        for (GroupMember member : group.getMembers()) {
            User recipient = member.getUser();
            if (recipient == null) {
                continue;
            }
            Notification n = new Notification();
            n.setType(NotificationType.COMMITTEE_ASSIGNED);
            n.setStatus(NotificationStatus.PENDING);
            n.setMessage(message);
            n.setGroupId(group.getId());
            n.setFromUser(actor);
            n.setToUser(recipient);
            batch.add(n);
        }
        if (!batch.isEmpty()) {
            notificationRepository.saveAll(batch);
        }
    }
}
