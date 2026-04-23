package com.spms.backend.dto.response;

public record GroupAdvisorAssignmentDto(
    Long groupId,
    String groupName,
    String leaderName,
    Long advisorId,
    String advisorName,
    String status
) {}
