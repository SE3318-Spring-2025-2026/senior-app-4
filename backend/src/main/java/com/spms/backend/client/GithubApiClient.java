package com.spms.backend.client;

import com.spms.backend.config.GithubProperties;
import com.spms.backend.dto.PrCheckResult;
import com.spms.backend.dto.external.GithubAccessTokenResponse;
import com.spms.backend.dto.external.GithubUserResponse;
import com.spms.backend.exception.GithubAuthenticationException;
import com.spms.backend.exception.P7ApiException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    public GithubUserResponse fetchGithubUser(String accessToken) {
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

            return response;
        } catch (RestClientException exception) {
            throw new GithubAuthenticationException("GitHub authentication failed.", exception);
        }
    }

    public String fetchGithubUsername(String accessToken) {
        return fetchGithubUser(accessToken).login().trim();
    }

    // issue 19 için eklenen method dk
    public boolean validateOrganizationAccess(String organizationName, String pat) {
        String url = "https://api.github.com/orgs/" + organizationName;

        try {
            ResponseEntity<Void> response = restClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + pat)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
                    .retrieve()
                    .toBodilessEntity();

            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException exception) {
            return false;
        }
    }

    /**
     * Find merged PR for a given branch
     * @param orgName GitHub organization name
     * @param repoName GitHub repository name
     * @param branchName branch name to search for
     * @param pat Personal Access Token
     * @return Optional containing PR check result if PR found, empty otherwise
     */
    public Optional<PrCheckResult> findMergedPrForBranch(String orgName, String repoName, String branchName, String pat) {
        // head filter ile tüm closed PR'ları çek, base branch fark etmez
        String url = String.format("https://api.github.com/repos/%s/%s/pulls?head=%s:%s&state=closed&per_page=5",
                orgName, repoName, orgName, branchName);

        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> response = restClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + pat)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .retrieve()
                    .body((Class<List<Map<String, Object>>>) (Class<?>) List.class);

            if (response == null || response.isEmpty()) {
                return Optional.empty();
            }

            // merged_at dolu olan ilk PR'ı bul
            for (Map<String, Object> pr : response) {
                Object mergedAtObj = pr.get("merged_at");
                if (mergedAtObj == null) continue;

                Long prNumber = ((Number) pr.get("number")).longValue();
                @SuppressWarnings("unchecked")
                Map<String, Object> userMap = (Map<String, Object>) pr.get("user");
                String authorLogin = userMap != null ? (String) userMap.get("login") : "unknown";

                return Optional.of(new PrCheckResult(prNumber, true, authorLogin));
            }

            return Optional.empty();
        } catch (RestClientException exception) {
            return Optional.empty();
        }
    }

    /**
     * Fetch PR review comments (review threads) for a given PR number.
     * Returns raw review objects: [{id, user.login, body, state}]
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchPrReviews(String orgName, String repoName, long prNumber, String pat) {
        String url = String.format("https://api.github.com/repos/%s/%s/pulls/%d/reviews",
                orgName, repoName, prNumber);
        try {
            List<Map<String, Object>> reviews = restClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + pat)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .retrieve()
                    .body((Class<List<Map<String, Object>>>) (Class<?>) List.class);
            return reviews != null ? reviews : Collections.emptyList();
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            if (status == 429 || status == 403) {
                throw new P7ApiException(HttpStatus.BAD_GATEWAY, "GITHUB_RATE_LIMITED",
                        "GitHub API rate limited (HTTP " + status + ")");
            }
            return Collections.emptyList();
        } catch (RestClientException ex) {
            return Collections.emptyList();
        }
    }

    /**
     * Fetch the unified diff of changed files for a PR.
     * Returns raw file objects: [{filename, patch, status}]
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchPrFiles(String orgName, String repoName, long prNumber, String pat) {
        String url = String.format("https://api.github.com/repos/%s/%s/pulls/%d/files",
                orgName, repoName, prNumber);
        try {
            List<Map<String, Object>> files = restClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + pat)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .retrieve()
                    .body((Class<List<Map<String, Object>>>) (Class<?>) List.class);
            return files != null ? files : Collections.emptyList();
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            if (status == 429 || status == 403) {
                throw new P7ApiException(HttpStatus.BAD_GATEWAY, "GITHUB_RATE_LIMITED",
                        "GitHub API rate limited (HTTP " + status + ")");
            }
            return Collections.emptyList();
        } catch (RestClientException ex) {
            return Collections.emptyList();
        }
    }

    /**
     * Fetch PR review comments (inline comments on code) for a given PR.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchPrReviewComments(String orgName, String repoName, long prNumber, String pat) {
        String url = String.format("https://api.github.com/repos/%s/%s/pulls/%d/comments",
                orgName, repoName, prNumber);
        try {
            List<Map<String, Object>> comments = restClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + pat)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .retrieve()
                    .body((Class<List<Map<String, Object>>>) (Class<?>) List.class);
            return comments != null ? comments : Collections.emptyList();
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            if (status == 429 || status == 403) {
                throw new P7ApiException(HttpStatus.BAD_GATEWAY, "GITHUB_RATE_LIMITED",
                        "GitHub API rate limited (HTTP " + status + ")");
            }
            return Collections.emptyList();
        } catch (RestClientException ex) {
            return Collections.emptyList();
        }
    }
}