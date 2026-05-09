package com.spms.backend.service;

import com.spms.backend.dto.response.ScrumSyncResponse;

/**
 * ISSUE #395: Cron Service & Manual Sync Trigger
 * Handles synchronization of data from JIRA and GitHub for active sprint context.
 */
public interface ScrumSyncService {

    /**
     * Trigger synchronization pipeline asynchronously.
     * Searches for active sprint context and syncs JIRA/GitHub data.
     * Returns immediately without blocking the caller.
     *
     * @return ScrumSyncResponse containing status and sync ID
     * @throws SyncAlreadyRunningException if a sync is already in progress
     */
    ScrumSyncResponse triggerSync();

    /**
     * Trigger synchronization pipeline asynchronously for a specific group.
     * @param groupId the ID of the group to sync
     * @return ScrumSyncResponse containing status and sync ID
     * @throws SyncAlreadyRunningException if a sync is already in progress
     */
    ScrumSyncResponse triggerSyncForGroup(Long groupId);

    /**
     * Execute the actual synchronization (called by cron or triggerSync).
     * Should be resilient to concurrent calls and prevent duplicate execution.
     */
    void executeSyncPipeline();

    /**
     * Check if a sync operation is currently running.
     *
     * @return true if sync is in progress, false otherwise
     */
    boolean isSyncRunning();
}
