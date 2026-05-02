package com.spms.backend.dto.response;

import java.util.List;

public record JiraIssueQueryResponse(
    int          total,
    List<String> issueKeys
) {}
