package com.spms.backend.service;

import com.spms.backend.dto.response.AdvisorRequestSummaryDto;
import java.util.List;

public interface AdvisorRequestService {
    List<AdvisorRequestSummaryDto> listAdvisorRequests(Long userId, String role, String status, Long teamId, Long professorId);
}
