package com.spms.backend.service.impl;

import com.spms.backend.dto.response.AuditLogListResponse;
import com.spms.backend.dto.response.AuditLogResponseDto;
import com.spms.backend.dto.response.CommitteePaginationInfo;
import com.spms.backend.exception.NotFoundException;
import com.spms.backend.model.AuditLog;
import com.spms.backend.repository.AuditLogRepository;
import com.spms.backend.repository.CommitteeRepository;
import com.spms.backend.service.AuditLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final CommitteeRepository committeeRepository;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository, CommitteeRepository committeeRepository) {
        this.auditLogRepository = auditLogRepository;
        this.committeeRepository = committeeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogListResponse getAuditLogsByCommittee(Long committeeId, Pageable pageable) {
        committeeRepository.findById(committeeId)
                .orElseThrow(() -> new NotFoundException("Committee not found with id " + committeeId));

        Page<AuditLog> auditLogs = auditLogRepository.findByCommitteeIdOrderByCreatedAtDesc(committeeId, pageable);

        List<AuditLogResponseDto> logs = auditLogs.getContent().stream()
                .map(this::toAuditLogResponseDto)
                .collect(Collectors.toList());

        CommitteePaginationInfo pagination = new CommitteePaginationInfo(
                auditLogs.getNumber(),
                auditLogs.getTotalPages(),
                auditLogs.getTotalElements(),
                auditLogs.getSize());

        return new AuditLogListResponse("success", logs, pagination);
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogListResponse getAllAuditLogs(Pageable pageable) {
        Page<AuditLog> auditLogs = auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);

        List<AuditLogResponseDto> logs = auditLogs.getContent().stream()
                .map(this::toAuditLogResponseDto)
                .collect(Collectors.toList());

        CommitteePaginationInfo pagination = new CommitteePaginationInfo(
                auditLogs.getNumber(),
                auditLogs.getTotalPages(),
                auditLogs.getTotalElements(),
                auditLogs.getSize());

        return new AuditLogListResponse("success", logs, pagination);
    }

    private AuditLogResponseDto toAuditLogResponseDto(AuditLog log) {
        return new AuditLogResponseDto(
                log.getId(),
                log.getUserId(),
                log.getActionType(),
                log.getEventDetails(),
                log.getGroupId(),
                log.getCommitteeId(),
                log.getIpAddress(),
                log.getCreatedAt());
    }
}
