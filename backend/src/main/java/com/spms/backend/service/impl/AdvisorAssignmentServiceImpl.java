package com.spms.backend.service.impl;

import com.spms.backend.exception.BadRequestException;
import com.spms.backend.exception.ForbiddenException;
import com.spms.backend.exception.NotFoundException;
import com.spms.backend.model.ActionType;
import com.spms.backend.model.AuditLog;
import com.spms.backend.model.Group;
import com.spms.backend.model.GroupStatus;
import com.spms.backend.model.User;
import java.time.Instant;
import com.spms.backend.repository.AuditLogRepository;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.service.AdvisorAssignmentService;
import com.spms.backend.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdvisorAssignmentServiceImpl implements AdvisorAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(AdvisorAssignmentServiceImpl.class);
    private static final String PROFESSOR_ROLE = "professor";

    private final GroupRepository groupRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationService notificationService;

    public AdvisorAssignmentServiceImpl(GroupRepository groupRepository,
                                        AuditLogRepository auditLogRepository,
                                        NotificationService notificationService) {
        this.groupRepository = groupRepository;
        this.auditLogRepository = auditLogRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public void releaseAdvisor(Long groupId, Long professorId, String role) {
        if (role == null || !PROFESSOR_ROLE.equalsIgnoreCase(role)) {
            throw new ForbiddenException("Only professors can release an advisee group.");
        }

        Group group = groupRepository.findById(groupId)
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
