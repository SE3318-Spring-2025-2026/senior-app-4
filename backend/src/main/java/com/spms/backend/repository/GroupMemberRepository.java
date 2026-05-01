package com.spms.backend.repository;


import com.spms.backend.model.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    boolean existsByUser_UserId(Long userId);

    boolean existsByGroup_IdAndUser_UserId(Long groupId, Long userId);

    Optional<GroupMember> findTopByUser_UserId(Long userId);

    Optional<GroupMember> findByUser_UserId(Long userId);

    Optional<GroupMember> findFirstByUser_UserIdOrderByJoinedAtDesc(Long userId);

    @Query("SELECT gm FROM GroupMember gm JOIN FETCH gm.user JOIN FETCH gm.group")
    List<GroupMember> findAllWithUserAndGroup();
}
