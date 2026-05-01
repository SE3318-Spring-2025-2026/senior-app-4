package com.spms.backend.service.impl;

import com.spms.backend.dto.response.ActiveSprintDto;
import com.spms.backend.exception.NotFoundException;
import com.spms.backend.model.Sprint;
import com.spms.backend.repository.SprintRepository;
import com.spms.backend.service.ActiveSprintService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * ISSUE #396: Implementation of Active Sprint Service
 * Finds the active sprint based on current date
 */
@Service
public class ActiveSprintServiceImpl implements ActiveSprintService {

    private static final Logger logger = LoggerFactory.getLogger(ActiveSprintServiceImpl.class);

    private final SprintRepository sprintRepository;

    public ActiveSprintServiceImpl(SprintRepository sprintRepository) {
        this.sprintRepository = sprintRepository;
    }

    @Override
    public ActiveSprintDto getActiveSprint() {
        LocalDate today = LocalDate.now();
        logger.debug("Searching for active sprint on date: {}", today);

        Sprint activeSprint = sprintRepository.findActiveSprintByDate(today)
                .orElseThrow(() -> {
                    logger.warn("No active sprint found for date: {}", today);
                    return new NotFoundException("Active sprint not found for the current date");
                });

        logger.info("Found active sprint: {} (ID: {})", activeSprint.getSprintName(), activeSprint.getId());

        return new ActiveSprintDto(
                activeSprint.getId(),
                activeSprint.getSprintName(),
                activeSprint.getStartDate(),
                activeSprint.getEndDate(),
                activeSprint.getStatus()
        );
    }
}
