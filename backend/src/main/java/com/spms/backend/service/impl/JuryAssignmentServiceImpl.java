package com.spms.backend.service.impl;

import com.spms.backend.dto.response.JuryListResponse;
import com.spms.backend.dto.response.JuryMemberDto;
import com.spms.backend.exception.ConflictException;
import com.spms.backend.exception.NotFoundException;
import com.spms.backend.model.ActionType;
import com.spms.backend.model.AuditLog;
import com.spms.backend.model.Committee;
import com.spms.backend.model.CommitteeJury;
import com.spms.backend.model.User;
import com.spms.backend.repository.AuditLogRepository;
import com.spms.backend.repository.CommitteeJuryRepository;
import com.spms.backend.repository.CommitteeRepository;
import com.spms.backend.repository.UserRepository;
import com.spms.backend.service.CommitteeNotificationService;
import com.spms.backend.service.JuryAssignmentService;
import com.spms.backend.service.JuryValidator;
import com.spms.backend.service.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JuryAssignmentServiceImpl implements JuryAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(JuryAssignmentServiceImpl.class);

    private final CommitteeRepository committeeRepository;
    private final UserRepository userRepository;
    private final CommitteeJuryRepository committeeJuryRepository;
    private final AuditLogRepository auditLogRepository;
    private final JuryValidator juryValidator;
    private final CommitteeNotificationService committeeNotificationService;

    public JuryAssignmentServiceImpl(
            CommitteeRepository committeeRepository,
            UserRepository userRepository,
            CommitteeJuryRepository committeeJuryRepository,
            AuditLogRepository auditLogRepository,
            JuryValidator juryValidator,
            CommitteeNotificationService committeeNotificationService) {
        this.committeeRepository = committeeRepository;
        this.userRepository = userRepository;
        this.committeeJuryRepository = committeeJuryRepository;
        this.auditLogRepository = auditLogRepository;
        this.juryValidator = juryValidator;
        this.committeeNotificationService = committeeNotificationService;
    }

    @Override
    @Transactional
    public void assignJury(Long committeeId, Long juryMemberId, String juryType, Long assignedBy) {
        Committee committee = committeeRepository.findById(committeeId)
                .orElseThrow(() -> new NotFoundException("Committee not found"));

        User juryMember = userRepository.findById(juryMemberId)
                .orElseThrow(() -> new NotFoundException("Jury member not found"));

        // P5.5 validation: Check if jury member is qualified
        ValidationResult qualificationResult = juryValidator.isJuryQualified(juryMemberId);
        if (!qualificationResult.valid()) {
            throw new com.spms.backend.exception.BadRequestException(qualificationResult.reason());
        }

        // P5.5 validation: Check jury type
        ValidationResult juryTypeResult = juryValidator.validateJuryType(juryType);
        if (!juryTypeResult.valid()) {
            throw new com.spms.backend.exception.BadRequestException(juryTypeResult.reason());
        }

        // P5.5 validation: Check for duplicate assignment
        ValidationResult duplicateResult = juryValidator.isDuplicateAssignment(committeeId, juryMemberId);
        if (!duplicateResult.valid()) {
            throw new ConflictException(duplicateResult.reason());
        }

        // Enforce max jury limit (matches ValidationService rules: max 2)
        if (committee.getJuryMembers() != null && committee.getJuryMembers().size() >= 2) {
            throw new com.spms.backend.exception.BadRequestException("Committee cannot have more than 2 jury members");
        }

        CommitteeJury committeeJury = new CommitteeJury(committee, juryMember, juryType.toUpperCase(), assignedBy);
        committee.getJuryMembers().add(committeeJury);
        committeeRepository.save(committee);

        AuditLog auditLog = new AuditLog();
        auditLog.setActionType(ActionType.JURY_ASSIGNED);
        auditLog.setUserId(assignedBy);
        auditLog.setEventDetails("Assigned jury member " + juryMemberId + " to committee " + committeeId + " with type " + juryType);
        auditLogRepository.save(auditLog);

        try {
            committeeNotificationService.notifyJuryAssignment(committeeId, juryMemberId, assignedBy);
        } catch (Exception e) {
            log.warn("Failed to send jury assignment notification: {}", e.getMessage());
        }

        log.info("[P5.3] Jury member assigned: committeeId={} juryMemberId={} type={}", committeeId, juryMemberId, juryType);
    }

    @Override
    @Transactional
    public void removeJury(Long committeeId, Long juryMemberId) {
        Committee committee = committeeRepository.findById(committeeId)
                .orElseThrow(() -> new NotFoundException("Committee not found"));

        CommitteeJury juryAssignment = committee.getJuryMembers().stream()
                .filter(cj -> cj.getJuryMember().getUserId().equals(juryMemberId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Jury member is not assigned to this committee"));

        committee.getJuryMembers().remove(juryAssignment);
        committeeJuryRepository.delete(juryAssignment);
        committeeRepository.save(committee);

        AuditLog auditLog = new AuditLog();
        auditLog.setActionType(ActionType.MEMBER_REMOVED);
        auditLog.setUserId(1L);
        auditLog.setEventDetails("Removed jury member " + juryMemberId + " from committee " + committeeId);
        auditLogRepository.save(auditLog);

        log.info("[P5.3] Jury member removed: committeeId={} juryMemberId={}", committeeId, juryMemberId);
    }

    @Override
    @Transactional(readOnly = true)
    public JuryListResponse getJuryByCommittee(Long committeeId) {
        Committee committee = committeeRepository.findById(committeeId)
                .orElseThrow(() -> new NotFoundException("Committee not found"));

        List<JuryMemberDto> juryMembers = committee.getJuryMembers().stream()
                .map(cj -> new JuryMemberDto(
                        cj.getJuryMemberId(),
                        cj.getJuryMember().getUserId(),
                        cj.getJuryMember().getFullName(),
                        cj.getJuryType(),
                        cj.getAssignedAt()))
                .collect(Collectors.toList());

        return new JuryListResponse("success", juryMembers);
    }

    @Override
    @Transactional(readOnly = true)
    public JuryListResponse getCommitteesByJury(Long juryMemberId) {
        userRepository.findById(juryMemberId)
                .orElseThrow(() -> new NotFoundException("Jury member not found"));

        List<CommitteeJury> assignments = committeeJuryRepository.findByJuryMemberId(juryMemberId);
        List<JuryMemberDto> committees = assignments.stream()
                .map(cj -> new JuryMemberDto(
                        cj.getJuryMemberId(),
                        cj.getCommittee().getCommitteeId(),
                        cj.getCommittee().getCommitteeName(),
                        cj.getJuryType(),
                        cj.getAssignedAt()))
                .collect(Collectors.toList());

        return new JuryListResponse("success", committees);
    }
}
