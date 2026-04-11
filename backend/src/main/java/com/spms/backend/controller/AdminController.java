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

    public AdminController(UserRepository userRepository,
                           PasswordHashingService passwordHashingService) {
        this.userRepository = userRepository;
        this.passwordHashingService = passwordHashingService;
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
}
