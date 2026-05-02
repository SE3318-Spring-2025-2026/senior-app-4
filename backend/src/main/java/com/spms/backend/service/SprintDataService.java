package com.spms.backend.service;

import com.spms.backend.dto.request.BulkUpdateRequestDto;
import com.spms.backend.model.ValidStudentId;
import java.util.List;

public interface SprintDataService {
    void bulkUpdateRecords(BulkUpdateRequestDto requestDto);
    List<ValidStudentId> getAllRecords();
}
