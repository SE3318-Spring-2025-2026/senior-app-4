package com.spms.backend.service;

import com.spms.backend.model.ValidationJob;

public interface AiValidationQueueProducer {
    void enqueue(ValidationJob job, boolean retryOnlyFailedIssues);
}
