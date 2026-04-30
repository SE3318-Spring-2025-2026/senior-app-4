package com.spms.backend.service.impl;

import com.spms.backend.dto.request.CommitteeNotifyRequestDto;
import com.spms.backend.dto.response.CommitteeNotificationResponse;
import com.spms.backend.dto.response.NotificationDto;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.exception.ForbiddenException;
import com.spms.backend.exception.NotFoundException;
import com.spms.backend.model.Committee;
import com.spms.backend.model.CommitteeAdvisor;
import com.spms.backend.model.CommitteeJury;
import com.spms.backend.model.User;
import com.spms.backend.model.notification.Notification;
import com.spms.backend.model.notification.NotificationStatus;
import com.spms.backend.model.notification.NotificationType;
import com.spms.backend.repository.CommitteeRepository;
import com.spms.backend.repository.NotificationRepository;
import com.spms.backend.repository.UserRepository;
import com.spms.backend.service.CommitteeNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * P5.7 — CommitteeNotificationServiceImpl
 * Stores notifications to D10 and dispatches them via WebSocket.
 */
@Service
public class CommitteeNotificationServiceImpl implements CommitteeNotificationService {

    private static final Logger log = LoggerFactory.getLogger(CommitteeNotificationServiceImpl.class);

    private final CommitteeRepository committeeRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public CommitteeNotificationServiceImpl(CommitteeRepository committeeRepository,
                                            NotificationRepository notificationRepository,
                                            UserRepository userRepository,
                                            SimpMessagingTemplate messagingTemplate) {
        this.committeeRepository = committeeRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/v1/committees/{committeeId}/notify
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public CommitteeNotificationResponse sendCommitteeNotification(Long committeeId,
                                                                    CommitteeNotifyRequestDto request,
                                                                    Long senderUserId) {
        // 1. Committee var mı?
        Committee committee = committeeRepository.findById(committeeId)
                .orElseThrow(() -> new NotFoundException("Committee not found with id: " + committeeId));

        // 2. Sender var mı?
        User sender = userRepository.findById(senderUserId)
                .orElseThrow(() -> new NotFoundException("Sender user not found."));

        // 3. notificationType enum validation
        NotificationType notifType = parseNotificationType(request.notificationType());

        // 4. Alıcıları belirle
        List<User> recipients = resolveRecipients(committee, request.recipients());
        if (recipients.isEmpty()) {
            throw new BadRequestException("No recipients found for this committee.");
        }

        // 5. Her alıcı için D10'a kaydet + WebSocket push
        Long firstId = null;
        for (User recipient : recipients) {
            Notification notif = buildNotification(sender, recipient, notifType, request.message(), committeeId, null);
            Notification saved = notificationRepository.save(notif);
            if (firstId == null) firstId = saved.getId();
            pushWebSocket(recipient.getUserId(), mapToDto(saved));
        }

        log.info("[P5.7] Committee notification sent: committeeId={} type={} recipients={}",
                committeeId, notifType, recipients.size());

        return new CommitteeNotificationResponse(firstId, "sent", recipients.size(), Instant.now());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/committees/{committeeId}/notifications
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> getCommitteeNotifications(Long committeeId) {
        committeeRepository.findById(committeeId)
                .orElseThrow(() -> new NotFoundException("Committee not found with id: " + committeeId));

        return notificationRepository.findByCommitteeIdOrderByCreatedAtDesc(committeeId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/committees/notifications  (caller'ın aldıkları)
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> getMyCommitteeNotifications(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

        return notificationRepository
                .findByToUser_UserIdAndCommitteeIdIsNotNullOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // P5.2 — Advisor atandı trigger
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void notifyAdvisorAssignment(Long committeeId, Long assignedUserId, Long actorUserId) {
        Committee committee = committeeRepository.findById(committeeId).orElse(null);
        if (committee == null) return;

        User actor = userRepository.findById(actorUserId).orElse(null);
        User assignedUser = userRepository.findById(assignedUserId).orElse(null);
        if (actor == null || assignedUser == null) return;

        String msg = actor.getFullName() + " assigned " + assignedUser.getFullName()
                + " as advisor to committee '" + committee.getCommitteeName() + "'.";

        List<User> members = resolveRecipients(committee, null);
        for (User member : members) {
            Notification notif = buildNotification(actor, member, NotificationType.ADVISOR_ASSIGNMENT,
                    msg, committeeId, null);
            Notification saved = notificationRepository.save(notif);
            pushWebSocket(member.getUserId(), mapToDto(saved));
        }
        log.info("[P5.2->P5.7] ADVISOR_ASSIGNMENT notified: committeeId={} assignedUser={}", committeeId, assignedUserId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // P5.3 — Jury atandı trigger
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void notifyJuryAssignment(Long committeeId, Long assignedUserId, Long actorUserId) {
        Committee committee = committeeRepository.findById(committeeId).orElse(null);
        if (committee == null) return;

        User actor = userRepository.findById(actorUserId).orElse(null);
        User assignedUser = userRepository.findById(assignedUserId).orElse(null);
        if (actor == null || assignedUser == null) return;

        String msg = actor.getFullName() + " assigned " + assignedUser.getFullName()
                + " as jury member to committee '" + committee.getCommitteeName() + "'.";

        List<User> members = resolveRecipients(committee, null);
        for (User member : members) {
            Notification notif = buildNotification(actor, member, NotificationType.JURY_ASSIGNMENT,
                    msg, committeeId, null);
            Notification saved = notificationRepository.save(notif);
            pushWebSocket(member.getUserId(), mapToDto(saved));
        }
        log.info("[P5.3->P5.7] JURY_ASSIGNMENT notified: committeeId={} assignedUser={}", committeeId, assignedUserId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // P5.4 — Group atandı trigger
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void notifyGroupAssignment(Long committeeId, Long groupId, Long actorUserId) {
        Committee committee = committeeRepository.findById(committeeId).orElse(null);
        if (committee == null) return;

        User actor = userRepository.findById(actorUserId).orElse(null);
        if (actor == null) return;

        String msg = actor.getFullName() + " assigned group #" + groupId
                + " to committee '" + committee.getCommitteeName() + "'.";

        List<User> members = resolveRecipients(committee, null);
        for (User member : members) {
            Notification notif = buildNotification(actor, member, NotificationType.GROUP_ASSIGNMENT,
                    msg, committeeId, groupId);
            Notification saved = notificationRepository.save(notif);
            pushWebSocket(member.getUserId(), mapToDto(saved));
        }
        log.info("[P5.4->P5.7] GROUP_ASSIGNMENT notified: committeeId={} groupId={}", committeeId, groupId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Alıcıları belirle: boşsa tüm committee üyeleri (advisor + jury).
     */
    private List<User> resolveRecipients(Committee committee, List<Long> recipientIds) {
        if (recipientIds != null && !recipientIds.isEmpty()) {
            return recipientIds.stream()
                    .map(id -> userRepository.findById(id)
                            .orElseThrow(() -> new NotFoundException("User not found: " + id)))
                    .collect(Collectors.toList());
        }

        // Default: tüm committee advisorları + jury üyeleri
        List<User> all = new ArrayList<>();
        if (committee.getAdvisors() != null) {
            committee.getAdvisors().stream()
                    .map(CommitteeAdvisor::getAdvisor)
                    .filter(u -> u != null)
                    .forEach(all::add);
        }
        if (committee.getJuryMembers() != null) {
            committee.getJuryMembers().stream()
                    .map(CommitteeJury::getJuryMember)
                    .filter(u -> u != null)
                    .forEach(all::add);
        }
        return all;
    }

    private Notification buildNotification(User from, User to, NotificationType type,
                                           String message, Long committeeId, Long groupId) {
        Notification n = new Notification();
        n.setFromUser(from);
        n.setToUser(to);
        n.setType(type);
        n.setMessage(message);
        n.setStatus(NotificationStatus.PENDING);
        n.setCommitteeId(committeeId);
        n.setGroupId(groupId);
        return n;
    }

    /**
     * WebSocket push: /user/{userId}/queue/notifications
     */
    private void pushWebSocket(Long toUserId, NotificationDto dto) {
        try {
            messagingTemplate.convertAndSendToUser(
                    toUserId.toString(),
                    "/queue/notifications",
                    dto
            );
        } catch (Exception e) {
            log.warn("[P5.7] WebSocket push failed for userId={}: {}", toUserId, e.getMessage());
        }
    }

    private NotificationType parseNotificationType(String raw) {
        try {
            return NotificationType.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid notificationType: '" + raw
                    + "'. Allowed: ADVISOR_ASSIGNMENT, JURY_ASSIGNMENT, GROUP_ASSIGNMENT, "
                    + "SCHEDULE_CHANGE, MEETING_REMINDER, GENERAL");
        }
    }

    private NotificationDto mapToDto(Notification n) {
        return new NotificationDto(
                n.getId(),
                n.getType().name().toLowerCase(java.util.Locale.ENGLISH),
                n.getMessage(),
                n.getStatus().name().toLowerCase(java.util.Locale.ENGLISH),
                n.isReadStatus(),
                n.getFromUser() != null ? n.getFromUser().getUserId() : null,
                n.getFromUser() != null ? n.getFromUser().getFullName() : null,
                n.getToUser() != null ? n.getToUser().getUserId() : null,
                n.getGroupId(),
                n.getCreatedAt()
        );
    }
}
