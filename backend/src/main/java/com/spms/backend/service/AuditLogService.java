package com.spms.backend.service;

import com.spms.backend.dto.response.AuditLogListResponse;
import org.springframework.data.domain.Pageable;

public interface AuditLogService {

    AuditLogListResponse getAuditLogsByCommittee(Long committeeId, Pageable pageable);

    AuditLogListResponse getAllAuditLogs(Pageable pageable);
}
