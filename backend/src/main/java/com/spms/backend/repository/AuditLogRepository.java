package com.spms.backend.repository;

import com.spms.backend.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import com.spms.backend.model.ActionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    Page<AuditLog> findByActionType(ActionType actionType, Pageable pageable);

    Page<AuditLog> findByGroupId(Long groupId, Pageable pageable);

    java.util.Optional<AuditLog> findTopByGroupIdAndActionTypeOrderByCreatedAtDesc(Long groupId, ActionType actionType);

    Page<AuditLog> findByCommitteeIdOrderByCreatedAtDesc(Long committeeId, Pageable pageable);

    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
