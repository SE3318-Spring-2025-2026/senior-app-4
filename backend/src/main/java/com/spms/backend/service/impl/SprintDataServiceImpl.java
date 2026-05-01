package com.spms.backend.service.impl;

import com.spms.backend.dto.request.BulkUpdateRequestDto;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.model.ValidStudentId;
import com.spms.backend.repository.ValidStudentIdRepository;
import com.spms.backend.service.SprintDataService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SprintDataServiceImpl implements SprintDataService {

    private final ValidStudentIdRepository validStudentIdRepository;

    public SprintDataServiceImpl(ValidStudentIdRepository validStudentIdRepository) {
        this.validStudentIdRepository = validStudentIdRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bulkUpdateRecords(BulkUpdateRequestDto requestDto) {
        List<ValidStudentId> entities = requestDto.records().stream()
                .map(dto -> {
                    if (dto.studentId() == null || dto.studentId().trim().isEmpty()) {
                        throw new BadRequestException("Student ID cannot be null or empty in bulk update");
                    }
                    return new ValidStudentId(dto.studentId());
                })
                .collect(Collectors.toList());

        validStudentIdRepository.saveAll(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ValidStudentId> getAllRecords() {
        return validStudentIdRepository.findAll();
    }
}
