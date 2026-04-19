package com.spms.backend.repository;

import com.spms.backend.model.AdvisorRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdvisorRequestRepository extends JpaRepository<AdvisorRequest, Long> {
}
