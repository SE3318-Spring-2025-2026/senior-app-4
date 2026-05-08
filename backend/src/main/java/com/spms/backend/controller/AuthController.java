package com.spms.backend.controller;

import com.spms.backend.dto.request.GithubAuthRequest;
import com.spms.backend.dto.request.LoginRequest;
import com.spms.backend.dto.request.PasswordChangeRequest;
import com.spms.backend.dto.response.GithubAuthResponse;
import com.spms.backend.dto.response.GithubCallbackResponse;
import com.spms.backend.dto.response.LoginResponse;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.exception.UnauthorizedException;
import com.spms.backend.model.User;
import com.spms.backend.repository.UserRepository;
import com.spms.backend.service.GithubOAuthService;
import com.spms.backend.service.JwtValidationService;
import com.spms.backend.service.PasswordHashingService;
import com.spms.backend.service.PasswordService;
import com.spms.backend.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Authentication")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final GithubOAuthService githubOAuthService;
    private final PasswordService passwordService;
    private final UserRepository userRepository;
    private final PasswordHashingService passwordHashingService;
    private final TokenService tokenService;
    private final JwtValidationService jwtValidationService;

    public AuthController(GithubOAuthService githubOAuthService,
                          PasswordService passwordService,
                          UserRepository userRepository,
                          PasswordHashingService passwordHashingService,
                          TokenService tokenService,
                          JwtValidationService jwtValidationService) {
        this.githubOAuthService = githubOAuthService;
        this.passwordService = passwordService;
        this.userRepository = userRepository;
        this.passwordHashingService = passwordHashingService;
        this.tokenService = tokenService;
        this.jwtValidationService = jwtValidationService;
    }

    @Operation(summary = "Start GitHub OAuth flow")
    @GetMapping("/github/callback")
    public ResponseEntity<GithubCallbackResponse> githubCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state
    ) {
        return ResponseEntity.ok(githubOAuthService.handleCallback(code, state));
    }

    @Operation(summary = "Generate GitHub authorization URL")
    @PostMapping("/github")
    public ResponseEntity<GithubAuthResponse> startGithubAuth(
            @RequestBody GithubAuthRequest request
    ) {
        String authorizationUrl = githubOAuthService.generateGithubAuthorizationUrl(request.studentId());
        return ResponseEntity.ok(new GithubAuthResponse(authorizationUrl));
    }

    @Operation(summary = "Professor/Coordinator login with email and password")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        if (!StringUtils.hasText(request.email()) || !StringUtils.hasText(request.password())) {
            throw new BadRequestException("Email and password are required.");
        }

        User user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password."));

        if (!"professor".equals(user.getRole()) && !"coordinator".equals(user.getRole())) {
            throw new UnauthorizedException("This login is for professors and coordinators only.");
        }

        if (!passwordHashingService.matchesPassword(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password.");
        }

        String token = tokenService.generateToken(user);

        return ResponseEntity.ok(new LoginResponse(
                "Login successful.",
                token,
                "Bearer",
                tokenService.getExpirationSeconds(),
                user.getRole(),
                user.isRequiresPasswordChange()
        ));
    }

    @Operation(summary = "Change password — with JWT token (newPassword only) or without JWT (email + tempPassword + newPassword)")
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @RequestBody PasswordChangeRequest request,
            HttpServletRequest httpRequest
    ) {
        String authHeader = httpRequest.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            Map<String, Object> claims = jwtValidationService.validateAndParse(token);
            if (claims == null) {
                throw new UnauthorizedException("Invalid or expired token.");
            }
            if (!StringUtils.hasText(request.newPassword())) {
                throw new BadRequestException("newPassword is required.");
            }
            Long userId = ((Number) claims.get("userId")).longValue();
            passwordService.changePasswordByUserId(userId, request.newPassword());
        } else {
            passwordService.changePassword(request);
        }
        return ResponseEntity.ok(Map.of("message", "Password updated successfully."));
    }

    @Operation(summary = "Reset password using one-time reset token generated by admin")
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @RequestParam String token,
            @RequestBody Map<String, String> body
    ) {
        String newPassword = body.get("newPassword");
        if (!StringUtils.hasText(newPassword)) {
            throw new BadRequestException("newPassword is required.");
        }

        User user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired reset token."));

        user.setPasswordHash(passwordHashingService.hashPassword(newPassword));
        user.setPasswordResetToken(null);
        user.setRequiresPasswordChange(false);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Password reset successfully."));
    }
}
