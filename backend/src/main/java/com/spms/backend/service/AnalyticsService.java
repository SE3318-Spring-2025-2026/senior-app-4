package com.spms.backend.service;

import com.spms.backend.dto.response.StudentPerformanceDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AnalyticsService {
    Page<StudentPerformanceDto> getLeaderboard(Pageable pageable);
    void recalculateAllPerformances();
}
