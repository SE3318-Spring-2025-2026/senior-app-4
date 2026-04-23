package com.spms.backend.service.impl;

import com.spms.backend.dto.response.AdvisorRequestDetailDto;
import com.spms.backend.exception.ForbiddenException;
import com.spms.backend.exception.NotFoundException;
import com.spms.backend.model.Group;
import com.spms.backend.model.notification.Notification;
import com.spms.backend.model.notification.NotificationStatus;
import com.spms.backend.model.notification.NotificationType;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.NotificationRepository;
import com.spms.backend.service.AdvisorRequestDetailService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdvisorRequestDetailServiceImpl implements AdvisorRequestDetailService {

    private final NotificationRepository notificationRepository;
    private final GroupRepository groupRepository;

    public AdvisorRequestDetailServiceImpl(NotificationRepository notificationRepository,
                                           GroupRepository groupRepository) {
        this.notificationRepository = notificationRepository;
        this.groupRepository = groupRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AdvisorRequestDetailDto getDetail(Long requestId, Long callerId, String callerRole) {
        Notification notification = notificationRepository.findById(requestId)
                .filter(n -> n.getType() == NotificationType.ADVISOR_REQUEST)
                .orElseThrow(() -> new NotFoundException("Advisor request not found: " + requestId));

        Long professorId = notification.getToUser().getUserId();
        boolean isTargetProfessor = callerId.equals(professorId);
        boolean isCoordinator = "COORDINATOR".equalsIgnoreCase(callerRole);

        if (!isTargetProfessor && !isCoordinator) {
            throw new ForbiddenException("Access denied: only the targeted professor or a coordinator may view this request.");
        }

        Group group = groupRepository.findById(notification.getGroupId())
                .orElseThrow(() -> new NotFoundException("Group not found: " + notification.getGroupId()));

        long currentAdviseeCount = groupRepository.countByAdvisor_UserId(professorId);

        AdvisorRequestDetailDto.TeamDto teamDto = new AdvisorRequestDetailDto.TeamDto(
                group.getId(),
                group.getGroupName(),
                group.getMembers().size(),
                group.getLeader().getFullName(),
                group.getLeader().getUserId(),
                group.getAdvisor() != null
        );

        AdvisorRequestDetailDto.ProfessorDto professorDto = new AdvisorRequestDetailDto.ProfessorDto(
                notification.getToUser().getUserId(),
                notification.getToUser().getFullName(),
                notification.getToUser().getEmail(),
                currentAdviseeCount
        );

        return new AdvisorRequestDetailDto(
                notification.getId(),
                mapStatus(notification.getStatus()),
                notification.getMessage(),
                notification.getCreatedAt(),
                null,   // decidedAt — not tracked in Notification
                null,   // reason — not tracked in Notification
                teamDto,
                professorDto
        );
    }

    private String mapStatus(NotificationStatus status) {
        return switch (status) {
            case PENDING  -> "PENDING";
            case ACCEPTED -> "APPROVED";
            case REJECTED -> "REJECTED";
            case CLEARED  -> "WITHDRAWN";
        };
    }
}
