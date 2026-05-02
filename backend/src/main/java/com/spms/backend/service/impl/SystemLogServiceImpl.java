package com.spms.backend.service.impl;

import com.spms.backend.dto.request.SystemLogCreateRequestDto;
import com.spms.backend.model.SystemLog;
import com.spms.backend.repository.SystemLogRepository;
import com.spms.backend.service.SystemLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class SystemLogServiceImpl implements SystemLogService {

    private final SystemLogRepository systemLogRepository;

    public SystemLogServiceImpl(SystemLogRepository systemLogRepository) {
        this.systemLogRepository = systemLogRepository;
    }

    @Async
    @Override
    public void logEventAsync(SystemLogCreateRequestDto request) {
        SystemLog log = new SystemLog(
                request.getEventType(),
                request.getMessage(),
                request.getStackTrace()
        );
        systemLogRepository.save(log);
    }

    @Override
    public Page<SystemLog> getLogsByType(String eventType, Pageable pageable) {
        if (eventType == null || eventType.trim().isEmpty()) {
            return systemLogRepository.findAll(pageable);
        }
        return systemLogRepository.findByEventType(eventType, pageable);
    }
}
