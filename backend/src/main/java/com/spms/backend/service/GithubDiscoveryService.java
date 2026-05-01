package com.spms.backend.service;

import com.spms.backend.dto.response.BranchMatchDto;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class GithubDiscoveryService {

    private final RestTemplate restTemplate;

    public GithubDiscoveryService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
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
