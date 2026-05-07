package com.spms.backend.controller;

import com.spms.backend.dto.request.ResetLinkRequest;
import com.spms.backend.dto.response.ResetLinkResponse;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.model.User;
import com.spms.backend.repository.UserRepository;
import com.spms.backend.service.EmailService;
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
    private final EmailService emailService;
    private final AuditLogRepository auditLogRepository;

    public AdminController(UserRepository userRepository,
                           EmailService emailService,
                           AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.auditLogRepository = auditLogRepository;
    }

    @Operation(summary = "Generate one-time password reset link and send via email")
    @PostMapping("/generate-reset-link")
    public ResponseEntity<ResetLinkResponse> generateResetLink(
            @Valid @RequestBody ResetLinkRequest request
    ) {
        User user = userRepository.findByEmail(request.targetEmail())
                .orElseThrow(() -> new BadRequestException(
                        "Professor not found for the given email."));

        String resetToken = UUID.randomUUID().toString();

        user.setPasswordResetToken(resetToken);
        user.setRequiresPasswordChange(true);
        userRepository.save(user);

        emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), resetToken);

        return ResponseEntity.ok(new ResetLinkResponse(
                "Password reset email sent successfully.",
                resetToken,
                null
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
