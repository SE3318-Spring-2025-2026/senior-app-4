package com.spms.backend.service;

import com.spms.backend.exception.BadRequestException;
import com.spms.backend.exception.UnauthorizedException;
import com.spms.backend.model.User;
import com.spms.backend.model.Group;
import com.spms.backend.repository.GroupMemberRepository;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class StudentAuthorizationService {

    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupRepository groupRepository;

    public StudentAuthorizationService(UserRepository userRepository, GroupMemberRepository groupMemberRepository, GroupRepository groupRepository) {
        this.userRepository = userRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.groupRepository = groupRepository;
    }

    public User validateStudentExists(Long studentId) {
        return userRepository.findById(studentId)
                .orElseThrow(() -> new BadRequestException("Student not found."));
    }


    public void validateNotInGroup(Long studentId) {
        if (groupMemberRepository.existsByUser_UserId(studentId)) {
            throw new BadRequestException("This student is already a member of another group.");
        }
    }


    public Group validateIsGroupLeader(Long userId, Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BadRequestException("Group not found."));

        if (!group.getLeader().getUserId().equals(userId)) {
            throw new UnauthorizedException("Only the group leader perform this action.");
        }
        return group;
    }
}
