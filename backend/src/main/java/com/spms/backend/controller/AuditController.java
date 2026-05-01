package com.spms.backend.controller;

import com.spms.backend.dto.response.AuditLogListResponse;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.exception.ForbiddenException;
import com.spms.backend.model.ActionType;
import com.spms.backend.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditController {

    private final AuditLogService auditLogService;

    public AuditController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<AuditLogListResponse> getAllAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false) ActionType actionType,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Instant startDate,
            @RequestParam(required = false) Instant endDate,
            HttpServletRequest httpReq) {

        Object role = httpReq.getAttribute("jwt_role");
        if (role == null || (!("coordinator".equalsIgnoreCase(role.toString())) && !("admin".equalsIgnoreCase(role.toString())))) {
            throw new ForbiddenException("Only coordinators and admins can view audit logs.");
        }

        if (page < 0 || size <= 0 || size > 100) {
            throw new BadRequestException("Invalid pagination parameters. Page must be >= 0, size must be between 1 and 100.");
        }

        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        Sort.Direction sortDirection = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortField));
        
        return ResponseEntity.ok(auditLogService.getAllAuditLogs(actionType, entityType, startDate, endDate, pageable));
    }
}
