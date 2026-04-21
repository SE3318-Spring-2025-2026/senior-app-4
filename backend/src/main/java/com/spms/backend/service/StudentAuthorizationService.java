package com.spms.backend.service;

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

    public StudentAuthorizationService(UserRepository userRepository,
                                       GroupMemberRepository groupMemberRepository,
                                       GroupRepository groupRepository) {
        this.userRepository = userRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.groupRepository = groupRepository;
    }

    public ValidationResult validateStudentExists(Long studentId) {
        if (userRepository.existsById(studentId)) {
            return ValidationResult.success("Student exists.");
        }
        return ValidationResult.failure("Student not found.");
    }

    public ValidationResult validateNotInGroup(Long studentId) {
        if (groupMemberRepository.existsByUser_UserId(studentId)) {
            return ValidationResult.failure("This student is already a member of another group.");
        }
        return ValidationResult.success("Student is not in any group.");
    }

    public ValidationResult validateIsGroupLeader(Long userId, Long groupId) {
        Group group = groupRepository.findById(groupId).orElse(null);

        if (group == null) {
            return ValidationResult.failure("Group not found.");
        }

        if (group.getLeader() == null || !group.getLeader().getUserId().equals(userId)) {
            return ValidationResult.failure("Only the group leader can perform this action.");
        }

        return ValidationResult.success("User is group leader.");
    }

    public ValidationResult validateIsGroupMember(Long userId, Long groupId) {
        if (!groupRepository.existsById(groupId)) {
            return ValidationResult.failure("Group not found.");
        }

        if (!groupMemberRepository.existsByGroup_IdAndUser_UserId(groupId, userId)) {
            return ValidationResult.failure("User is not a member of this group.");
        }

        return ValidationResult.success("User is a group member.");
    }
}
