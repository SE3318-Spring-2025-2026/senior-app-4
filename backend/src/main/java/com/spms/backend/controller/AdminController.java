package com.spms.backend.controller;

import com.spms.backend.dto.request.ResetLinkRequest;
import com.spms.backend.dto.response.ResetLinkResponse;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.model.User;
import com.spms.backend.repository.UserRepository;
import com.spms.backend.service.PasswordHashingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.spms.backend.model.ActionType;
import com.spms.backend.model.AuditLog;
import com.spms.backend.dto.response.AuditLogResponseDto;
import com.spms.backend.repository.AuditLogRepository;

/**
 * Admin endpoints.
 *
 * POST /api/v1/admin/generate-reset-link — Process 1.9
 * Admin, şifresini unutan profesör için tek kullanımlık sıfırlama linki üretir.
 */
@Tag(name = "Admin")
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final PasswordHashingService passwordHashingService;
    private final AuditLogRepository auditLogRepository;

    public AdminController(UserRepository userRepository,
                           PasswordHashingService passwordHashingService,
                           AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.passwordHashingService = passwordHashingService;
        this.auditLogRepository = auditLogRepository;
    }

    @Operation(summary = "Generate one-time password reset link for a professor")
    @PostMapping("/generate-reset-link")
    public ResponseEntity<ResetLinkResponse> generateResetLink(
            @Valid @RequestBody ResetLinkRequest request
    ) {
        User user = userRepository.findByEmail(request.targetEmail())
                .orElseThrow(() -> new BadRequestException(
                        "Professor not found for the given email."));

        // Tek kullanımlık token üret
        String resetToken = UUID.randomUUID().toString();

        // Geçici şifre olarak token'ı hash'le ve kullanıcıyı güncelle
        user.setPasswordHash(passwordHashingService.hashPassword(resetToken));
        user.setRequiresPasswordChange(true);
        userRepository.save(user);

        String resetUrl = "http://localhost:3000/auth/change-password?token=" + resetToken;

        return ResponseEntity.ok(new ResetLinkResponse(
                "Password reset link generated successfully.",
                resetToken,
                resetUrl
        ));
    }
    @Operation(summary = "Get global audit logs")
    @GetMapping("/logs")
    public ResponseEntity<Page<AuditLogResponseDto>> getSystemLogs(
            @RequestParam(required = false) ActionType actionType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestAttribute("jwt_role") Object role) {

        if (!"coordinator".equalsIgnoreCase(role.toString())) {
            throw new com.spms.backend.exception.ForbiddenException("Only coordinators can view global logs.");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AuditLog> logsPage;

        if (actionType != null) {
            logsPage = auditLogRepository.findByActionType(actionType, pageable);
        } else {
            logsPage = auditLogRepository.findAll(pageable);
        }

        Page<AuditLogResponseDto> responsePage = logsPage.map(log -> new AuditLogResponseDto(
                log.getId(),
                log.getUserId(),
                log.getActionType(),
                log.getEventDetails(),
                log.getGroupId(),
                log.getCommitteeId(),
                log.getIpAddress(),
                log.getCreatedAt()
        ));

        return ResponseEntity.ok(responsePage);
    }
}
