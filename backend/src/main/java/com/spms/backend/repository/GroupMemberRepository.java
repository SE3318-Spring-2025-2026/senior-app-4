package com.spms.backend.repository;

import com.spms.backend.model.GroupMember;
import com.spms.backend.model.GroupMember.GroupMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, GroupMemberId> {

    List<GroupMember> findByIdGroupId(Long groupId);

    long countByIdGroupId(Long groupId);
}
