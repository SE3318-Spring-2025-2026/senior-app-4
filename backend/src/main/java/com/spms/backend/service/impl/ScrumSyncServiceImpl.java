package com.spms.backend.service.impl;

import com.spms.backend.client.GithubApiClient;
import com.spms.backend.client.JiraApiClient;
import com.spms.backend.dto.response.ScrumSyncResponse;
import com.spms.backend.exception.SyncAlreadyRunningException;
import com.spms.backend.service.ScrumSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ISSUE #395: Implementation of Scrum Sync Service
 * Orchestrates synchronization of JIRA and GitHub data for active sprint.
 */
@Service
public class ScrumSyncServiceImpl implements ScrumSyncService {

    private static final Logger logger = LoggerFactory.getLogger(ScrumSyncServiceImpl.class);

    private final AtomicBoolean syncInProgress = new AtomicBoolean(false);
    private final JiraApiClient jiraApiClient;
    private final GithubApiClient githubApiClient;

    public ScrumSyncServiceImpl(JiraApiClient jiraApiClient, GithubApiClient githubApiClient) {
        this.jiraApiClient = jiraApiClient;
        this.githubApiClient = githubApiClient;
    }

    @Override
    public ScrumSyncResponse triggerSync() {
        if (!syncInProgress.compareAndSet(false, true)) {
            throw new SyncAlreadyRunningException("Another sync process is currently active");
        }

        String syncId = UUID.randomUUID().toString();
        Instant startedAt = Instant.now();

        logger.info("Sync triggered with ID: {}", syncId);

        // Execute async without blocking
        executeSyncAsync(syncId);

        return new ScrumSyncResponse(
            "STARTED",
            "Synchronization pipeline started",
            syncId,
            startedAt
        );
    }

    @Async
    protected void executeSyncAsync(String syncId) {
        try {
            logger.info("Executing sync pipeline for syncId: {}", syncId);
            executeSyncPipeline();
            logger.info("Sync completed successfully for syncId: {}", syncId);
        } catch (Exception e) {
            logger.error("Sync failed for syncId: {}", syncId, e);
        } finally {
            syncInProgress.set(false);
        }
    }

    @Override
    public void executeSyncPipeline() {
        // Phase 1: Find active sprint context
        logger.debug("Phase 1: Searching for active sprint context");
        // TODO: Implement active sprint lookup from database

        // Phase 2: Fetch data from JIRA
        logger.debug("Phase 2: Fetching data from JIRA");
        // TODO: Use jiraApiClient to fetch sprint issues

        // Phase 3: Fetch data from GitHub
        logger.debug("Phase 3: Fetching data from GitHub");
        // TODO: Use githubApiClient to fetch repository data

        // Phase 4: Merge and persist data
        logger.debug("Phase 4: Merging and persisting data");
        // TODO: Implement data merge and persistence logic

        logger.info("Sync pipeline execution completed");
    }

    @Override
    public boolean isSyncRunning() {
        return syncInProgress.get();
    }
}
