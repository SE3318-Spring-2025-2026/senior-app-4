package com.spms.backend.service;

import com.spms.backend.dto.request.GradingCriteriaCreateRequestDto;
import com.spms.backend.dto.response.GradingCriteriaDto;
import com.spms.backend.model.DeliverableType;
import com.spms.backend.model.GradingCriteria;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.exception.ForbiddenException;
import com.spms.backend.repository.GradingCriteriaRepository;
import com.spms.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GradingCriteriaService {

    private final GradingCriteriaRepository criteriaRepository;
    private final UserRepository userRepository;

    public GradingCriteriaService(GradingCriteriaRepository criteriaRepository,
                                   UserRepository userRepository) {
        this.criteriaRepository = criteriaRepository;
        this.userRepository = userRepository;
    }

    public GradingCriteriaDto create(GradingCriteriaCreateRequestDto request, Long creatorId) {
        var user = userRepository.findByUserId(creatorId)
                .orElseThrow(() -> new BadRequestException("User not found."));

        if (!"coordinator".equalsIgnoreCase(user.getRole())) {
            throw new ForbiddenException("Only coordinators can define grading criteria.");
        }

        DeliverableType deliverableType = parseDeliverableType(request.deliverableType());

        GradingCriteria criteria = new GradingCriteria();
        criteria.setDeliverableType(deliverableType);
        criteria.setName(request.name());
        criteria.setDescription(request.description());
        criteria.setWeight(request.weight());
        criteria.setCreatedBy(creatorId);

        GradingCriteria saved = criteriaRepository.save(criteria);
        return toDto(saved);
    }

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

    private DeliverableType parseDeliverableType(String value) {
        try {
            return DeliverableType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid deliverableType: " + value +
                    ". Must be one of: PROPOSAL, REVISED_PROPOSAL, STATEMENT_OF_WORK");
        }
    }

    private GradingCriteriaDto toDto(GradingCriteria c) {
        return new GradingCriteriaDto(
                c.getId(),
                c.getDeliverableType().name(),
                c.getName(),
                c.getDescription(),
                c.getWeight()
        );
    }
}
