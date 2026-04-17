package com.spms.backend.service;

import com.spms.backend.dto.request.GroupCreateRequestDto;
import com.spms.backend.dto.request.GroupUpdateRequestDto;
import com.spms.backend.dto.response.GroupDetailDto;
import com.spms.backend.dto.response.GroupResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GroupService {
    GroupResponseDto createGroup(GroupCreateRequestDto request, Long creatorId);
    void updateGroupName(Long groupId, GroupUpdateRequestDto request, Long requesterId);
    Page<GroupResponseDto> getAllGroups(Pageable pageable);
    GroupDetailDto getGroupDetails(Long groupId);
    void disbandGroup(Long groupId, Long requesterId, String requesterRole);

    /** coordinator_fX: Retrieve all system group-advisor mappings. */
    java.util.List<com.spms.backend.dto.response.GroupAdvisorAssignmentDto> getAdvisorAssignments();
}
