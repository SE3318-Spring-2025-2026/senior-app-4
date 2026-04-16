package com.spms.backend.service;

import com.spms.backend.model.Group;
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

import java.time.Instant;
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

    private void handleGroupFormationDeadline() {
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

        // Yetersiz üyeli aktif grupları bul ve 'incomplete' yap
        List<Group> activeGroups = groupRepository.findByStatus(com.spms.backend.model.GroupStatus.FORMED);
        long incompleteCount = 0;
        for (Group g : activeGroups) {
            long memberCount = g.getMembers().size();
            if (memberCount < MIN_GROUP_MEMBERS) {
                g.setStatus(com.spms.backend.model.GroupStatus.FORMING);
                groupRepository.save(g);
                incompleteCount++;
            }
        }

        String msg = "Group formation deadline has passed. "
                + incompleteCount + " incomplete group(s) detected.";
        notificationService.createSystemAlert(coordinatorId, msg,
                "formation_deadline_missed", null);

        log.info("[ScheduleValidator] Formation deadline alert created. Incomplete groups: {}",
                incompleteCount);
    }

    // ── Danışman Atama Deadline Geçince ──────────────────────────────────

    private void handleAdvisorAssignmentDeadline() {
        Long coordinatorId = findCoordinatorId();
        if (coordinatorId == null) {
            log.warn("[ScheduleValidator] No coordinator found in database.");
            return;
        }

        if (notificationService.systemAlertExists(coordinatorId, "advisor_deadline_missed")) {
            log.info("[ScheduleValidator] Advisor deadline alert already exists. Skipping.");
            return;
        }

        // Danışman atanmamış, disbanded olmayan grupları bul
        List<Group> unadvisedGroups = groupRepository
                .findByAdvisorIsNullAndStatusNot(com.spms.backend.model.GroupStatus.DISBANDED);

        // Bu grupları 'advisor_needed' statüsüne al
        for (Group g : unadvisedGroups) {
            g.setStatus(com.spms.backend.model.GroupStatus.FORMING);
            groupRepository.save(g);
        }

        String msg = "Advisor assignment deadline has passed. "
                + unadvisedGroups.size() + " group(s) without an advisor.";
        notificationService.createSystemAlert(coordinatorId, msg,
                "advisor_deadline_missed", null);

        log.info("[ScheduleValidator] Advisor deadline alert created. Unadvised groups: {}",
                unadvisedGroups.size());
    }

    // ── Koordinatörün ID'sini bul ─────────────────────────────────────────

    private Long findCoordinatorId() {
        return userRepository.findAllByRole("coordinator")
                .stream()
                .findFirst()
                .map(u -> u.getUserId())
                .orElse(null);
    }
}
