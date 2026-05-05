package com.spms.backend.exception;

import org.springframework.http.HttpStatus;

public class P7ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;
    private final Long existingJobId;

    public P7ApiException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.existingJobId = null;
    }

    public P7ApiException(HttpStatus status, String errorCode, String message, Long existingJobId) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.existingJobId = existingJobId;
    }

    public HttpStatus getStatus() { return status; }
    public String getErrorCode() { return errorCode; }
    public Long getExistingJobId() { return existingJobId; }
}
