package com.spms.backend.repository;

import com.spms.backend.model.GithubIntegration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GithubIntegrationRepository extends JpaRepository<GithubIntegration, Long> {
    Optional<GithubIntegration> findByGroup_Id(Long groupId);
}
