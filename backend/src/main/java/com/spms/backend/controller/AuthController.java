package com.spms.backend.controller;

import com.spms.backend.dto.request.PasswordChangeRequest;
import com.spms.backend.dto.response.GithubCallbackResponse;
import com.spms.backend.service.GithubOAuthService;
import com.spms.backend.service.PasswordService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    private final GithubOAuthService githubOAuthService;
    private final PasswordService passwordService;

    @Value("${github.client-id}")
    private String githubClientId;

    @Value("${github.redirect-uri}")
    private String githubRedirectUri;

    public AuthController(GithubOAuthService githubOAuthService,
                          PasswordService passwordService) {
        this.githubOAuthService = githubOAuthService;
        this.passwordService = passwordService;
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("AuthController works");
    }

    @PostMapping("/validate-id")
    public ResponseEntity<Boolean> validateStudentId(@RequestBody String studentId) {
        if (studentId == null || studentId.isBlank()) {
            return ResponseEntity.ok(false);
        }
        return ResponseEntity.ok(true);
    }

   @GetMapping("/github")
public ResponseEntity<String> redirectToGithub() {
    String encodedRedirectUri = URLEncoder.encode(githubRedirectUri, StandardCharsets.UTF_8);
    String githubUrl =
            "https://github.com/login/oauth/authorize?client_id="
                    + githubClientId
                    + "&redirect_uri="
                    + encodedRedirectUri;

    return ResponseEntity.ok(githubUrl);
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