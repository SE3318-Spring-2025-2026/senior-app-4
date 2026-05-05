package com.spms.backend.service;

import com.spms.backend.dto.request.ScheduleUpdateRequest;
import com.spms.backend.dto.response.ScheduleResponse;
import com.spms.backend.dto.response.SystemAlertResponse;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.model.Group;
import com.spms.backend.model.notification.Notification;
import com.spms.backend.model.Schedule;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.GroupMemberRepository;
import com.spms.backend.repository.ScheduleRepository;
import com.spms.backend.dto.response.SystemAlertResponse.AffectedGroup;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final NotificationService notificationService;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;

    public ScheduleService(ScheduleRepository scheduleRepository,
                           NotificationService notificationService,
                           GroupRepository groupRepository,
                           GroupMemberRepository groupMemberRepository) {
        this.scheduleRepository = scheduleRepository;
        this.notificationService = notificationService;
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
    }

    /**
     * D11'den en güncel deadline konfigürasyonunu döner.
     * Kayıt yoksa 404 fırlatır.
     */
    public ScheduleResponse getLatestSchedule() {
        Schedule s = scheduleRepository.findTopByOrderByIdDesc()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Schedule not initialized."));
        return toDto(s);
    }

    /**
     * Koordinatör tarafından deadline ayarlar/günceller.
     * Geçmiş tarih verilirse 400 döner.
     */
    public ScheduleResponse updateSchedule(ScheduleUpdateRequest req, Long coordinatorId) {
        Instant now = Instant.now();

        if (req.getGroupFormationDeadline().isBefore(now)) {
            throw new BadRequestException("groupFormationDeadline must be in the future.");
        }
        if (req.getAdvisorAssignmentDeadline().isBefore(now)) {
            throw new BadRequestException("advisorAssignmentDeadline must be in the future.");
        }

        Schedule s = scheduleRepository.findTopByOrderByIdDesc()
                .orElse(new Schedule());
        s.setGroupFormationDeadline(req.getGroupFormationDeadline());
        s.setAdvisorAssignmentDeadline(req.getAdvisorAssignmentDeadline());
        s.setUpdatedBy(coordinatorId);
        s.setUpdatedAt(now);

        Schedule saved = scheduleRepository.save(s);
        return toDto(saved);
    }

    /**
     * Koordinatörün sistem uyarılarını D10'dan okur ve
     * SystemAlertResponse listesine dönüştürür.
     */
    public List<SystemAlertResponse> getSystemAlerts(Long coordinatorId) {
        List<Notification> notifications = notificationService.getSystemAlertsByUserId(coordinatorId);
        List<SystemAlertResponse> result = new ArrayList<>();

        Schedule schedule = scheduleRepository.findTopByOrderByIdDesc().orElse(null);
        Instant now = Instant.now();

        for (Notification n : notifications) {
            // alertType'ı metadata'dan veya message'dan çıkar
            String alertType = resolveAlertType(n.getMessage());
            String deadlineDate = resolveDeadlineDate(alertType, schedule);
            long daysOverdue = 0;
            if (deadlineDate != null) {
                Instant dl = Instant.parse(deadlineDate);
                daysOverdue = ChronoUnit.DAYS.between(dl, now);
                if (daysOverdue < 0) daysOverdue = 0;
            }

            String suggestedAction = "formation_deadline_missed".equals(alertType)
                    ? "extend_deadline"
                    : "disband";

            List<AffectedGroup> affectedGroups = new ArrayList<>();
            if ("formation_deadline_missed".equals(alertType)) {
                List<Group> incGroups = groupRepository.findByStatus(com.spms.backend.model.GroupStatus.FORMING);
                for (Group g : incGroups) {
                    int count = g.getMembers().size();
                    affectedGroups.add(new AffectedGroup(g.getId(), g.getGroupName(), count));
                }
            } else if ("advisor_deadline_missed".equals(alertType)) {
                List<Group> advGroups = groupRepository.findByAdvisorIsNullAndStatusNot(com.spms.backend.model.GroupStatus.DISBANDED);
                for (Group g : advGroups) {
                    int count = g.getMembers().size();
                    affectedGroups.add(new AffectedGroup(g.getId(), g.getGroupName(), count));
                }
            }

            result.add(new SystemAlertResponse(
                    n.getId(),
                    alertType,
                    n.getMessage(),
                    deadlineDate,
                    now.toString(),
                    daysOverdue,
                    affectedGroups,
                    suggestedAction,
                    n.getCreatedAt() != null ? n.getCreatedAt().toString() : null
            ));
        }
        return result;
    }

    // ── Yardımcı Metodlar ─────────────────────────────────────────────────

    private ScheduleResponse toDto(Schedule s) {
        return new ScheduleResponse(
                s.getId(),
                s.getGroupFormationDeadline() != null
                        ? s.getGroupFormationDeadline().toString() : null,
                s.getAdvisorAssignmentDeadline() != null
                        ? s.getAdvisorAssignmentDeadline().toString() : null,
                s.getUpdatedAt() != null ? s.getUpdatedAt().toString() : null
        );
    }

    private String resolveAlertType(String message) {
        if (message == null) return "unknown";
        if (message.contains("formation")) return "formation_deadline_missed";
        if (message.contains("advisor")) return "advisor_deadline_missed";
        return "system_alert";
    }

    private String resolveDeadlineDate(String alertType, Schedule schedule) {
        if (schedule == null) return null;
        if ("formation_deadline_missed".equals(alertType)) {
            return schedule.getGroupFormationDeadline() != null
                    ? schedule.getGroupFormationDeadline().toString() : null;
        }
        if ("advisor_deadline_missed".equals(alertType)) {
            return schedule.getAdvisorAssignmentDeadline() != null
                    ? schedule.getAdvisorAssignmentDeadline().toString() : null;
        }
        return null;
    }
}
