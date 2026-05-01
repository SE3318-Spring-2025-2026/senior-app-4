package com.spms.backend.service;

import com.spms.backend.dto.response.AuditLogListResponse;
import org.springframework.data.domain.Pageable;

public interface AuditLogService {

    AuditLogListResponse getAuditLogsByCommittee(Long committeeId, Pageable pageable);

    AuditLogListResponse getAllAuditLogs(com.spms.backend.model.ActionType action, String entityType, java.time.Instant startDate, java.time.Instant endDate, Pageable pageable);
}
