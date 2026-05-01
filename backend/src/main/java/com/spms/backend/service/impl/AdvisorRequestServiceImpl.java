package com.spms.backend.service.impl;

import com.spms.backend.dto.response.AdvisorRequestSummaryDto;
import com.spms.backend.model.Group;
import com.spms.backend.model.notification.Notification;
import com.spms.backend.model.notification.NotificationType;
import com.spms.backend.repository.GroupMemberRepository;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.NotificationRepository;
import com.spms.backend.service.AdvisorRequestService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdvisorRequestServiceImpl implements AdvisorRequestService {

    private final NotificationRepository notificationRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;

    public AdvisorRequestServiceImpl(NotificationRepository notificationRepository,
                                     GroupRepository groupRepository,
                                     GroupMemberRepository groupMemberRepository) {
        this.notificationRepository = notificationRepository;
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdvisorRequestSummaryDto> listAdvisorRequests(Long userId, String role, String status, Long teamId, Long professorId) {
        String userRole = (role != null) ? role.toLowerCase() : "guest";
        List<Notification> notifications;

        switch (userRole) {
            case "professor":
                notifications = notificationRepository.findByToUser_UserIdAndTypeOrderByCreatedAtDesc(userId, NotificationType.ADVISOR_REQUEST);
                break;
            case "student":
                var membership = groupMemberRepository.findFirstByUser_UserIdOrderByJoinedAtDesc(userId);
                if (membership.isEmpty()) {
                    return Collections.emptyList();
                }
                Long groupId = membership.get().getGroup().getId();
                notifications = notificationRepository.findByGroupIdAndTypeOrderByCreatedAtDesc(groupId, NotificationType.ADVISOR_REQUEST);
                break;
            case "coordinator":
                notifications = notificationRepository.findByTypeOrderByCreatedAtDesc(NotificationType.ADVISOR_REQUEST);
                break;
            default:
                return Collections.emptyList();
        }

        return notifications.stream()
                .filter(n -> teamId == null || (n.getGroupId() != null && n.getGroupId().equals(teamId)))
                .filter(n -> professorId == null || (n.getToUser() != null && n.getToUser().getUserId().equals(professorId)))
                .map(n -> {
                    String mappedStatus;
                    switch (n.getStatus()) {
                        case ACCEPTED: mappedStatus = "APPROVED"; break;
                        case CLEARED: mappedStatus = "WITHDRAWN"; break;
                        case REJECTED: mappedStatus = "REJECTED"; break;
                        default: mappedStatus = "PENDING"; break;
                    }

                    Group group = null;
                    if (n.getGroupId() != null) {
                        group = groupRepository.findById(n.getGroupId()).orElse(null);
                    }
                    String teamName = (group != null) ? group.getGroupName() : "Unknown Team";
                    Long gId = (group != null) ? group.getId() : n.getGroupId();
                    Long pId = (n.getToUser() != null) ? n.getToUser().getUserId() : null;
                    String pName = (n.getToUser() != null) ? n.getToUser().getFullName() : "Unknown Professor";

                    return new AdvisorRequestSummaryDto(
                            n.getId(),
                            gId,
                            teamName,
                            pId,
                            pName,
                            mappedStatus,
                            n.getCreatedAt(),
                            null
                    );
                })
                .filter(dto -> status == null || dto.status().equalsIgnoreCase(status))
                .collect(Collectors.toList());
    }
}
