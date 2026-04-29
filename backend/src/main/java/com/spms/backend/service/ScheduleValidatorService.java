package com.spms.backend.service;

import com.spms.backend.model.Group;
import com.spms.backend.model.GroupMember;
import com.spms.backend.model.GroupStatus;
import com.spms.backend.model.Schedule;
import com.spms.backend.repository.GroupMemberRepository;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.NotificationRepository;
import com.spms.backend.repository.ScheduleRepository;
import com.spms.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * P2.7 — Schedule Validator Background Service
 * Her saat başı çalışarak deadline'ları kontrol eder.
 */
@Service
public class ScheduleValidatorService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleValidatorService.class);
    private static final int MIN_GROUP_MEMBERS = 3;

    private final ScheduleRepository scheduleRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public ScheduleValidatorService(ScheduleRepository scheduleRepository,
                                    GroupRepository groupRepository,
                                    GroupMemberRepository groupMemberRepository,
                                    NotificationRepository notificationRepository,
                                    NotificationService notificationService,
                                    UserRepository userRepository) {
        this.scheduleRepository = scheduleRepository;
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    /**
     * Her saat başı çalışır: "0 0 * * * *"
     * Test için her dakika: "0 * * * * *"
     */
    @Scheduled(cron = "0 0 * * * *")
    public void checkDeadlines() {
        log.info("[ScheduleValidator] Deadline check started at {}", Instant.now());

        Optional<Schedule> scheduleOpt = scheduleRepository.findTopByOrderByIdDesc();
        if (scheduleOpt.isEmpty()) {
            log.info("[ScheduleValidator] No schedule configured. Skipping.");
            return;
        }

        Schedule schedule = scheduleOpt.get();
        Instant now = Instant.now();

        // 1. Grup oluşturma deadline kontrolü
        if (now.isAfter(schedule.getGroupFormationDeadline())) {
            log.info("[ScheduleValidator] Group formation deadline has passed.");
            handleGroupFormationDeadline();
        }

        // 2. Danışman atama deadline kontrolü
        if (now.isAfter(schedule.getAdvisorAssignmentDeadline())) {
            log.info("[ScheduleValidator] Advisor assignment deadline has passed.");
            handleAdvisorAssignmentDeadline();
        }
    }

    // ── Grup Oluşturma Deadline Geçince ──────────────────────────────────

    @Transactional
    void handleGroupFormationDeadline() {
        Long coordinatorId = findCoordinatorId();
        if (coordinatorId == null) {
            log.warn("[ScheduleValidator] No coordinator found in database.");
            return;
        }

        // Aynı uyarı zaten varsa tekrar oluşturma
        if (notificationService.systemAlertExists(coordinatorId, "formation_deadline_missed")) {
            log.info("[ScheduleValidator] Formation deadline alert already exists. Skipping.");
            return;
        }

        // Yetersiz üyeli FORMING/FORMED grupları bul — deadline geçti, disband et
        List<Group> activeGroups = new ArrayList<>(groupRepository.findByStatus(GroupStatus.FORMING));
        activeGroups.addAll(groupRepository.findByStatus(GroupStatus.FORMED));
        long disbandedCount = 0;

        for (Group g : activeGroups) {
            if (g.getMembers().size() < MIN_GROUP_MEMBERS) {
                // disb_f1 + disb_f3: üyeleri sil, roller sıfırla
                disbandGroupAutomatically(g, coordinatorId);
                disbandedCount++;
            }
        }

        String msg = "Group formation deadline has passed. "
                + disbandedCount + " incomplete group(s) automatically disbanded.";
        notificationService.createSystemAlert(coordinatorId, msg,
                "formation_deadline_missed", null);

        log.info("[AUDIT] formation deadline: {} group(s) disbanded automatically.", disbandedCount);
    }

    // ── Danışman Atama Deadline Geçince ──────────────────────────────────

    @Transactional
    void handleAdvisorAssignmentDeadline() {
        Long coordinatorId = findCoordinatorId();
        if (coordinatorId == null) {
            log.warn("[ScheduleValidator] No coordinator found in database.");
            return;
        }

        if (notificationService.systemAlertExists(coordinatorId, "advisor_deadline_missed")) {
            log.info("[ScheduleValidator] Advisor deadline alert already exists. Skipping.");
            return;
        }

        // Danışman atanmamış, disbanded olmayan grupları bul — disb_f1 + disb_f3 uygula
        List<Group> unadvisedGroups = groupRepository
                .findByAdvisorIsNullAndStatusNot(GroupStatus.DISBANDED);

        for (Group g : unadvisedGroups) {
            disbandGroupAutomatically(g, coordinatorId);
        }

        String msg = "Advisor assignment deadline has passed. "
                + unadvisedGroups.size() + " group(s) without an advisor automatically disbanded.";
        notificationService.createSystemAlert(coordinatorId, msg,
                "advisor_deadline_missed", null);

        log.info("[AUDIT] advisor deadline: {} group(s) disbanded automatically.", unadvisedGroups.size());
    }

    /**
     * disb_f1: üye kayıtlarını sil, grubu DISBANDED yap
     * disb_f3: her üyenin rolünü 'student' olarak sıfırla
     * Manuel disbandGroup() ile aynı akışı izler.
     */
    @Transactional
    void disbandGroupAutomatically(Group group, Long actorId) {
        List<GroupMember> members = new ArrayList<>(group.getMembers());
        List<Long> memberIds = members.stream()
                .map(m -> m.getUser().getUserId())
                .toList();

        // disb_f3 — öğrenci rollerini sıfırla
        for (GroupMember member : members) {
            member.getUser().setRole("student");
            userRepository.save(member.getUser());
        }

        // disb_f1 — üye kayıtlarını sil, grubu disbanded yap
        groupMemberRepository.deleteAll(members);
        group.setStatus(GroupStatus.DISBANDED);
        group.setUpdatedAt(Instant.now());
        groupRepository.save(group);

        // Üyelere bildirim gönder
        try {
            notificationService.sendGroupDisbandedNotification(
                    group.getId(), actorId, group.getGroupName(), memberIds);
        } catch (Exception ex) {
            log.warn("[ScheduleValidator] Failed to send disband notification for group {}: {}",
                    group.getId(), ex.getMessage());
        }

        log.info("[AUDIT] disbandGroupAutomatically: groupId={} groupName='{}' memberCount={}",
                group.getId(), group.getGroupName(), memberIds.size());
    }

    // ── Koordinatörün ID'sini bul ─────────────────────────────────────────

    private Long findCoordinatorId() {
        return userRepository.findAllByRoleIgnoreCase("coordinator")
                .stream()
                .findFirst()
                .map(u -> u.getUserId())
                .orElse(null);
    }
}
