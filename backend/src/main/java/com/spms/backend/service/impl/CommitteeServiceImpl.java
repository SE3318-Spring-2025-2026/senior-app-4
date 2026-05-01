package com.spms.backend.service.impl;

import com.spms.backend.dto.request.CommitteeCreateRequest;
import com.spms.backend.exception.NotFoundException;
import com.spms.backend.model.ActionType;
import com.spms.backend.model.AuditLog;
import com.spms.backend.model.Committee;
import com.spms.backend.model.enums.CommitteeStatus;
import com.spms.backend.repository.AuditLogRepository;
import com.spms.backend.repository.CommitteeRepository;
import com.spms.backend.service.CommitteeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class CommitteeServiceImpl implements CommitteeService {

    private final CommitteeRepository committeeRepository;
    private final AuditLogRepository auditLogRepository;

    public CommitteeServiceImpl(CommitteeRepository committeeRepository, AuditLogRepository auditLogRepository) {
        this.committeeRepository = committeeRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public Committee createCommittee(CommitteeCreateRequest request, Long coordinatorId) {
        CommitteeStatus status = request.getStatus() != null ? request.getStatus() : CommitteeStatus.ACTIVE;
        Committee committee = new Committee(
                request.getCommitteeName(),
                request.getDescription(),
                status,
                coordinatorId
        );

        Committee saved = committeeRepository.save(committee);

        logAction(ActionType.COMMITTEE_CREATED, coordinatorId, saved.getCommitteeId(), "Committee created");

        return saved;
    }

    @Override
    public Committee updateCommittee(Long id, CommitteeCreateRequest request, Long coordinatorId) {
        Committee committee = getCommitteeById(id);
        committee.setCommitteeName(request.getCommitteeName());
        committee.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            committee.setStatus(request.getStatus());
        }
        committee.setUpdatedAt(Instant.now());

        Committee saved = committeeRepository.save(committee);

        logAction(ActionType.COMMITTEE_UPDATED, coordinatorId, saved.getCommitteeId(), "Committee updated");

        return saved;
    }

    @Override
    public void deleteCommittee(Long id, Long coordinatorId) {
        Committee committee = getCommitteeById(id);
        logAction(ActionType.COMMITTEE_DELETED, coordinatorId, id, "Committee deleted");
        committeeRepository.delete(committee);
    }

    @Override
    public Committee getCommitteeById(Long id) {
        return committeeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Committee not found with id " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Committee getCommitteeByIdWithFullDetails(Long id) {
        return committeeRepository.findWithFullDetails(id)
                .orElseThrow(() -> new NotFoundException("Committee not found with id " + id));
    }

    @Override
    public List<Committee> getAllCommittees() {
        return committeeRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Committee> getAllCommitteesPaginated(Pageable pageable) {
        return committeeRepository.findAllWithDetails(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Committee> getCommitteesByStatus(CommitteeStatus status, Pageable pageable) {
        return committeeRepository.findByStatusWithDetails(status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Committee> getCommitteesByCoordinator(Long coordinatorId, Pageable pageable) {
        return committeeRepository.findByCreatedByWithDetails(coordinatorId, pageable);
    }

    private void logAction(ActionType actionType, Long userId, Long committeeId, String details) {
        AuditLog log = new AuditLog();
        log.setActionType(actionType);
        log.setUserId(userId);
        log.setEventDetails(details + " (Committee ID: " + committeeId + ")");
        // Notice we don't set GroupId because this is a committee action.
        auditLogRepository.save(log);
    }
}
