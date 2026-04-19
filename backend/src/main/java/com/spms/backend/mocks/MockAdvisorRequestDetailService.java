package com.spms.backend.mocks;

import com.spms.backend.dto.response.AdvisorRequestDetailDto;
import com.spms.backend.exception.ForbiddenException;
import com.spms.backend.exception.NotFoundException;
import com.spms.backend.model.AdvisorRequest;
import com.spms.backend.model.Group;
import com.spms.backend.repository.AdvisorRequestRepository;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.service.AdvisorRequestDetailService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// TODO(P2-dep:#14-22): uses MockAdvisorRequestDetailService until P2 advisor-request workflow merges.
//   After merge, replace with a real @Service impl and delete this class. See DEPENDENCIES.md.
@Service
@Profile("dev-mock")
public class MockAdvisorRequestDetailService implements AdvisorRequestDetailService {

    private final AdvisorRequestRepository advisorRequestRepository;
    private final GroupRepository groupRepository;

    public MockAdvisorRequestDetailService(AdvisorRequestRepository advisorRequestRepository,
                                           GroupRepository groupRepository) {
        this.advisorRequestRepository = advisorRequestRepository;
        this.groupRepository = groupRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AdvisorRequestDetailDto getDetail(Long requestId, Long callerId, String callerRole) {
        AdvisorRequest request = advisorRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Advisor request not found: " + requestId));

        Long professorId = request.getProfessor().getUserId();
        boolean isTargetProfessor = callerId.equals(professorId);
        boolean isCoordinator = "COORDINATOR".equalsIgnoreCase(callerRole);

        if (!isTargetProfessor && !isCoordinator) {
            throw new ForbiddenException("Access denied: only the targeted professor or a coordinator may view this request.");
        }

        Group group = request.getGroup();
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
                request.getProfessor().getUserId(),
                request.getProfessor().getFullName(),
                request.getProfessor().getEmail(),
                currentAdviseeCount
        );

        return new AdvisorRequestDetailDto(
                request.getId(),
                request.getStatus().name(),
                request.getMessage(),
                request.getRequestedAt(),
                request.getDecidedAt(),
                request.getReason(),
                teamDto,
                professorDto
        );
    }
}
