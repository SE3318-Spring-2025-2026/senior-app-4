package com.spms.backend.service.impl;

import com.spms.backend.dto.request.SystemLogCreateRequestDto;
import com.spms.backend.model.ValidationJob;
import com.spms.backend.service.AiValidationQueueProducer;
import com.spms.backend.service.SystemLogService;
import org.springframework.stereotype.Service;

@Service
public class AiValidationQueueProducerImpl implements AiValidationQueueProducer {
    private final SystemLogService systemLogService;

    public AiValidationQueueProducerImpl(SystemLogService systemLogService) {
        this.systemLogService = systemLogService;
    }

    @Override
    public void enqueue(ValidationJob job, boolean retryOnlyFailedIssues) {
        String message = "P7 validation job enqueued. jobId=" + job.getJobId()
                + ", sprintId=" + job.getSprint().getId()
                + ", teamId=" + (job.getTeam() != null ? job.getTeam().getId() : "null")
                + ", retryOnlyFailedIssues=" + retryOnlyFailedIssues;
        SystemLogCreateRequestDto request = new SystemLogCreateRequestDto();
        request.setEventType("P7_VALIDATION_ENQUEUED");
        request.setMessage(message);
        request.setStackTrace(null);
        systemLogService.logEventAsync(request);
    }
}
