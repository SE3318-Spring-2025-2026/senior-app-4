package com.spms.backend.service;

import com.spms.backend.dto.request.AssignmentStatusUpdateRequest;
import com.spms.backend.dto.request.GroupAssignmentRequest;
import com.spms.backend.dto.response.DeleteResponse;
import com.spms.backend.dto.response.GroupAssignmentResponse;

import java.util.List;

/**
 * P5.4 — Assign Groups to Committee.
 */
public interface GroupCommitteeAssignmentService {

    GroupAssignmentResponse assignGroup(Long committeeId, GroupAssignmentRequest request, Long actorUserId);

    List<GroupAssignmentResponse> listGroupsForCommittee(Long committeeId);

    List<GroupAssignmentResponse> listCommitteesForGroup(Long groupId);

    GroupAssignmentResponse updateStatus(Long committeeId, Long assignmentId,
                                         AssignmentStatusUpdateRequest request, Long actorUserId);

    DeleteResponse deleteAssignment(Long committeeId, Long assignmentId, Long actorUserId);
}
