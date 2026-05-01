package com.spms.backend.controller;

import com.spms.backend.dto.request.DiscoveryRequest;
import com.spms.backend.dto.response.BranchMatchDto;
import com.spms.backend.dto.response.DiscoveryResponse;
import com.spms.backend.service.GithubDiscoveryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class GithubDiscoveryController {

    private final GithubDiscoveryService githubDiscoveryService;

    public GithubDiscoveryController(GithubDiscoveryService githubDiscoveryService) {
        this.githubDiscoveryService = githubDiscoveryService;
    }

    @PostMapping("/github-discovery/start")
    public ResponseEntity<DiscoveryResponse> startDiscovery() {
        return ResponseEntity.ok(new DiscoveryResponse("STARTED", "Discovery workflow initiated."));
    }

    @GetMapping("/github/branch-query")
    public ResponseEntity<List<BranchMatchDto>> queryBranches(
            @RequestParam String owner,
            @RequestParam String repo,
            @RequestParam List<String> jiraIds) {
        
        List<BranchMatchDto> matches = githubDiscoveryService.matchBranchesWithJiraIds(owner, repo, jiraIds);
        return ResponseEntity.ok(matches);
    }
}
