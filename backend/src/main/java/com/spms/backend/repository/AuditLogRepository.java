package com.spms.backend.repository;

import com.spms.backend.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.spms.backend.model.ActionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    Page<AuditLog> findByActionType(ActionType actionType, Pageable pageable);
    
    Page<AuditLog> findByGroupId(Long groupId, Pageable pageable);
}
