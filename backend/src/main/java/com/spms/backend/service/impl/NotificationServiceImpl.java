package com.spms.backend.service.impl;

import com.spms.backend.dto.request.AdvisorRequestDto;
import com.spms.backend.dto.response.AdvisorRequestStatusDto;
import com.spms.backend.dto.response.NotificationDto;
import com.spms.backend.annotation.AuditableOperation;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.exception.ConflictException;
import com.spms.backend.exception.ForbiddenException;
import com.spms.backend.exception.NotFoundException;
import com.spms.backend.exception.UnauthorizedException;
import com.spms.backend.model.ActionType;
import com.spms.backend.model.Group;
import com.spms.backend.model.Schedule;
import com.spms.backend.model.notification.Notification;
import com.spms.backend.model.notification.NotificationStatus;
import com.spms.backend.model.notification.NotificationType;
import com.spms.backend.model.User;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.NotificationRepository;
import com.spms.backend.repository.ScheduleRepository;
import com.spms.backend.repository.UserRepository;
import com.spms.backend.service.MemberService;
import com.spms.backend.service.NotificationService;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Optional;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final ScheduleRepository scheduleRepository;
    private final MemberService memberService;
    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   UserRepository userRepository,
                                   GroupRepository groupRepository,
                                   ScheduleRepository scheduleRepository,
                                   @Lazy MemberService memberService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.scheduleRepository = scheduleRepository;
        this.memberService = memberService;
    }

    @Override
    @Transactional
    @AuditableOperation(actionType = ActionType.ADVISOR_REQUESTED)
    public Long requestAdvisor(Long groupId, AdvisorRequestDto request, Long leaderId) {

        // ── P4.2: Validate group exists ───────────────────────────────────────
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BadRequestException("Group not found."));

        // ── P4.2: Validate requester is the group leader ───────────────────────
        if (!group.getLeader().getUserId().equals(leaderId)) {
            throw new ForbiddenException("Only the group leader can send advisor requests.");
        }

        // ── P4.2: Check if group already has an active advisor [P4-CONFLICT-1] ─
        if (group.getAdvisor() != null) {
            throw new ConflictException("This group already has an active advisor.");
        }

        // ── P4.2: Check advisor assignment deadline (D10) ─────────────────────
        Optional<Schedule> scheduleOpt = scheduleRepository.findTopByOrderByIdDesc();
        if (scheduleOpt.isPresent()) {
            Schedule schedule = scheduleOpt.get();
            if (schedule.getAdvisorAssignmentDeadline() != null
                    && Instant.now().isAfter(schedule.getAdvisorAssignmentDeadline())) {
                throw new BadRequestException(
                        "Advisor assignment deadline has passed. Requests are no longer accepted.");
            }
        }

        // ── P4.2: only 1 pending advisor request may exist at a time ───────────
        Optional<Notification> existingPendingRequest = notificationRepository
                .findByGroupIdAndTypeAndStatus(groupId, NotificationType.ADVISOR_REQUEST, NotificationStatus.PENDING);
        if (existingPendingRequest.isPresent()) {
            throw new BadRequestException("This group already has a pending advisor request.");
        }

        // ── P4.1: Resolve users ────────────────────────────────────────────────
        User leader = userRepository.findById(leaderId)
                .orElseThrow(() -> new BadRequestException("Leader user not found."));

        User professor = userRepository.findById(request.professorId())
                .orElseThrow(() -> new BadRequestException("Professor not found."));

        if (!"professor".equalsIgnoreCase(professor.getRole())) {
            throw new BadRequestException("Target user is not a professor.");
        }

        // ── D8: Create ADVISOR_REQUEST notification for the professor ──────────
        Notification notification = new Notification();
        notification.setType(NotificationType.ADVISOR_REQUEST);
        notification.setStatus(NotificationStatus.PENDING);
        // C-28: include optional custom message from team leader
        String baseMsg = leader.getFullName() + " has requested you as an advisor for their group.";
        String customMsg = request.message();
        notification.setMessage(customMsg != null && !customMsg.isBlank()
                ? baseMsg + " Message: " + customMsg
                : baseMsg);
        notification.setGroupId(groupId);
        notification.setFromUser(leader);
        notification.setToUser(professor);

        return notificationRepository.save(notification).getId();
    }

    @Override
    @Transactional(readOnly = true)
    public AdvisorRequestStatusDto getAdvisorRequestStatus(Long groupId) {
        Notification lastRequest = notificationRepository
                .findByGroupIdAndTypeAndStatus(groupId, NotificationType.ADVISOR_REQUEST, NotificationStatus.PENDING)
                .orElseThrow(() -> new BadRequestException("No pending advisor request found."));

        return new AdvisorRequestStatusDto(
                lastRequest.getId(),
                lastRequest.getToUser() != null ? lastRequest.getToUser().getFullName() : "Unknown",
                lastRequest.getStatus().name()
        );
    }

    @Override
    @Transactional
    @AuditableOperation(actionType = ActionType.ADVISOR_REQUEST_CANCELLED)
    public void cancelAdvisorRequest(Long groupId) {
        Notification request = notificationRepository
                .findByGroupIdAndTypeAndStatus(groupId, NotificationType.ADVISOR_REQUEST, NotificationStatus.PENDING)
                .orElseThrow(() -> new BadRequestException("No pending advisor request found to cancel."));

        request.setStatus(NotificationStatus.CLEARED);
        notificationRepository.save(request);

        log.info("[AUDIT] cancelAdvisorRequest: groupId={} notificationId={} newStatus=CLEARED",
                groupId, request.getId());
    }

    @Override
    @Transactional
    @AuditableOperation(actionType = ActionType.ADVISOR_REQUEST_WITHDRAWN)
    public void withdrawAdvisorRequest(Long notificationId, Long requesterId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException(
                        "Advisor request not found with id: " + notificationId));

        if (notification.getType() != NotificationType.ADVISOR_REQUEST) {
            throw new BadRequestException("Notification is not an advisor request.");
        }

        if (notification.getStatus() != NotificationStatus.PENDING) {
            throw new BadRequestException(
                    "Only pending advisor requests can be withdrawn. Current status: "
                            + notification.getStatus().name());
        }

        if (notification.getFromUser() == null
                || !notification.getFromUser().getUserId().equals(requesterId)) {
            throw new ForbiddenException(
                    "You are not authorized to withdraw this advisor request.");
        }

        notification.setStatus(NotificationStatus.REVOKED);
        notificationRepository.save(notification);
    }
    /**
     * getNotifications (Enhanced Version)
     * Supports filtering by UNREAD (readStatus=false), READ (readStatus=true), or ALL.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDto> getNotifications(Long userId, String readStatus, Pageable pageable) {
        if ("UNREAD".equalsIgnoreCase(readStatus)) {
            return notificationRepository.findByToUser_UserIdAndReadStatus(userId, false, pageable)
                    .map(this::mapToDto);
        }
        if ("READ".equalsIgnoreCase(readStatus)) {
            return notificationRepository.findByToUser_UserIdAndReadStatus(userId, true, pageable)
                    .map(this::mapToDto);
        }
        if ("ALL".equalsIgnoreCase(readStatus)) {
            return notificationRepository.findByToUser_UserId(userId, pageable)
                    .map(this::mapToDto);
        }
        throw new BadRequestException("Invalid readStatus. Supported values: UNREAD, READ, or ALL.");
    }

    /**
     * markAsRead (PATCH /notifications/{id}/read)
     */
    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification not found."));

        if (!notification.getToUser().getUserId().equals(userId)) {
            throw new ForbiddenException("You are not authorized to access this notification.");
        }

        notification.setReadStatus(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void clearNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BadRequestException("Notification not found."));


        if (!notification.getToUser().getUserId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to clear this notification.");
        }

        notification.setStatus(NotificationStatus.CLEARED);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void clearAllNotifications(Long userId) {
        notificationRepository.deleteByToUser_UserId(userId);
    }

    @Override
    @Transactional
    public void respondToNotification(Long notificationId, String decision, Long userId) {

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BadRequestException("Notification not found."));

        if (!notification.getToUser().getUserId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to respond to this request.");
        }

        if (notification.getStatus() != NotificationStatus.PENDING) {
            throw new BadRequestException("This notification has already been responded to.");
        }

        if ("accept".equalsIgnoreCase(decision)) {
            notification.setStatus(NotificationStatus.ACCEPTED);
            if (notification.getType() == NotificationType.MEMBERSHIP_INVITE
                    && notification.getGroupId() != null) {
                memberService.addMember(notification.getGroupId(), userId);
            }
        } else if ("reject".equalsIgnoreCase(decision)) {
            notification.setStatus(NotificationStatus.REJECTED);
        } else {
            throw new BadRequestException("Invalid decision value! Only 'accept' or 'reject' are supported.");
        }

        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void sendGroupDisbandedNotification(Long groupId, Long actorUserId, String groupName, List<Long> memberIds) {
        User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new BadRequestException("User not found."));

        for (Long memberId : memberIds) {
            User recipient = userRepository.findById(memberId)
                    .orElseThrow(() -> new BadRequestException("Notification recipient not found."));

            Notification notification = new Notification();
            notification.setType(NotificationType.GROUP_DISBANDED);
            notification.setStatus(NotificationStatus.PENDING);
            notification.setMessage("Group '" + groupName + "' has been disbanded.");
            notification.setGroupId(groupId);
            notification.setFromUser(actor);
            notification.setToUser(recipient);

            notificationRepository.save(notification);
        }
    }


    @Override
    @Transactional
    public Notification createSystemAlert(Long toUserId, String message, String alertType, String metadata) {
        User recipient = userRepository.findById(toUserId)
                .orElseThrow(() -> new BadRequestException("Notification recipient not found."));
        Notification n = new Notification();
        n.setType(NotificationType.SYSTEM_ALERT);
        n.setMessage(message + (metadata != null ? " | " + metadata : ""));
        n.setStatus(NotificationStatus.PENDING);
        n.setToUser(recipient);
        notificationRepository.save(n);
        return n;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getSystemAlertsByUserId(Long userId) {
        return notificationRepository.findByToUser_UserIdAndTypeOrderByCreatedAtDesc(userId, NotificationType.SYSTEM_ALERT);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean systemAlertExists(Long toUserId, String alertType) {
        return notificationRepository.existsByToUser_UserIdAndTypeAndStatusAndMessageContaining(
                toUserId, NotificationType.SYSTEM_ALERT, NotificationStatus.PENDING, alertType);
    }

    private NotificationDto mapToDto(Notification notif) {
        return new NotificationDto(
                notif.getId(),
                notif.getType().name().toLowerCase(),
                notif.getMessage(),
                notif.getStatus().name().toLowerCase(),
                notif.isReadStatus(),
                notif.getFromUser() != null ? notif.getFromUser().getUserId() : null,
                notif.getFromUser() != null ? notif.getFromUser().getFullName() : null,
                notif.getToUser() != null ? notif.getToUser().getUserId() : null,
                notif.getGroupId(),
                notif.getCreatedAt()
        );
    }
    @Override
    public void sendMembershipInvite(Long toUserId, Long groupId, String groupName) {
        log.info("Membership invite notification sent to user {} for group {}", toUserId, groupName);
    }
}
