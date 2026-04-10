package com.spms.backend.controller;

import com.spms.backend.dto.request.AuthTokenRequest;
import com.spms.backend.dto.request.GithubAuthRequest;
import com.spms.backend.dto.request.PasswordChangeRequest;
import com.spms.backend.dto.response.AuthTokenResponse;
import com.spms.backend.dto.response.GithubAuthResponse;
import com.spms.backend.dto.response.GithubCallbackResponse;
import com.spms.backend.service.GithubOAuthService;
import com.spms.backend.service.PasswordService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final GithubOAuthService githubOAuthService;
    private final PasswordService passwordService;

    public AuthController(GithubOAuthService githubOAuthService,
                          PasswordService passwordService) {
        this.githubOAuthService = githubOAuthService;
        this.passwordService = passwordService;
    }

    @GetMapping("/github/callback")
    public ResponseEntity<GithubCallbackResponse> githubCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state
    ) {
        return ResponseEntity.ok(githubOAuthService.handleCallback(code, state));
    }

    // B1 düzeltmesi: Spec'e göre JSON objesi dönmeli, düz string değil
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @RequestBody PasswordChangeRequest request
    ) {
        passwordService.changePassword(request);
        return ResponseEntity.ok(Map.of("message", "Password updated successfully."));
    }

    @PostMapping("/github")
    public ResponseEntity<GithubAuthResponse> startGithubAuth(
            @RequestBody GithubAuthRequest request
    ) {
        String authorizationUrl = githubOAuthService.generateGithubAuthorizationUrl(request.studentId());
        return ResponseEntity.ok(new GithubAuthResponse(authorizationUrl));
    }

    @PostMapping("/token")
    public ResponseEntity<AuthTokenResponse> generateToken(
            @RequestBody AuthTokenRequest request
    ) {
        AuthTokenResponse response = githubOAuthService.generateToken(request.userId());
        return ResponseEntity.ok(response);
    }
}
