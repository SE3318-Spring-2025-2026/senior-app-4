package com.spms.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring'in @Scheduled cron job desteğini aktif eder.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // Sadece @EnableScheduling yeterli — ek bean tanımı gerekmez.
}
