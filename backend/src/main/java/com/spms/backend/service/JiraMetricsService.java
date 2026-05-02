package com.spms.backend.service;

import com.spms.backend.dto.request.JiraCallbackRequest;
import com.spms.backend.dto.request.JiraInitializeRequest;
import com.spms.backend.dto.request.JiraIssueDetailsRequest;
import com.spms.backend.dto.request.JiraIssueQueryRequest;
import com.spms.backend.dto.response.JiraCallbackResponse;
import com.spms.backend.dto.response.JiraInitializeResponse;
import com.spms.backend.dto.response.JiraIssueDetailsResponse;
import com.spms.backend.dto.response.JiraIssueQueryResponse;

public interface JiraMetricsService {
    JiraCallbackResponse    parseAndCleansePayload(JiraCallbackRequest request);
    JiraIssueDetailsResponse fetchAndMergeIssueDetails(JiraIssueDetailsRequest request);
    JiraInitializeResponse  initializeConnection(JiraInitializeRequest request);
    JiraIssueQueryResponse  queryIssuesByJql(JiraIssueQueryRequest request);
}
