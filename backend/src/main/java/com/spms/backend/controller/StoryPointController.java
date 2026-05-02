package com.spms.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Issue #14 AC2 & AC3 — Story Point Math Engine
 * POST /api/v1/story-points/validate
 *
 * AC2: targetSP = 0 → performanceRatio = 0.0 (ZeroDivision prevention)
 * AC3: ratio > 1.0 → capped at 1.0 (100% cap limit)
 * Boundary: negative completedSP or targetSP → 400 Bad Request
 */
@RestController
@RequestMapping("/api/v1/story-points")
public class StoryPointController {

    @PostMapping("/validate")
    public ResponseEntity<?> validateStoryPoints(@RequestBody Map<String, Object> request) {
        if (!request.containsKey("completedSP") || !request.containsKey("targetSP")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing required fields"));
        }

        double completedSP;
        double targetSP;
        try {
            completedSP = Double.parseDouble(request.get("completedSP").toString());
            targetSP   = Double.parseDouble(request.get("targetSP").toString());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid number format"));
        }

        // Boundary: reject negatives
        if (completedSP < 0 || targetSP < 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Story points cannot be negative"));
        }

        double ratio;
        if (targetSP == 0) {
            // AC2: ZeroDivision prevention
            ratio = 0.0;
        } else {
            ratio = completedSP / targetSP;
            // AC3: 100% cap limit
            if (ratio > 1.0) {
                ratio = 1.0;
            }
        }

        return ResponseEntity.ok(Map.of("performanceRatio", ratio));
    }
    // Merge Statüsüne Göre SP Uygunluğu Endpoint'i
    @PostMapping("/merge-status")
    public ResponseEntity<?> checkMergeStatus(@RequestBody Map<String, Object> request) {
        Boolean isMerged = (Boolean) request.getOrDefault("merged", false);
        
        return ResponseEntity.ok(Map.of(
                "eligibleForPoints", isMerged,
                "message", isMerged ? "Task is merged. Points will be calculated." : "Task is NOT merged. No points."
        ));
    }
}
