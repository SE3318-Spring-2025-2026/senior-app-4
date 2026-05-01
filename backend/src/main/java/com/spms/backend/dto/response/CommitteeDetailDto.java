package com.spms.backend.dto.response;

import java.time.Instant;
import java.util.List;

public record CommitteeDetailDto(
        Long committeeId,
        String committeeName,
        String description,
        String status,
        Long createdBy,
        Instant createdAt,
        Instant updatedAt,
        List<AdvisorDto> advisors,
        List<JuryMemberDto> juryMembers,
        List<GroupAssignmentDto> groupAssignments) {}
