package com.spms.backend.service;

import com.spms.backend.dto.response.BranchMatchDto;
import com.spms.backend.model.GithubIntegration;
import com.spms.backend.repository.GithubIntegrationRepository;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class GithubDiscoveryService {

    private final RestTemplate restTemplate;
    private final GithubIntegrationRepository githubIntegrationRepository;

    public GithubDiscoveryService(RestTemplateBuilder restTemplateBuilder,
                                 GithubIntegrationRepository githubIntegrationRepository) {
        this.restTemplate = restTemplateBuilder.build();
        this.githubIntegrationRepository = githubIntegrationRepository;
    }

    public Optional<String> findBranchForIssueKey(Long groupId, String issueKey, String repositoryName) {
        try {
            GithubIntegration integration = githubIntegrationRepository.findByGroup_Id(groupId)
                    .orElseThrow(() -> new IllegalArgumentException("GitHub integration not found for group: " + groupId));

            // EncryptionConverter already decrypts on read — no extra decrypt needed
            String decryptedPat = integration.getGithubPatEncrypted();
            String orgName = integration.getOrganizationName();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(decryptedPat);
            headers.set("Accept", "application/vnd.github+json");
            headers.set("X-GitHub-Api-Version", "2022-11-28");
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String keyLower = issueKey.toLowerCase();
            int page = 1;
            while (true) {
                String url = String.format(
                        "https://api.github.com/repos/%s/%s/branches?per_page=100&page=%d",
                        orgName, repositoryName, page);

                ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                        url, HttpMethod.GET, entity,
                        new ParameterizedTypeReference<List<Map<String, Object>>>() {});

                List<Map<String, Object>> branches = response.getBody();
                if (branches == null || branches.isEmpty()) break;

                for (Map<String, Object> branchObj : branches) {
                    String branchName = (String) branchObj.get("name");
                    if (branchName == null) continue;
                    String nameLower = branchName.toLowerCase();
                    if (nameLower.contains(keyLower + "-") ||
                        nameLower.contains(keyLower + "/") ||
                        nameLower.endsWith(keyLower) ||
                        nameLower.equals(keyLower)) {
                        return Optional.of(branchName);
                    }
                }

                if (branches.size() < 100) break;
                page++;
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public List<BranchMatchDto> matchBranchesWithJiraIds(String owner, String repo, List<String> jiraIds) {
        String url = String.format("https://api.github.com/repos/%s/%s/branches", owner, repo);

        ResponseEntity<List<Map<String, Object>>> response;
        try {
            response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
        } catch (Exception e) {
            return new ArrayList<>(); // Return empty on API failure or not found
        }

        List<BranchMatchDto> matches = new ArrayList<>();
        List<Map<String, Object>> branches = response.getBody();
        if (branches == null) return matches;

        for (Map<String, Object> branchObj : branches) {
            String branchName = (String) branchObj.get("name");
            if (branchName == null) continue;

            for (String jiraId : jiraIds) {
                // regex case-insensitive matching
                Pattern pattern = Pattern.compile(".*" + Pattern.quote(jiraId) + ".*", Pattern.CASE_INSENSITIVE);
                if (pattern.matcher(branchName).matches()) {
                    matches.add(new BranchMatchDto(branchName, jiraId));
                    break; // Move to the next branch after finding a match
                }
            }
        }
        return matches;
    }
}
