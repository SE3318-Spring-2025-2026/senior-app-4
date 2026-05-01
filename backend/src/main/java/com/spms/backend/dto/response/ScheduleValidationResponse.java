package com.spms.backend.dto.response;

public record ScheduleValidationResponse(boolean valid, String conflictDetails) {
}
