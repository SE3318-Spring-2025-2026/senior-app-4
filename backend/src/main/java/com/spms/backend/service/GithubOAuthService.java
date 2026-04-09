package com.spms.backend.service;

import com.spms.backend.client.GithubApiClient;
import com.spms.backend.dto.internal.StudentRegistrationData;
import com.spms.backend.dto.response.GithubCallbackResponse;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.model.User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.spms.backend.config.GithubProperties;

@Service
public class GithubOAuthService {

    private final GithubApiClient githubApiClient;
    private final StudentRegistrationService studentRegistrationService;
    private final TokenService tokenService;
    private final GithubProperties githubProperties;

    public GithubOAuthService(
            GithubApiClient githubApiClient,
            StudentRegistrationService studentRegistrationService,
            TokenService tokenService,
            GithubProperties githubProperties
    ) {
        this.githubApiClient = githubApiClient;
        this.studentRegistrationService = studentRegistrationService;
        this.tokenService = tokenService;
        this.githubProperties = githubProperties;
    }

    public GithubCallbackResponse handleCallback(String code, String state) {
        String authorizationCode = requireText(code, "GitHub authorization code is required.");
        // OpenAPI currently marks state optional, but Issue #7 requires it to recover validated studentId.
        String studentId = requireText(state, "state is required and must contain the validated studentId.");

        String accessToken = githubApiClient.exchangeCodeForAccessToken(authorizationCode);
        String githubUsername = githubApiClient.fetchGithubUsername(accessToken);

        User user = studentRegistrationService.findOrCreateFromCallback(
                new StudentRegistrationData(studentId, githubUsername, accessToken)
        );
        String token = tokenService.generateToken(user);

        return new GithubCallbackResponse(
                "Authentication successful.",
                user.getStudentId(),
                user.getGithubUsername(),
                token
        );
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException(message);
        }
        return value.trim();
    }

    public String generateGithubAuthorizationUrl(String studentId) {
    String validatedStudentId = requireText(studentId, "studentId is required.");

    // student var mı kontrol
    boolean exists = studentRegistrationService.validateStudent(validatedStudentId);
    if (!exists) {
        throw new BadRequestException("Student ID not found.");
    }

    String clientId = githubProperties.getClientId();
    String redirectUri = githubProperties.getRedirectUri();

    String scope = "read:user";
    String state = validatedStudentId;

    return "https://github.com/login/oauth/authorize"
            + "?client_id=" + clientId
            + "&redirect_uri=" + redirectUri
            + "&scope=" + scope
            + "&state=" + state;
    }

}
