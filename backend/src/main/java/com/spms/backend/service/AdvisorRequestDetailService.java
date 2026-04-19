package com.spms.backend.service;

import com.spms.backend.dto.response.AdvisorRequestDetailDto;

public interface AdvisorRequestDetailService {

    AdvisorRequestDetailDto getDetail(Long requestId, Long callerId, String callerRole);
}
