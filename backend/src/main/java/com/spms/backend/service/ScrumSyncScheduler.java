package com.spms.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * ISSUE #395: Cron-based Synchronization Scheduler
 * Automatically triggers scrum sync at a configured time daily.
 */
@Component
public class ScrumSyncScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ScrumSyncScheduler.class);

    private final ScrumSyncService scrumSyncService;

    // Cron expression from application.yml: scrum.sync.cron
    @Value("${scrum.sync.cron:0 0 2 * * *}")
    private String syncCronExpression;

    public ScrumSyncScheduler(ScrumSyncService scrumSyncService) {
        this.scrumSyncService = scrumSyncService;
    }

    /**
     * Scheduled task that triggers sync at configured cron time.
     * Default: 2 AM daily (0 0 2 * * *)
     * Configurable via: scrum.sync.cron environment variable or application.yml
     * Format: second minute hour day month dayOfWeek
     */
    @Scheduled(cron = "${scrum.sync.cron:0 0 2 * * *}")
    public void scheduledSyncTask() {
        logger.info("Starting scheduled scrum sync task");

        // Skip if sync already running (already checked in triggerSync)
        if (scrumSyncService.isSyncRunning()) {
            logger.warn("Skipping scheduled sync: another sync is already in progress");
            return;
        }

        try {
            scrumSyncService.triggerSync();
        } catch (Exception e) {
            logger.error("Scheduled sync task failed", e);
            // Don't re-throw — let scheduler continue
        }
    }
}
