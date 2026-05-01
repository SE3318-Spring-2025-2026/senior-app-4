package com.spms.backend.dto.request;

import java.util.List;

public record DiscoveryRequest(String owner, String repo, List<String> jiraIds) {
}
