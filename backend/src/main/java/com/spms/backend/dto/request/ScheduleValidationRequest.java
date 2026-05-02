package com.spms.backend.dto.request;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public record ScheduleValidationRequest(
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm") LocalDateTime examDate,
        Long groupId) {
}