package com.spms.backend.service.impl;

import com.spms.backend.dto.response.AdvisorAssignmentListResponse;
import com.spms.backend.dto.response.GroupAdvisorAssignmentDto;
import com.spms.backend.exception.ForbiddenException;
import com.spms.backend.model.Group;
import com.spms.backend.model.GroupStatus;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.service.AdvisorAssignmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * P4-ASSIGN-1 list logic.
 * TODO(P4-ASSIGN-2): expose {@code assignmentType} and accurate {@code assignedAt} once stored on group or derivable from audit.
 */
@Service
public class AdvisorAssignmentServiceImpl implements AdvisorAssignmentService {

    private final GroupRepository groupRepository;

    public AdvisorAssignmentServiceImpl(GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AdvisorAssignmentListResponse listAdvisorAssignments(
            String requesterRole, Long requesterUserId, Long filterAdvisorId, Boolean hasAdvisor) {

        if (requesterRole == null || requesterRole.isBlank()) {
            throw new ForbiddenException("Missing role.");
        }

        if ("coordinator".equalsIgnoreCase(requesterRole.trim())) {
            List<GroupAdvisorAssignmentDto> rows =
                    buildCoordinatorRows(filterAdvisorId, hasAdvisor);
            return new AdvisorAssignmentListResponse("success", rows);
        }

        if ("professor".equalsIgnoreCase(requesterRole.trim())) {
            List<GroupAdvisorAssignmentDto> rows = buildProfessorRows(requesterUserId, hasAdvisor);
            return new AdvisorAssignmentListResponse("success", rows);
        }

        throw new ForbiddenException("Only coordinators and professors can list advisor assignments.");
    }

    private List<GroupAdvisorAssignmentDto> buildCoordinatorRows(Long filterAdvisorId, Boolean hasAdvisor) {
        List<Group> groups =
                groupRepository.findAllNonDisbandedWithAdvisorAndLeaderFetched(GroupStatus.DISBANDED);

        return groups.stream()
                .filter(g -> matchesAdvisorIdFilter(g, filterAdvisorId))
                .filter(g -> matchesHasAdvisorFilter(g, hasAdvisor))
                .map(AdvisorAssignmentServiceImpl::toDto)
                .sorted(Comparator.comparing(GroupAdvisorAssignmentDto::groupId))
                .collect(Collectors.toList());
    }

    private List<GroupAdvisorAssignmentDto> buildProfessorRows(Long professorUserId, Boolean hasAdvisor) {
        if (professorUserId == null) {
            throw new ForbiddenException("Missing user context.");
        }

        List<Group> groups =
                groupRepository.findAllNonDisbandedWithAdvisorAndLeaderFetched(GroupStatus.DISBANDED);

        return groups.stream()
                .filter(g -> g.getAdvisor() != null && Objects.equals(g.getAdvisor().getUserId(), professorUserId))
                .filter(g -> matchesHasAdvisorFilter(g, hasAdvisor))
                .map(AdvisorAssignmentServiceImpl::toDto)
                .sorted(Comparator.comparing(GroupAdvisorAssignmentDto::groupId))
                .collect(Collectors.toList());
    }

    /**
     * Default (hasAdvisor null): only groups that have an advisor assigned (#160 acceptance).
     */
    private static boolean matchesHasAdvisorFilter(Group g, Boolean hasAdvisor) {
        if (Boolean.FALSE.equals(hasAdvisor)) {
            return g.getAdvisor() == null;
        }
        // null or true → assigned teams only
        return g.getAdvisor() != null;
    }

    private static boolean matchesAdvisorIdFilter(Group g, Long filterAdvisorId) {
        if (filterAdvisorId == null) {
            return true;
        }
        return g.getAdvisor() != null && filterAdvisorId.equals(g.getAdvisor().getUserId());
    }

    private static GroupAdvisorAssignmentDto toDto(Group g) {
        return new GroupAdvisorAssignmentDto(
                g.getId(),
                g.getGroupName(),
                g.getLeader() != null ? g.getLeader().getFullName() : "N/A",
                g.getAdvisor() != null ? g.getAdvisor().getUserId() : null,
                g.getAdvisor() != null ? g.getAdvisor().getFullName() : null,
                g.getStatus().name());
    }
}
