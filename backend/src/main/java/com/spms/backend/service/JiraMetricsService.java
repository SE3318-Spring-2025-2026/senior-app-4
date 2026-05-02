package com.spms.backend.service;

import com.spms.backend.dto.request.JiraCallbackRequest;
import com.spms.backend.dto.request.JiraIssueDetailsRequest;
import com.spms.backend.dto.response.JiraCallbackResponse;
import com.spms.backend.dto.response.JiraIssueDetailsResponse;

public interface JiraMetricsService {
    JiraCallbackResponse parseAndCleansePayload(JiraCallbackRequest request);
    JiraIssueDetailsResponse fetchAndMergeIssueDetails(JiraIssueDetailsRequest request);
}
