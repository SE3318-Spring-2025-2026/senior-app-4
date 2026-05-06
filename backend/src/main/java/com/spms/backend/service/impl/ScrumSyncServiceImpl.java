package com.spms.backend.service.impl;

import com.spms.backend.dto.response.ScrumSyncResponse;
import com.spms.backend.exception.SyncAlreadyRunningException;
import com.spms.backend.model.Group;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.SprintRepository;
import com.spms.backend.service.ScrumSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ScrumSyncServiceImpl implements ScrumSyncService {

    private static final Logger logger = LoggerFactory.getLogger(ScrumSyncServiceImpl.class);

    private final AtomicBoolean syncInProgress = new AtomicBoolean(false);
    private final GroupRepository groupRepository;
    private final SprintRepository sprintRepository;
    private final ScrumSyncGroupProcessor groupProcessor;

    public ScrumSyncServiceImpl(GroupRepository groupRepository,
                                SprintRepository sprintRepository,
                                ScrumSyncGroupProcessor groupProcessor) {
        this.groupRepository = groupRepository;
        this.sprintRepository = sprintRepository;
        this.groupProcessor = groupProcessor;
    }

    @Override
    public ScrumSyncResponse triggerSync() {
        if (!syncInProgress.compareAndSet(false, true)) {
            throw new SyncAlreadyRunningException("Another sync process is currently active");
        }

        String syncId = UUID.randomUUID().toString();
        Instant startedAt = Instant.now();
        logger.info("Sync triggered with ID: {}", syncId);

        executeSyncAsync(syncId);

        return new ScrumSyncResponse("STARTED", "Synchronization pipeline started", syncId, startedAt);
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
        logger.debug("Phase 1: Searching for active sprint context");
        var activeSprint = sprintRepository.findActiveSprintByDate(java.time.LocalDate.now());
        if (activeSprint.isEmpty()) {
            logger.warn("No active sprint found, skipping sync");
            return;
        }

        var sprint = activeSprint.get();
        List<Group> allGroups = groupRepository.findAll();
        logger.debug("Found {} groups to sync", allGroups.size());

        for (Group group : allGroups) {
            try {
                groupProcessor.processGroup(group, sprint);
            } catch (Exception e) {
                logger.error("Failed to sync group: {}", group.getId(), e);
            }
        }

        logger.info("Sync pipeline execution completed");
    }

    @Override
    public boolean isSyncRunning() {
        return syncInProgress.get();
    }
}
