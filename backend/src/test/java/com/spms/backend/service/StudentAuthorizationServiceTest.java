package com.spms.backend.service;

import com.spms.backend.model.Group;
import com.spms.backend.model.User;
import com.spms.backend.repository.GroupMemberRepository;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentAuthorizationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private GroupRepository groupRepository;

    private StudentAuthorizationService studentAuthorizationService;

    @BeforeEach
    void setUp() {
        studentAuthorizationService = new StudentAuthorizationService(userRepository, groupMemberRepository, groupRepository);
    }

    @Test
    void validateStudentExistsReturnsTrueWhenStudentExists() {
        when(userRepository.existsById(1L)).thenReturn(true);

        ValidationResult result = studentAuthorizationService.validateStudentExists(1L);

        assertTrue(result.valid());
    }

    @Test
    void validateStudentExistsReturnsFalseWhenStudentDoesNotExist() {
        when(userRepository.existsById(99L)).thenReturn(false);

        ValidationResult result = studentAuthorizationService.validateStudentExists(99L);

        assertFalse(result.valid());
    }

    @Test
    void validateNotInGroupReturnsTrueWhenStudentHasNoGroup() {
        when(groupMemberRepository.existsByUser_UserId(2L)).thenReturn(false);

        ValidationResult result = studentAuthorizationService.validateNotInGroup(2L);

        assertTrue(result.valid());
    }

    @Test
    void validateNotInGroupReturnsFalseWhenStudentAlreadyInGroup() {
        when(groupMemberRepository.existsByUser_UserId(2L)).thenReturn(true);

        ValidationResult result = studentAuthorizationService.validateNotInGroup(2L);

        assertFalse(result.valid());
    }

    @Test
    void validateIsGroupLeaderReturnsTrueWhenUserIsLeader() {
        User leader = new User();
        leader.setUserId(10L);

        Group group = new Group();
        group.setLeader(leader);

        when(groupRepository.findById(3L)).thenReturn(Optional.of(group));

        ValidationResult result = studentAuthorizationService.validateIsGroupLeader(10L, 3L);

        assertTrue(result.valid());
    }

    @Test
    void validateIsGroupLeaderReturnsFalseWhenUserIsNotLeader() {
        User leader = new User();
        leader.setUserId(10L);

        Group group = new Group();
        group.setLeader(leader);

        when(groupRepository.findById(3L)).thenReturn(Optional.of(group));

        ValidationResult result = studentAuthorizationService.validateIsGroupLeader(11L, 3L);

        assertFalse(result.valid());
    }

    @Test
    void validateIsGroupMemberReturnsFalseWhenGroupDoesNotExist() {
        when(groupRepository.existsById(3L)).thenReturn(false);

        ValidationResult result = studentAuthorizationService.validateIsGroupMember(10L, 3L);

        assertFalse(result.valid());
    }

    @Test
    void validateIsGroupMemberReturnsFalseWhenUserIsNotMember() {
        when(groupRepository.existsById(3L)).thenReturn(true);
        when(groupMemberRepository.existsByGroup_IdAndUser_UserId(3L, 10L)).thenReturn(false);

        ValidationResult result = studentAuthorizationService.validateIsGroupMember(10L, 3L);

        assertFalse(result.valid());
    }

    @Test
    void validateIsGroupMemberReturnsTrueWhenUserIsMember() {
        when(groupRepository.existsById(3L)).thenReturn(true);
        when(groupMemberRepository.existsByGroup_IdAndUser_UserId(3L, 10L)).thenReturn(true);

        ValidationResult result = studentAuthorizationService.validateIsGroupMember(10L, 3L);

        assertTrue(result.valid());
    }
}
