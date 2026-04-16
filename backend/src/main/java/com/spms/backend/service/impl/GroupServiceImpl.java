package com.spms.backend.service.impl;

import com.spms.backend.dto.request.GroupCreateRequestDto;
import com.spms.backend.dto.request.GroupUpdateRequestDto;
import com.spms.backend.dto.response.GroupDetailDto;
import com.spms.backend.dto.response.GroupMemberDto;
import com.spms.backend.dto.response.GroupResponseDto;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.model.User;
import com.spms.backend.model.Group;
import com.spms.backend.model.GroupMember;
import com.spms.backend.model.GroupRole;
import com.spms.backend.model.GroupStatus;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.service.GroupService;
import com.spms.backend.service.StudentAuthorizationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GroupServiceImpl implements GroupService {

    private final GroupRepository groupRepository;
    private final StudentAuthorizationService authService;

    public GroupServiceImpl(GroupRepository groupRepository, StudentAuthorizationService authService) {
        this.groupRepository = groupRepository;
        this.authService = authService;
    }

    @Override
    @Transactional
    public GroupResponseDto createGroup(GroupCreateRequestDto request, Long creatorId) {
        User creator = authService.validateStudentExists(creatorId);
        authService.validateNotInGroup(creatorId);


        Group group = new Group();
        group.setGroupName(request.groupName());
        group.setLeader(creator);
        group.setStatus(GroupStatus.FORMING);


        GroupMember leaderMember = new GroupMember();
        leaderMember.setGroup(group);
        leaderMember.setUser(creator);
        leaderMember.setRole(GroupRole.LEADER);

        group.getMembers().add(leaderMember);

        Group savedGroup = groupRepository.save(group);

        return mapToSimpleDto(savedGroup);
    }

    @Override
    @Transactional
    public void updateGroupName(Long groupId, GroupUpdateRequestDto request, Long requesterId) {

        Group group = authService.validateIsGroupLeader(requesterId, groupId);

        group.setGroupName(request.groupName());
        group.setUpdatedAt(Instant.now());

        groupRepository.save(group);
        //todo Issue P2.9 Audit Log service
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GroupResponseDto> getAllGroups(Pageable pageable) {
        return groupRepository.findAll(pageable)
                .map(this::mapToSimpleDto);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupDetailDto getGroupDetails(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BadRequestException("Group not found."));

        List<GroupMemberDto> memberDtos = group.getMembers().stream()
                .map(m -> new GroupMemberDto(
                        m.getUser().getUserId(),
                        m.getUser().getFullName(),
                        m.getRole().name(),
                        m.getJoinedAt()))
                .collect(Collectors.toList());

        return new GroupDetailDto(
                group.getId(),
                group.getGroupName(),
                group.getLeader().getUserId(),
                group.getAdvisor() != null ? group.getAdvisor().getUserId() : null,
                group.getStatus().name(),
                group.getCreatedAt(),
                group.getUpdatedAt(),
                memberDtos
        );
    }


    private GroupResponseDto mapToSimpleDto(Group group) {
        return new GroupResponseDto(
                group.getId(),
                group.getGroupName(),
                group.getLeader().getUserId(),
                group.getAdvisor() != null ? group.getAdvisor().getUserId() : null,
                group.getStatus().name(),
                group.getMembers().size(),
                group.getCreatedAt()
        );
    }
}
