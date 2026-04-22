package com.spms.backend.service;

/**
 * P2-API-05  — inviteMember: leader sends a MEMBERSHIP_INVITE notification (ns_f1)
 * P2-API-19  — addMember:    on accept, student is added to the group      (ns_f4 → P2.2)
 */
public interface MemberService {

    /**
     * Validates the invite and creates a MEMBERSHIP_INVITE notification for the target student.
     *
     * @param groupId         the group whose leader is sending the invite
     * @param targetStudentId the user ID of the student being invited
     * @param leaderId        the user ID of the requesting leader (must be group leader)
     */
    void inviteMember(Long groupId, Long targetStudentId, Long leaderId);

    /**
     * Adds the student to the group as a MEMBER.
     * Called by NotificationService when the student accepts the invite (ns_f4).
     *
     * @param groupId   the group to join
     * @param studentId the user ID of the accepting student
     */
    void addMember(Long groupId, Long studentId);
}
