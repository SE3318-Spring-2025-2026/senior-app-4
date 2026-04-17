package com.spms.backend.dto.response;

public record GroupAdvisorAssignmentDto(
    Long groupId,
    String groupName,
    String leaderName,
    String advisorName,
    String status
) {}
