package com.spms.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public class SystemLogCreateRequestDto {
    @NotBlank(message = "eventType cannot be empty")
    private String eventType;

    @NotBlank(message = "message cannot be empty")
    private String message;

    private String stackTrace;

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getStackTrace() { return stackTrace; }
    public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }
}
