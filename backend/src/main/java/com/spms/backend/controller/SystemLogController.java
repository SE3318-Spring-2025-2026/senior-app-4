package com.spms.backend.controller;

import com.spms.backend.dto.request.SystemLogCreateRequestDto;
import com.spms.backend.model.SystemLog;
import com.spms.backend.service.SystemLogService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/logs")
public class SystemLogController {

    private final SystemLogService systemLogService;

    public SystemLogController(SystemLogService systemLogService) {
        this.systemLogService = systemLogService;
    }

    @PostMapping("/audit-event")
    public ResponseEntity<Void> logAuditEvent(@Valid @RequestBody SystemLogCreateRequestDto request) {
        systemLogService.logEventAsync(request);
        return ResponseEntity.accepted().build();
    }

    @GetMapping
    public ResponseEntity<Page<SystemLog>> getLogs(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        
        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        Sort.Direction sortDirection = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortField));
        
        return ResponseEntity.ok(systemLogService.getLogsByType(type, pageable));
    }
}
