package com.spms.backend.service;

import com.spms.backend.dto.request.GradingCriteriaCreateRequestDto;
import com.spms.backend.dto.response.GradingCriteriaDto;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.exception.NotFoundException;
import com.spms.backend.model.GradingCriteria;
import com.spms.backend.model.enums.DeliverableType;
import com.spms.backend.model.enums.GradingType;
import com.spms.backend.repository.GradingCriteriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class GradingCriteriaService {

    private final GradingCriteriaRepository criteriaRepository;

    public GradingCriteriaService(GradingCriteriaRepository criteriaRepository) {
        this.criteriaRepository = criteriaRepository;
    }

    @Transactional
    public GradingCriteriaDto create(GradingCriteriaCreateRequestDto request, Long creatorId) {
        DeliverableType deliverableType = parseDeliverableType(request.deliverableType());

        GradingCriteria criteria = new GradingCriteria();
        criteria.setDeliverableType(deliverableType);
        criteria.setName(request.name());
        criteria.setDescription(request.description());
        criteria.setWeight(request.weight());
        criteria.setCreatedBy(creatorId);
        criteria.setGradingType(parseGradingTypeOrDefault(request.gradingType()));
        requireWeightWithinDeliverableLimit(deliverableType, request.weight(), null);

        GradingCriteria saved = criteriaRepository.save(criteria);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<GradingCriteriaDto> list(String deliverableTypeParam) {
        if (deliverableTypeParam == null || deliverableTypeParam.isBlank()) {
            return criteriaRepository.findAll().stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
        }

        DeliverableType deliverableType = parseDeliverableType(deliverableTypeParam);
        return criteriaRepository.findByDeliverableType(deliverableType).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public GradingCriteriaDto update(Long id, GradingCriteriaCreateRequestDto request, String callerRole) {
        if (!"coordinator".equals(callerRole)) {
            throw new com.spms.backend.exception.ForbiddenException("Only coordinators can update grading criteria.");
        }
        GradingCriteria criteria = criteriaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Grading criteria not found: " + id));

        criteria.setDeliverableType(parseDeliverableType(request.deliverableType()));
        criteria.setName(request.name());
        criteria.setDescription(request.description());
        criteria.setWeight(request.weight());
        criteria.setGradingType(parseGradingTypeOrDefault(request.gradingType()));
        requireWeightWithinDeliverableLimit(criteria.getDeliverableType(), request.weight(), id);

        return toDto(criteriaRepository.save(criteria));
    }

    @Transactional
    public void delete(Long id, String callerRole) {
        if (!"coordinator".equals(callerRole)) {
            throw new com.spms.backend.exception.ForbiddenException("Only coordinators can delete grading criteria.");
        }
        if (!criteriaRepository.existsById(id)) {
            throw new NotFoundException("Grading criteria not found: " + id);
        }
        criteriaRepository.deleteById(id);
    }

    private DeliverableType parseDeliverableType(String value) {
        try {
            return DeliverableType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid deliverableType: " + value +
                    ". Must be one of: PROPOSAL, REVISED_PROPOSAL, STATEMENT_OF_WORK, DEMONSTRATION");
        }
    }

    private GradingType parseGradingTypeOrDefault(String value) {
        if (value == null || value.isBlank()) {
            return GradingType.SOFT;
        }
        return parseGradingType(value);
    }

    private GradingType parseGradingType(String value) {
        try {
            return GradingType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid gradingType: " + value + ". Must be one of: BINARY, SOFT");
        }
    }

    private void requireWeightWithinDeliverableLimit(DeliverableType deliverableType, Double requestedWeight, Long excludedId) {
        double total = criteriaRepository.findByDeliverableType(deliverableType)
                .stream()
                .filter(criteria -> excludedId == null || !excludedId.equals(criteria.getId()))
                .mapToDouble(criteria -> criteria.getWeight() != null ? criteria.getWeight() : 0.0)
                .sum();
        total += requestedWeight != null ? requestedWeight : 0.0;
        if (total > 100.0 + 0.01) {
            throw new BadRequestException("Grading criteria weights for " + deliverableType.name()
                    + " cannot exceed 100. Current total would be " + total);
        }
    }

    private GradingCriteriaDto toDto(GradingCriteria c) {
        return new GradingCriteriaDto(
                c.getId(),
                c.getDeliverableType().name(),
                c.getGradingType() != null ? c.getGradingType().name() : null,
                c.getName(),
                c.getDescription(),
                c.getWeight()
        );
    }
}
