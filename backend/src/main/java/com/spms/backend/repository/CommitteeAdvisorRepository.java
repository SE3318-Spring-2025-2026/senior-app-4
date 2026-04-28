package com.spms.backend.repository;

import com.spms.backend.model.CommitteeAdvisor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommitteeAdvisorRepository extends JpaRepository<CommitteeAdvisor, Long> {

    @Query("SELECT ca.committee.committeeId FROM CommitteeAdvisor ca WHERE ca.advisor.userId = :userId")
    List<Long> findCommitteeIdsByAdvisorUserId(@Param("userId") Long userId);
}
