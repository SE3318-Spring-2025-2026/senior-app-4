package com.spms.backend.repository;

import com.spms.backend.model.Committee;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CommitteeRepository extends JpaRepository<Committee, Long> {
    Optional<Committee> findByCommitteeId(Long committeeId);
}
