package com.spms.backend.service;

import com.spms.backend.model.Group;
import com.spms.backend.model.GroupStatus;
import com.spms.backend.model.Schedule;
import com.spms.backend.repository.GroupRepository;
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
 * Process 4.9 — Disband Unassigned Groups (DFD flow disb_f1).
 *
 * Advisor assignment deadline geçtiğinde, advisor ataması yapılmamış
 * (advisor == null) grupları otomatik olarak disband eder.
 * Manual tetikleme için aynı mantık coordinator endpoint'i tarafından
 * da kullanılabilir (ileride eklenirse).
 */
@Service
public class AdvisorDeadlineDisbandService {

    private static final Logger log = LoggerFactory.getLogger(AdvisorDeadlineDisbandService.class);
    private static final String COORDINATOR_ROLE = "coordinator";

    private final ScheduleRepository scheduleRepository;
    private final GroupRepository groupRepository;
    private final GroupService groupService;
    private final UserRepository userRepository;

    public AdvisorDeadlineDisbandService(ScheduleRepository scheduleRepository,
                                         GroupRepository groupRepository,
                                         GroupService groupService,
                                         UserRepository userRepository) {
        this.scheduleRepository = scheduleRepository;
        this.groupRepository = groupRepository;
        this.groupService = groupService;
        this.userRepository = userRepository;
    }

    /**
     * Saat başı çalışır — ScheduleValidatorService ile aynı cadence.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void disbandUnassignedGroups() {
        log.info("[AdvisorDeadlineDisband] Cron started at {}", Instant.now());

        Optional<Schedule> scheduleOpt = scheduleRepository.findTopByOrderByIdDesc();
        if (scheduleOpt.isEmpty()) {
            log.info("[AdvisorDeadlineDisband] No schedule configured. Skipping.");
            return;
        }

        Schedule schedule = scheduleOpt.get();
        if (!Instant.now().isAfter(schedule.getAdvisorAssignmentDeadline())) {
            log.info("[AdvisorDeadlineDisband] Advisor assignment deadline not yet reached. Skipping.");
            return;
        }

        Long coordinatorId = findCoordinatorId();
        if (coordinatorId == null) {
            log.warn("[AdvisorDeadlineDisband] No coordinator found; cannot perform disbandment.");
            return;
        }

        List<Group> unassignedGroups = groupRepository
                .findByAdvisorIsNullAndStatusNot(GroupStatus.DISBANDED);

        if (unassignedGroups.isEmpty()) {
            log.info("[AdvisorDeadlineDisband] No unassigned groups found.");
            return;
        }

        int disbanded = 0;
        int failed = 0;
        for (Group group : unassignedGroups) {
            Long groupId = group.getId();
            try {
                groupService.disbandGroup(groupId, coordinatorId, COORDINATOR_ROLE);
                disbanded++;
            } catch (Exception ex) {
                failed++;
                log.warn("[AdvisorDeadlineDisband] Failed to disband group {}: {}",
                        groupId, ex.getMessage());
            }
        }

        log.info("[AdvisorDeadlineDisband] Completed. Disbanded={}, Failed={}, Total={}",
                disbanded, failed, unassignedGroups.size());
    }

    private Long findCoordinatorId() {
        return userRepository.findAllByRole(COORDINATOR_ROLE)
                .stream()
                .findFirst()
                .map(u -> u.getUserId())
                .orElse(null);
    }
}
