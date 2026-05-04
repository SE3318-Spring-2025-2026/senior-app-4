package com.spms.backend.service.impl;

import com.spms.backend.dto.response.StudentPerformanceDto;
import com.spms.backend.model.StudentPerformance;
import com.spms.backend.model.User;
import com.spms.backend.repository.StudentPerformanceRepository;
import com.spms.backend.repository.UserRepository;
import com.spms.backend.service.AnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsServiceImpl.class);
    private final StudentPerformanceRepository performanceRepository;
    private final UserRepository userRepository;

    public AnalyticsServiceImpl(StudentPerformanceRepository performanceRepository, UserRepository userRepository) {
        this.performanceRepository = performanceRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Page<StudentPerformanceDto> getLeaderboard(Pageable pageable) {
        try {
            return performanceRepository.findAll(pageable).map(p -> {
                User u = p.getUser();
                return new StudentPerformanceDto(
                    u != null ? u.getUserId() : -1L,
                    u != null ? u.getFullName() : "Unknown Student",
                    p.getAssignedSp() != null ? p.getAssignedSp() : 0,
                    p.getAccomplishedSp() != null ? p.getAccomplishedSp() : 0,
                    p.getRatio() != null ? p.getRatio() : 0.0
                );
            });
        } catch (Exception e) {
            logger.error("CRITICAL ERROR in getLeaderboard: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public void recalculateAllPerformances() {
        List<User> students = userRepository.findAllByRoleIgnoreCase("student");

        for (User student : students) {
            // Real integration logic will go here
            int assigned = 0; 
            int accomplished = 0;
            double ratio = calculateRatio(assigned, accomplished);

            StudentPerformance perf = performanceRepository.findByUser_UserId(student.getUserId())
                .orElse(new StudentPerformance());
            
            perf.setUser(student);
            perf.setAssignedSp(assigned);
            perf.setAccomplishedSp(accomplished);
            perf.setRatio(ratio);
            perf.setLastUpdatedAt(Instant.now());

            performanceRepository.save(perf);
        }
    }

    /**
     * Issue #14 Math Engine Core Logic
     */
    private double calculateRatio(int assigned, int accomplished) {
        if (assigned <= 0) {
            return 0.0; // ZeroDivision prevention (AC2)
        }
        
        double ratio = (double) accomplished / assigned;
        
        if (ratio > 1.0) {
            return 1.0; // 100% cap limit (AC3)
        }
        
        if (ratio < 0) {
            return 0.0; // Negative prevention
        }
        
        return ratio;
    }
}
