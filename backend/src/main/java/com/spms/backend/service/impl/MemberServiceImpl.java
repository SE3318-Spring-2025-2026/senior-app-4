package com.spms.backend.service.impl;

import com.spms.backend.exception.BadRequestException;
import com.spms.backend.exception.ForbiddenException;
import com.spms.backend.exception.NotFoundException;
import com.spms.backend.model.Group;
import com.spms.backend.model.GroupMember;
import com.spms.backend.model.GroupRole;
import com.spms.backend.model.GroupStatus;
import com.spms.backend.model.User;
import com.spms.backend.model.notification.Notification;
import com.spms.backend.model.notification.NotificationStatus;
import com.spms.backend.model.notification.NotificationType;
import com.spms.backend.repository.GroupMemberRepository;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.NotificationRepository;
import com.spms.backend.repository.UserRepository;
import com.spms.backend.service.MemberService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements MemberService without injecting GroupService or NotificationService
 * to avoid circular-bean dependencies. Uses repositories directly.
 */
@Service
public class MemberServiceImpl implements MemberService {

    private static final int MAX_GROUP_SIZE = 8;

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    public MemberServiceImpl(GroupRepository groupRepository,
                             GroupMemberRepository groupMemberRepository,
                             UserRepository userRepository,
                             NotificationRepository notificationRepository) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    // ------------------------------------------------------------------ ns_f1
    @Override
    @Transactional
    public void inviteMember(Long groupId, Long targetStudentId, Long leaderId) {

        // 1. Group must exist
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found."));

        // 2. Caller must be the group leader
        if (!group.getLeader().getUserId().equals(leaderId)) {
            throw new ForbiddenException("Only the group leader can invite members.");
        }

        // 3. Group must be in FORMING status
        if (group.getStatus() != GroupStatus.FORMING) {
            throw new BadRequestException("Invites can only be sent while the group is in FORMING status.");
        }

        // 4. Group must not be full
        if (group.getMembers().size() >= MAX_GROUP_SIZE) {
            throw new BadRequestException("Group is full (max " + MAX_GROUP_SIZE + " members).");
        }


        User targetStudent = userRepository.findByStudentId(String.valueOf(targetStudentId))
        .orElseThrow(() -> new NotFoundException("Student not found."));
        Long targetUserId = targetStudent.getUserId();

        // 6. Target student must not already belong to a group
        if (groupMemberRepository.existsByUser_UserId(targetStudentId)) {
            throw new BadRequestException("This student is already a member of another group.");
        }

        // 7. Leader can't invite themselves
        if (targetStudentId.equals(leaderId)) {
            throw new BadRequestException("You cannot invite yourself.");
        }

        // 8. Cancel any existing PENDING invite for this (group, student) pair — re-invite support
        notificationRepository
                .findByGroupIdAndToUser_UserIdAndTypeAndStatus(
                        groupId, targetStudentId,
                        NotificationType.MEMBERSHIP_INVITE,
                        NotificationStatus.PENDING)
                .ifPresent(existing -> {
                    existing.setStatus(NotificationStatus.CLEARED);
                    notificationRepository.save(existing);
                });

        // 9. Build and persist the invite notification
        User leader = userRepository.findById(leaderId)
                .orElseThrow(() -> new NotFoundException("Leader not found."));

        Notification invite = new Notification();
        invite.setType(NotificationType.MEMBERSHIP_INVITE);
        invite.setStatus(NotificationStatus.PENDING);
        invite.setMessage(leader.getFullName() + " has invited you to join group \"" + group.getGroupName() + "\".");
        invite.setGroupId(groupId);
        invite.setFromUser(leader);
        invite.setToUser(targetStudent);

        notificationRepository.save(invite);
    }

    // ------------------------------------------------------------------ ns_f4
    @Override
    @Transactional
    public void addMember(Long groupId, Long studentId) {

        // 1. Group must still exist
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found."));

        // 2. Guard: group must not be full (race-condition safety)
        if (group.getMembers().size() >= MAX_GROUP_SIZE) {
            throw new BadRequestException("Cannot join: group is already full.");
        }

        // 3. Guard: student must not have joined another group in the meantime
        if (groupMemberRepository.existsByUser_UserId(studentId)) {
            throw new BadRequestException("Cannot join: you are already a member of another group.");
        }

        // 4. Retrieve student
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new NotFoundException("Student not found."));

        // 5. Create the GroupMember record
        GroupMember newMember = new GroupMember();
        newMember.setGroup(group);
        newMember.setUser(student);
        newMember.setRole(GroupRole.MEMBER);

        groupMemberRepository.save(newMember);
    }
}
