package com.spms.backend.service.impl;

import com.spms.backend.dto.request.SystemLogCreateRequestDto;
import com.spms.backend.model.ValidationJob;
import com.spms.backend.service.AiValidationQueueProducer;
import com.spms.backend.service.SystemLogService;
import com.spms.backend.service.ValidationPipelineOrchestrator;
import org.springframework.stereotype.Service;

@Service
public class AiValidationQueueProducerImpl implements AiValidationQueueProducer {

    private final SystemLogService systemLogService;
    private final ValidationPipelineOrchestrator orchestrator;

    public AiValidationQueueProducerImpl(SystemLogService systemLogService,
                                          ValidationPipelineOrchestrator orchestrator) {
        this.systemLogService = systemLogService;
        this.orchestrator = orchestrator;
    }

    @Override
    public void enqueue(ValidationJob job, boolean retryOnlyFailedIssues) {
        SystemLogCreateRequestDto request = new SystemLogCreateRequestDto();
        request.setEventType("P7_VALIDATION_ENQUEUED");
        request.setMessage("P7 validation job enqueued. jobId=" + job.getJobId()
                + ", sprintId=" + job.getSprint().getId()
                + ", teamId=" + (job.getTeam() != null ? job.getTeam().getId() : "null")
                + ", retryOnlyFailedIssues=" + retryOnlyFailedIssues);
        request.setStackTrace(null);
        systemLogService.logEventAsync(request);

        orchestrator.runAsync(job, retryOnlyFailedIssues);
    }
}
