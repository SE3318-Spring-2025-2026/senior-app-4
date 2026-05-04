package com.spms.backend.controller;

import com.spms.backend.dto.response.StudentPerformanceDto;
import com.spms.backend.service.AnalyticsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<Page<StudentPerformanceDto>> getLeaderboard(
            @PageableDefault(size = 10, sort = "performanceRatio") Pageable pageable) {
        return ResponseEntity.ok(analyticsService.getLeaderboard(pageable));
    }

    @PostMapping("/recalculate")
    public ResponseEntity<Void> recalculate() {
        analyticsService.recalculateAllPerformances();
        return ResponseEntity.ok().build();
    }
}
