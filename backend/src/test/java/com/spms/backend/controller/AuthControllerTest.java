package com.spms.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spms.backend.client.GithubApiClient;
import com.spms.backend.config.GithubProperties;
import com.spms.backend.config.TokenProperties;
import com.spms.backend.exception.GithubAuthenticationException;
import com.spms.backend.exception.GlobalExceptionHandler;
import com.spms.backend.repository.SupabaseUserRepository;
import com.spms.backend.service.GithubOAuthService;
import com.spms.backend.service.StudentRegistrationService;
import com.spms.backend.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private MockMvc mockMvc;
    private StubGithubApiClient githubApiClient;

    @BeforeEach
    void setUp() {
        githubApiClient = new StubGithubApiClient();

        StudentRegistrationService studentRegistrationService =
                new StudentRegistrationService(new SupabaseUserRepository());

        TokenProperties tokenProperties = new TokenProperties();
        tokenProperties.setSecret("test-token-secret");
        tokenProperties.setExpirationSeconds(3600);

        TokenService tokenService = new TokenService(tokenProperties, new ObjectMapper());
        GithubOAuthService githubOAuthService =
                new GithubOAuthService(githubApiClient, studentRegistrationService, tokenService);

        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(githubOAuthService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void githubCallbackReturns400WhenCodeIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/auth/github/callback")
                        .queryParam("state", "11070001000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("GitHub authorization code is required."));
    }

    @Test
    void githubCallbackReturns400WhenStateIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/auth/github/callback")
                        .queryParam("code", "valid-code"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("state is required and must contain the validated studentId."));
    }

    @Test
    void githubCallbackReturns401WhenGithubAuthenticationFails() throws Exception {
        githubApiClient.tokenException = new GithubAuthenticationException("GitHub authentication failed.");

        mockMvc.perform(get("/api/v1/auth/github/callback")
                        .queryParam("code", "valid-code")
                        .queryParam("state", "11070001000"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("GitHub authentication failed."));
    }

    @Test
    void githubCallbackReturns200WhenAuthenticationSucceeds() throws Exception {
        mockMvc.perform(get("/api/v1/auth/github/callback")
                        .queryParam("code", "valid-code")
                        .queryParam("state", "11070001000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Authentication successful."))
                .andExpect(jsonPath("$.studentId").value("11070001000"))
                .andExpect(jsonPath("$.githubUsername").value("furkangncr"))
                .andExpect(jsonPath("$.token").isString());
    }

    private static final class StubGithubApiClient extends GithubApiClient {

        private RuntimeException tokenException;
        private RuntimeException userException;

        private StubGithubApiClient() {
            super(RestClient.builder(), new GithubProperties());
        }

        @Override
        public String exchangeCodeForAccessToken(String code) {
            if (tokenException != null) {
                throw tokenException;
            }
            return "gho_valid";
        }

        @Override
        public String fetchGithubUsername(String accessToken) {
            if (userException != null) {
                throw userException;
            }
            return "furkangncr";
        }
    }
}
