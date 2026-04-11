package com.spms.backend.controller;

import com.spms.backend.dto.request.AuthTokenRequest;
import com.spms.backend.dto.request.GithubAuthRequest;
import com.spms.backend.dto.request.LoginRequest;
import com.spms.backend.dto.request.PasswordChangeRequest;
import com.spms.backend.dto.response.AuthTokenResponse;
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
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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

    // ── GET /api/v1/auth/github/callback ──
    @GetMapping("/github/callback")
    public ResponseEntity<GithubCallbackResponse> githubCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state
    ) {
        return ResponseEntity.ok(githubOAuthService.handleCallback(code, state));
    }

    // ── POST /api/v1/auth/github ──
    @PostMapping("/github")
    public ResponseEntity<GithubAuthResponse> startGithubAuth(
            @RequestBody GithubAuthRequest request
    ) {
        String authorizationUrl = githubOAuthService.generateGithubAuthorizationUrl(request.studentId());
        return ResponseEntity.ok(new GithubAuthResponse(authorizationUrl));
    }

    // ── POST /api/v1/auth/token ──
    @PostMapping("/token")
    public ResponseEntity<AuthTokenResponse> generateToken(
            @RequestBody AuthTokenRequest request
    ) {
        return ResponseEntity.ok(githubOAuthService.generateToken(request.userId()));
    }

    // ── POST /api/v1/auth/login — Professor/Coordinator login ──
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

    // ── POST /api/v1/auth/change-password ──
    // JWT varsa token üzerinden, yoksa email+tempPassword ile şifre değiştirir
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

    // ── POST /api/v1/auth/reset-password — Token ile şifre sıfırlama ──
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @RequestParam String token,
            @RequestBody Map<String, String> body
    ) {
        String newPassword = body.get("newPassword");
        if (!StringUtils.hasText(newPassword)) {
            throw new BadRequestException("newPassword is required.");
        }

        // Token, AdminController'da reset token olarak passwordHash'e kaydedildi
        // Tüm kullanıcıları tara ve eşleşeni bul (küçük sistemlerde yeterli)
        User user = userRepository.findAll().stream()
                .filter(u -> u.isRequiresPasswordChange()
                        && u.getPasswordHash() != null
                        && passwordHashingService.matchesPassword(token, u.getPasswordHash()))
                .findFirst()
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired reset token."));

        user.setPasswordHash(passwordHashingService.hashPassword(newPassword));
        user.setRequiresPasswordChange(false);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Password reset successfully."));
    }
}
