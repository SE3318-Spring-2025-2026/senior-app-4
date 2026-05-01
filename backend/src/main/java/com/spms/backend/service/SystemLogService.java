package com.spms.backend.service;

import com.spms.backend.dto.request.SystemLogCreateRequestDto;
import com.spms.backend.model.SystemLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SystemLogService {
    void logEventAsync(SystemLogCreateRequestDto request);
    Page<SystemLog> getLogsByType(String eventType, Pageable pageable);
}
