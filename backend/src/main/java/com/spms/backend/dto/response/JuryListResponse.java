package com.spms.backend.dto.response;

import java.util.List;

public record JuryListResponse(String status, List<JuryMemberDto> data) {}
