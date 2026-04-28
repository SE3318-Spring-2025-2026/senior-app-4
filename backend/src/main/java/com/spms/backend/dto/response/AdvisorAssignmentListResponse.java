package com.spms.backend.dto.response;

import java.util.List;

/** P4-ASSIGN-1 — list advisor–group mappings (see OpenAPI AdvisorAssignmentListResponse). */
public record AdvisorAssignmentListResponse(String status, List<GroupAdvisorAssignmentDto> data) {}
