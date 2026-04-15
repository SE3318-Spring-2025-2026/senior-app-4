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
}