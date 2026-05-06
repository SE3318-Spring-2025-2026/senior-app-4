package com.spms.backend.service.impl;

import com.spms.backend.dto.response.ScrumSyncResponse;
import com.spms.backend.exception.SyncAlreadyRunningException;
import com.spms.backend.service.ScrumSyncService;
import com.spms.backend.util.ScrumSyncServiceHelper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * ISSUE #395: Implementation of Scrum Sync Service
 * Orchestrates synchronization of JIRA and GitHub data for active sprint.
 */
@Service
@RequiredArgsConstructor
public class ScrumSyncServiceImpl implements ScrumSyncService {
    private static final Logger logger = LoggerFactory.getLogger(ScrumSyncServiceImpl.class);

    private final ScrumSyncServiceHelper scrumSyncServiceHelper;

    @Override
    public ScrumSyncResponse triggerSync() {
        if (!scrumSyncServiceHelper.getSyncInProgress().compareAndSet(false, true)) {
            throw new SyncAlreadyRunningException("Another sync process is currently active");
        }

        String syncId = UUID.randomUUID().toString();
        Instant startedAt = Instant.now();

        logger.info("Sync triggered with ID: {}", syncId);

        // Execute async without blocking
        scrumSyncServiceHelper.executeSyncAsync(syncId);

        return new ScrumSyncResponse(
                "STARTED",
                "Synchronization pipeline started",
                syncId,
                startedAt
        );
    }

    @Override
    public boolean isSyncRunning() {
        return scrumSyncServiceHelper.getSyncInProgress().get();
    }
}
