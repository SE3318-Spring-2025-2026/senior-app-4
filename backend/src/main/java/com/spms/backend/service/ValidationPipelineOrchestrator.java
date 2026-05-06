package com.spms.backend.service;

import com.spms.backend.model.ValidationJob;

public interface ValidationPipelineOrchestrator {
    void runAsync(ValidationJob job, boolean retryOnlyFailedIssues);
}
