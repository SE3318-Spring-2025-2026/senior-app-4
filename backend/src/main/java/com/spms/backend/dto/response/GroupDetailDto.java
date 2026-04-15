package com.spms.backend.dto.response;

import java.time.Instant;
import java.util.List;
public record GroupDetailDto(
        Long id,
        String groupName,
        Long leaderId,
        Long advisorId,
        String status,
        Instant createdAt,
        Instant updatedAt,
        List<GroupMemberDto> members
) {}