package com.spms.backend.service;

import com.spms.backend.dto.response.ActiveSprintDto;

/**
 * ISSUE #396: Service for finding active sprint context
 * Used by Cron Service (#395) to determine which sprint to synchronize
 */
public interface ActiveSprintService {

    /**
     * Find the currently active sprint based on today's date.
     * Searches for a sprint where today falls within start_date and end_date
     * and the status is 'Active'.
     *
     * @return ActiveSprintDto containing sprint details
     * @throws com.spms.backend.exception.NotFoundException if no active sprint found
     */
    ActiveSprintDto getActiveSprint();
}
