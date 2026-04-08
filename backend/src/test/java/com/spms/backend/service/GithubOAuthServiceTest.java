package com.spms.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spms.backend.client.GithubApiClient;
import com.spms.backend.config.GithubProperties;
import com.spms.backend.config.TokenProperties;
import com.spms.backend.dto.response.GithubCallbackResponse;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.exception.GithubAuthenticationException;
import com.spms.backend.repository.SupabaseUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GithubOAuthServiceTest {

    private StubGithubApiClient githubApiClient;
    private GithubOAuthService githubOAuthService;

    @BeforeEach
    void setUp() {
        githubApiClient = new StubGithubApiClient();

        StudentRegistrationService studentRegistrationService =
                new StudentRegistrationService(new SupabaseUserRepository());

        TokenProperties tokenProperties = new TokenProperties();
        tokenProperties.setSecret("test-token-secret");
        tokenProperties.setExpirationSeconds(3600);

        TokenService tokenService = new TokenService(tokenProperties, new ObjectMapper());
        githubOAuthService = new GithubOAuthService(githubApiClient, studentRegistrationService, tokenService);
    }

    @Test
    void handleCallbackThrows400WhenCodeIsMissing() {
        assertThrows(BadRequestException.class, () -> githubOAuthService.handleCallback(null, "11070001000"));
    }

    @Test
    void handleCallbackThrows400WhenStateIsMissing() {
        assertThrows(BadRequestException.class, () -> githubOAuthService.handleCallback("valid-code", " "));
    }

    @Test
    void handleCallbackThrows401WhenTokenExchangeFails() {
        githubApiClient.tokenException = new GithubAuthenticationException("GitHub authentication failed.");

        assertThrows(
                GithubAuthenticationException.class,
                () -> githubOAuthService.handleCallback("valid-code", "11070001000")
        );
    }

    @Test
    void handleCallbackThrows401WhenGithubUserFetchFails() {
        githubApiClient.userException = new GithubAuthenticationException("GitHub authentication failed.");

        assertThrows(
                GithubAuthenticationException.class,
                () -> githubOAuthService.handleCallback("valid-code", "11070001000")
        );
    }

    @Test
    void handleCallbackReturnsResponseWhenGithubAuthenticationSucceeds() {
        GithubCallbackResponse response = githubOAuthService.handleCallback("valid-code", "11070001000");

        assertEquals("Authentication successful.", response.message());
        assertEquals("11070001000", response.studentId());
        assertEquals("furkangncr", response.githubUsername());
        assertNotNull(response.token());
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
