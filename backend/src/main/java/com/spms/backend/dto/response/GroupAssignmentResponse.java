package com.spms.backend.dto.response;

import com.spms.backend.model.GroupCommitteeAssignment;

import java.time.Instant;

public record GroupAssignmentResponse(
        Long assignmentId,
        Long committeeId,
        Long groupId,
        String status,
        Instant examDate,
        Instant assignedAt,
        Long assignedBy
) {
    public static GroupAssignmentResponse from(GroupCommitteeAssignment a) {
        return new GroupAssignmentResponse(
                a.getAssignmentId(),
                a.getCommittee() != null ? a.getCommittee().getCommitteeId() : null,
                a.getGroupId(),
                a.getStatus(),
                a.getExamDate(),
                a.getAssignedAt(),
                a.getAssignedBy()
        );
    }
}
