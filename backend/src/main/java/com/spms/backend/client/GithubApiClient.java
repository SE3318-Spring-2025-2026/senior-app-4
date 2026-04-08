package com.spms.backend.client;

import com.spms.backend.config.GithubProperties;
import com.spms.backend.dto.external.GithubAccessTokenResponse;
import com.spms.backend.dto.external.GithubUserResponse;
import com.spms.backend.exception.GithubAuthenticationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class GithubApiClient {

    private static final String GITHUB_TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String GITHUB_USER_URL = "https://api.github.com/user";

    private final RestClient restClient;
    private final GithubProperties githubProperties;

    public GithubApiClient(RestClient.Builder restClientBuilder, GithubProperties githubProperties) {
        this.restClient = restClientBuilder.build();
        this.githubProperties = githubProperties;
    }

    public String exchangeCodeForAccessToken(String code) {
        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", githubProperties.getClientId());
        form.add("client_secret", githubProperties.getClientSecret());
        form.add("code", code);
        if (StringUtils.hasText(githubProperties.getRedirectUri())) {
            form.add("redirect_uri", githubProperties.getRedirectUri().trim());
        }

        try {
            GithubAccessTokenResponse response = restClient.post()
                    .uri(GITHUB_TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(form)
                    .retrieve()
                    .body(GithubAccessTokenResponse.class);

            if (response == null || !StringUtils.hasText(response.accessToken())) {
                throw new GithubAuthenticationException("GitHub authentication failed.");
            }

            return response.accessToken().trim();
        } catch (RestClientException exception) {
            throw new GithubAuthenticationException("GitHub authentication failed.", exception);
        }
    }

    public String fetchGithubUsername(String accessToken) {
        try {
            GithubUserResponse response = restClient.get()
                    .uri(GITHUB_USER_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(GithubUserResponse.class);

            if (response == null || !StringUtils.hasText(response.login())) {
                throw new GithubAuthenticationException("GitHub authentication failed.");
            }

            return response.login().trim();
        } catch (RestClientException exception) {
            throw new GithubAuthenticationException("GitHub authentication failed.", exception);
        }
    }
}
