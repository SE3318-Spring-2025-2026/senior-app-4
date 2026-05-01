package com.spms.backend.repository;

import com.spms.backend.model.Committee;
import com.spms.backend.model.enums.CommitteeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommitteeRepository extends JpaRepository<Committee, Long> {
    Optional<Committee> findByCommitteeId(Long committeeId);

    Page<Committee> findByStatus(CommitteeStatus status, Pageable pageable);
    
    List<Committee> findByStatus(CommitteeStatus status); // For testing

    Page<Committee> findByCreatedBy(Long coordinatorId, Pageable pageable);
    
    List<Committee> findByCreatedBy(Long coordinatorId); // For testing

    @EntityGraph(attributePaths = {"advisors", "juryMembers"})
    @Query("SELECT c FROM Committee c WHERE c.committeeId = :committeeId")
    Optional<Committee> findWithAdvisorsAndJury(@Param("committeeId") Long committeeId);
    
    @EntityGraph(attributePaths = {"advisors", "juryMembers"})
    @Query("SELECT c FROM Committee c")
    List<Committee> findAllWithDetails();
}
