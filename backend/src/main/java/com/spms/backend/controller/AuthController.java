package com.spms.backend.controller;

import com.spms.backend.dto.request.PasswordChangeRequest;
import com.spms.backend.dto.response.GithubCallbackResponse;
import com.spms.backend.service.GithubOAuthService;
import com.spms.backend.service.PasswordService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(@RequestBody PasswordChangeRequest request) {

        passwordService.changePassword(request);

        return ResponseEntity.ok("Password updated successfully.");
    }
}