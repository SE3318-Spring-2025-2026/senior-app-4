package com.spms.backend.repository;

import com.spms.backend.model.GroupCommitteeAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupCommitteeAssignmentRepository extends JpaRepository<GroupCommitteeAssignment, Long> {
    
    Optional<GroupCommitteeAssignment> findTopByGroupIdAndStatusOrderByAssignedAtDesc(Long groupId, String status);

}
