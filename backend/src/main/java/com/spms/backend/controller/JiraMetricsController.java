package com.spms.backend.controller;

import com.spms.backend.dto.request.JiraCallbackRequest;
import com.spms.backend.dto.request.JiraInitializeRequest;
import com.spms.backend.dto.request.JiraIssueDetailsRequest;
import com.spms.backend.dto.request.JiraIssueQueryRequest;
import com.spms.backend.dto.response.JiraCallbackResponse;
import com.spms.backend.dto.response.JiraInitializeResponse;
import com.spms.backend.dto.response.JiraIssueDetailsResponse;
import com.spms.backend.dto.response.JiraIssueQueryResponse;
import com.spms.backend.service.JiraMetricsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/jira-metrics")
public class JiraMetricsController {

    private final JiraMetricsService jiraMetricsService;

    public JiraMetricsController(JiraMetricsService jiraMetricsService) {
        this.jiraMetricsService = jiraMetricsService;
    }

    @PostMapping("/initialize")
    public ResponseEntity<JiraInitializeResponse> initialize(
            @Valid @RequestBody JiraInitializeRequest request) {
        return ResponseEntity.ok(jiraMetricsService.initializeConnection(request));
    }

    @PostMapping("/issue-query")
    public ResponseEntity<JiraIssueQueryResponse> issueQuery(
            @Valid @RequestBody JiraIssueQueryRequest request) {
        return ResponseEntity.ok(jiraMetricsService.queryIssuesByJql(request));
    }

    @PostMapping("/callback")
    public ResponseEntity<JiraCallbackResponse> handleCallback(
            @Valid @RequestBody JiraCallbackRequest request) {
        return ResponseEntity.ok(jiraMetricsService.parseAndCleansePayload(request));
    }

    @PostMapping("/issue-details")
    public ResponseEntity<JiraIssueDetailsResponse> getIssueDetails(
            @Valid @RequestBody JiraIssueDetailsRequest request) {
        return ResponseEntity.ok(jiraMetricsService.fetchAndMergeIssueDetails(request));
    }
}
