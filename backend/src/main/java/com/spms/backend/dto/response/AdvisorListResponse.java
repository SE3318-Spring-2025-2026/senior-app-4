package com.spms.backend.dto.response;

import java.util.List;

public record AdvisorListResponse(String status, List<AdvisorDto> data) {}
