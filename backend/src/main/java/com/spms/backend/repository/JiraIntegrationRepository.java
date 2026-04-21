package com.spms.backend.repository;

import com.spms.backend.model.JiraIntegration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JiraIntegrationRepository extends JpaRepository<JiraIntegration, Long> {
    Optional<JiraIntegration> findByGroup_Id(Long groupId);
    void deleteByGroup_Id(Long groupId);
}
