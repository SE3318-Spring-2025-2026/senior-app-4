package com.spms.backend.repository;

import com.spms.backend.model.AuditLog;
import com.spms.backend.model.ActionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    Page<AuditLog> findByActionType(ActionType actionType, Pageable pageable);

    Page<AuditLog> findByGroupId(Long groupId, Pageable pageable);

    Optional<AuditLog> findTopByGroupIdAndActionTypeOrderByCreatedAtDesc(Long groupId, ActionType actionType);

    Optional<AuditLog> findTopByGroupIdAndActionTypeInOrderByCreatedAtDesc(Long groupId, Collection<ActionType> actionTypes);

    Page<AuditLog> findByCommitteeIdOrderByCreatedAtDesc(Long committeeId, Pageable pageable);

    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
