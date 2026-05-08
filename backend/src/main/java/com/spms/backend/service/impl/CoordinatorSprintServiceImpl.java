package com.spms.backend.service.impl;

import com.spms.backend.dto.request.SprintCreateRequestDto;
import com.spms.backend.dto.request.SprintUpdateRequestDto;
import com.spms.backend.dto.response.SprintDto;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.exception.NotFoundException;
import com.spms.backend.model.Sprint;
import com.spms.backend.repository.SprintRepository;
import com.spms.backend.service.CoordinatorSprintService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class CoordinatorSprintServiceImpl implements CoordinatorSprintService {

    private final SprintRepository sprintRepository;

    public CoordinatorSprintServiceImpl(SprintRepository sprintRepository) {
        this.sprintRepository = sprintRepository;
    }

    @Override
    public List<SprintDto> getAllSprints() {
        LocalDate today = LocalDate.now();
        return sprintRepository.findAll().stream()
                .sorted((a, b) -> a.getStartDate().compareTo(b.getStartDate()))
                .map(s -> toDto(s, today))
                .toList();
    }

    @Override
    public SprintDto createSprint(SprintCreateRequestDto request) {
        validateDates(request.getStartDate(), request.getEndDate(), null);
        checkOverlap(request.getStartDate(), request.getEndDate(), null);

        Sprint sprint = new Sprint();
        sprint.setSprintName(request.getSprintName().trim());
        sprint.setStartDate(request.getStartDate());
        sprint.setEndDate(request.getEndDate());
        sprint.setRequiredStoryPoints(request.getRequiredStoryPoints());
        sprint.setCreatedAt(Instant.now());
        sprint.setUpdatedAt(Instant.now());

        LocalDate today = LocalDate.now();
        sprint.setStatus(computeStatus(request.getStartDate(), request.getEndDate(), today));

        Sprint saved = sprintRepository.save(sprint);
        return toDto(saved, today);
    }

    @Override
    public SprintDto updateSprint(Long sprintId, SprintUpdateRequestDto request) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new NotFoundException("Sprint not found: " + sprintId));

        validateDates(request.getStartDate(), request.getEndDate(), null);
        checkOverlap(request.getStartDate(), request.getEndDate(), sprintId);

        sprint.setSprintName(request.getSprintName().trim());
        sprint.setStartDate(request.getStartDate());
        sprint.setEndDate(request.getEndDate());
        sprint.setRequiredStoryPoints(request.getRequiredStoryPoints());
        sprint.setUpdatedAt(Instant.now());

        LocalDate today = LocalDate.now();
        sprint.setStatus(computeStatus(request.getStartDate(), request.getEndDate(), today));

        Sprint saved = sprintRepository.save(sprint);
        return toDto(saved, today);
    }

    @Override
    public void deleteSprint(Long sprintId) {
        if (!sprintRepository.existsById(sprintId)) {
            throw new NotFoundException("Sprint not found: " + sprintId);
        }
        sprintRepository.deleteById(sprintId);
    }

    private void validateDates(LocalDate start, LocalDate end, Object unused) {
        if (!end.isAfter(start)) {
            throw new BadRequestException("End date must be after start date.");
        }
    }

    private void checkOverlap(LocalDate start, LocalDate end, Long excludeId) {
        List<Sprint> all = sprintRepository.findAll();
        for (Sprint existing : all) {
            if (excludeId != null && excludeId.equals(existing.getId())) continue;
            boolean overlaps = start.isBefore(existing.getEndDate()) && end.isAfter(existing.getStartDate());
            if (overlaps) {
                throw new BadRequestException(
                        "Sprint dates overlap with existing sprint \"" + existing.getSprintName() + "\" (" +
                        existing.getStartDate() + " – " + existing.getEndDate() + ")."
                );
            }
        }
    }

    private String computeStatus(LocalDate start, LocalDate end, LocalDate today) {
        if (today.isBefore(start)) return "UPCOMING";
        if (!today.isAfter(end)) return "ACTIVE";
        return "COMPLETED";
    }

    private SprintDto toDto(Sprint sprint, LocalDate today) {
        String status = computeStatus(sprint.getStartDate(), sprint.getEndDate(), today);
        return new SprintDto(sprint.getId(), sprint.getSprintName(), sprint.getStartDate(), sprint.getEndDate(), status, sprint.getRequiredStoryPoints());
    }
}
