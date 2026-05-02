package com.spms.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/pr-verification")
public class PrVerificationController {

    // AC1: PR Verification (Sadece Merged olanlar puanlanır)
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPrStatus(@RequestBody Map<String, Object> payload) {
        Boolean isMerged = (Boolean) payload.getOrDefault("merged", false);
        String status = (String) payload.getOrDefault("status", "closed");

        if ("closed".equals(status) && Boolean.FALSE.equals(isMerged)) {
            return ResponseEntity.ok(Map.of(
                "verified", false, 
                "reason", "PR closed but unmerged. No points."
            ));
        }
        
        if (Boolean.TRUE.equals(isMerged)) {
            return ResponseEntity.ok(Map.of(
                "verified", true, 
                "reason", "PR successfully merged."
            ));
        }

        return ResponseEntity.badRequest().body(Map.of("error", "Invalid PR data"));
    }

    // Branch Discovery (İleride GitHub entegrasyonu ile dolacak)
    @PostMapping("/branch-list")
    public ResponseEntity<?> getBranchList() {
        return ResponseEntity.ok(Map.of(
            "branches", List.of("main", "feature/PROJ-123", "bugfix/PROJ-456")
        ));
    }
}