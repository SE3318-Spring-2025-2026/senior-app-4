package com.spms.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables async method execution via @Async annotation.
 * Required for non-blocking sync operations.
 */
@Configuration
@EnableAsync
public class
AsyncConfig {
    // @EnableAsync is sufficient — default executor will be used
}
