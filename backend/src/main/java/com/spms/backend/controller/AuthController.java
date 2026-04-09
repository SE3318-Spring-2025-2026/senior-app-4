package com.spms.backend.controller;

import com.spms.backend.dto.response.GithubCallbackResponse;
import com.spms.backend.service.GithubOAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.spms.backend.dto.request.GithubAuthRequest;
import com.spms.backend.dto.response.GithubAuthResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.spms.backend.dto.request.AuthTokenRequest;
import com.spms.backend.dto.response.AuthTokenResponse;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final GithubOAuthService githubOAuthService;

    public AuthController(GithubOAuthService githubOAuthService) {
        this.githubOAuthService = githubOAuthService;
    }

    @GetMapping("/github/callback")
    public ResponseEntity<GithubCallbackResponse> githubCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state
    ) {
        return ResponseEntity.ok(githubOAuthService.handleCallback(code, state));
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
