package com.spms.backend.service;

import com.spms.backend.dto.request.SprintCreateRequestDto;
import com.spms.backend.dto.request.SprintUpdateRequestDto;
import com.spms.backend.dto.response.SprintDto;

import java.util.List;

public interface CoordinatorSprintService {
    List<SprintDto> getAllSprints();
    SprintDto createSprint(SprintCreateRequestDto request);
    SprintDto updateSprint(Long sprintId, SprintUpdateRequestDto request);
    void deleteSprint(Long sprintId);
}
